package com.marcel.parking.service;

import com.marcel.parking.dto.RevenueRequest;
import com.marcel.parking.dto.RevenueResponse;
import com.marcel.parking.repository.ParkingTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final ParkingTicketRepository parkingTicketRepository;

    @Cacheable(cacheNames = "revenue", key = "#request.sector() + ':' + #request.date()")
    public RevenueResponse getRevenue(RevenueRequest request) {
        Instant start = request.date().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = request.date().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        BigDecimal amount = parkingTicketRepository.sumRevenueBySectorAndExitTimeBetween(
                request.sector(),
                start,
                end
        );

        return new RevenueResponse(amount, "BRL", Instant.now());
    }
}
