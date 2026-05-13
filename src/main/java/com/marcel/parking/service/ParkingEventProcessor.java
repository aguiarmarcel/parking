package com.marcel.parking.service;

import com.marcel.parking.dto.WebhookEventRequest;
import com.marcel.parking.entity.GarageSector;
import com.marcel.parking.entity.ParkingSpot;
import com.marcel.parking.entity.ParkingTicket;
import com.marcel.parking.enumtype.ParkingSpotStatus;
import com.marcel.parking.enumtype.ParkingTicketStatus;
import com.marcel.parking.exception.BusinessException;
import com.marcel.parking.exception.ResourceNotFoundException;
import com.marcel.parking.exception.SectorFullException;
import com.marcel.parking.repository.GarageSectorRepository;
import com.marcel.parking.repository.ParkingSpotRepository;
import com.marcel.parking.repository.ParkingTicketRepository;
import com.marcel.parking.strategy.DynamicPricingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingEventProcessor {

    private final GarageSectorRepository garageSectorRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingTicketRepository parkingTicketRepository;
    private final DynamicPricingStrategy dynamicPricingStrategy;
    private final PricingService pricingService;

    @Transactional
    @CacheEvict(cacheNames = "revenue", allEntries = true)
    public void process(WebhookEventRequest request) {
        switch (request.eventType()) {
            case ENTRY -> processEntry(request);
            case PARKED -> processParked(request);
            case EXIT -> processExit(request);
        }
    }

    private void processEntry(WebhookEventRequest request) {
        Instant entryTime = request.entryTime() == null ? Instant.now() : request.entryTime();

        ParkingSpot spot = parkingSpotRepository
                .findFirstBySectorAndStatusOrderByIdAsc(resolveEntrySector(), ParkingSpotStatus.AVAILABLE)
                .orElseThrow(() -> new SectorFullException("ALL"));

        GarageSector sector = garageSectorRepository.findWithLockBySector(spot.getSector())
                .orElseThrow(() -> new ResourceNotFoundException("Sector not found: " + spot.getSector()));

        if (sector.isFull()) {
            throw new SectorFullException(sector.getSector());
        }

        BigDecimal priceMultiplier = dynamicPricingStrategy.resolveMultiplier(sector);

        spot.occupy(request.licensePlate());
        sector.incrementOccupancy();

        parkingTicketRepository.save(
                new ParkingTicket(
                        request.licensePlate(),
                        sector.getSector(),
                        spot.getId(),
                        entryTime,
                        priceMultiplier
                )
        );

        log.info("Vehicle entered. licensePlate={}, sector={}, spot={}, multiplier={}",
                request.licensePlate(),
                sector.getSector(),
                spot.getId(),
                priceMultiplier
        );
    }

    private void processParked(WebhookEventRequest request) {
        ParkingTicket ticket = parkingTicketRepository
                .findFirstByLicensePlateAndStatusInOrderByEntryTimeDesc(
                        request.licensePlate(),
                        List.of(ParkingTicketStatus.OPEN, ParkingTicketStatus.PARKED)
                )
                .orElseThrow(() -> new ResourceNotFoundException("Open ticket not found for " + request.licensePlate()));

        if (request.lat() != null && request.lng() != null) {
            ParkingSpot spot = parkingSpotRepository.findByLatitudeAndLongitude(request.lat(), request.lng())
                    .orElseThrow(() -> new ResourceNotFoundException("Spot not found by coordinates"));

            ticket.markAsParked(spot.getId());
        } else {
            ticket.setStatus(ParkingTicketStatus.PARKED);
            ticket.setParkedTime(Instant.now());
        }

        log.info("Vehicle parked. licensePlate={}, ticket={}", request.licensePlate(), ticket.getId());
    }

    private void processExit(WebhookEventRequest request) {
        Instant exitTime = request.exitTime() == null ? Instant.now() : request.exitTime();

        ParkingTicket ticket = parkingTicketRepository
                .findFirstByLicensePlateAndStatusInOrderByEntryTimeDesc(
                        request.licensePlate(),
                        List.of(ParkingTicketStatus.OPEN, ParkingTicketStatus.PARKED)
                )
                .orElseThrow(() -> new ResourceNotFoundException("Open ticket not found for " + request.licensePlate()));

        GarageSector sector = garageSectorRepository.findWithLockBySector(ticket.getSector())
                .orElseThrow(() -> new ResourceNotFoundException("Sector not found: " + ticket.getSector()));

        BigDecimal amount = pricingService.calculate(
                sector,
                ticket.getEntryTime(),
                exitTime,
                ticket.getPriceMultiplier()
        );

        ticket.close(exitTime, amount);

        parkingSpotRepository.findById(ticket.getSpotId())
                .ifPresent(ParkingSpot::release);

        sector.decrementOccupancy();

        log.info("Vehicle exited. licensePlate={}, sector={}, amount={}",
                request.licensePlate(),
                sector.getSector(),
                amount
        );
    }

    private String resolveEntrySector() {
        return garageSectorRepository.findAll()
                .stream()
                .filter(sector -> !sector.isFull())
                .map(GarageSector::getSector)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Parking is full"));
    }
}
