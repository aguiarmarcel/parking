package com.marcel.parking.service;

import com.marcel.parking.dto.WebhookEventRequest;
import com.marcel.parking.entity.GarageSector;
import com.marcel.parking.entity.ParkingSpot;
import com.marcel.parking.entity.ParkingTicket;
import com.marcel.parking.enumtype.ParkingSpotStatus;
import com.marcel.parking.enumtype.ParkingTicketStatus;
import com.marcel.parking.enumtype.WebhookEventType;
import com.marcel.parking.exception.BusinessException;
import com.marcel.parking.exception.ResourceNotFoundException;
import com.marcel.parking.repository.GarageSectorRepository;
import com.marcel.parking.repository.ParkingSpotRepository;
import com.marcel.parking.repository.ParkingTicketRepository;
import com.marcel.parking.strategy.DynamicPricingStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingEventProcessorTest {

    @Mock
    private GarageSectorRepository garageSectorRepository;

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @Mock
    private ParkingTicketRepository parkingTicketRepository;

    @Mock
    private DynamicPricingStrategy dynamicPricingStrategy;

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private ParkingEventProcessor parkingEventProcessor;

    @Test
    @DisplayName("Deve processar ENTRY ocupando vaga, incrementando setor e criando ticket")
    void shouldProcessEntry() {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                Instant.parse("2026-05-13T10:00:00Z"),
                null,
                null,
                null,
                WebhookEventType.ENTRY
        );

        GarageSector sector = new GarageSector("A", new BigDecimal("10.00"), 10);
        ParkingSpot spot = new ParkingSpot(1L, "A", BigDecimal.ONE, BigDecimal.TEN);

        when(garageSectorRepository.findAll()).thenReturn(List.of(sector));
        when(parkingSpotRepository.findFirstBySectorAndStatusOrderByIdAsc("A", ParkingSpotStatus.AVAILABLE))
                .thenReturn(Optional.of(spot));
        when(garageSectorRepository.findWithLockBySector("A"))
                .thenReturn(Optional.of(sector));
        when(dynamicPricingStrategy.resolveMultiplier(sector))
                .thenReturn(new BigDecimal("1.00"));

        parkingEventProcessor.process(request);

        ArgumentCaptor<ParkingTicket> ticketCaptor = ArgumentCaptor.forClass(ParkingTicket.class);
        verify(parkingTicketRepository).save(ticketCaptor.capture());

        ParkingTicket ticket = ticketCaptor.getValue();

        assertThat(spot.getStatus()).isEqualTo(ParkingSpotStatus.OCCUPIED);
        assertThat(spot.getOccupiedByLicensePlate()).isEqualTo("ABC1D23");

        assertThat(sector.getOccupiedCount()).isEqualTo(1);

        assertThat(ticket.getLicensePlate()).isEqualTo("ABC1D23");
        assertThat(ticket.getSector()).isEqualTo("A");
        assertThat(ticket.getSpotId()).isEqualTo(1L);
        assertThat(ticket.getEntryTime()).isEqualTo(Instant.parse("2026-05-13T10:00:00Z"));
        assertThat(ticket.getPriceMultiplier()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(ticket.getStatus()).isEqualTo(ParkingTicketStatus.OPEN);
    }

    @Test
    @DisplayName("Deve lançar exceção quando estacionamento estiver cheio")
    void shouldThrowExceptionWhenParkingIsFull() {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                Instant.parse("2026-05-13T10:00:00Z"),
                null,
                null,
                null,
                WebhookEventType.ENTRY
        );

        GarageSector fullSector = new GarageSector("A", new BigDecimal("10.00"), 1);
        fullSector.setOccupiedCount(1);

        when(garageSectorRepository.findAll()).thenReturn(List.of(fullSector));

        assertThatThrownBy(() -> parkingEventProcessor.process(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Parking is full");

        verify(parkingTicketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve processar PARKED sem coordenadas")
    void shouldProcessParkedWithoutCoordinates() {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                null,
                null,
                null,
                null,
                WebhookEventType.PARKED
        );

        ParkingTicket ticket = new ParkingTicket(
                "ABC1D23",
                "A",
                1L,
                Instant.parse("2026-05-13T10:00:00Z"),
                new BigDecimal("1.00")
        );

        when(parkingTicketRepository.findFirstByLicensePlateAndStatusInOrderByEntryTimeDesc(
                eq("ABC1D23"),
                any()
        )).thenReturn(Optional.of(ticket));

        parkingEventProcessor.process(request);

        assertThat(ticket.getStatus()).isEqualTo(ParkingTicketStatus.PARKED);
        assertThat(ticket.getParkedTime()).isNotNull();
    }

    @Test
    @DisplayName("Deve processar PARKED com coordenadas, vinculando vaga pelo lat/lng")
    void shouldProcessParkedWithCoordinates() {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                null,
                null,
                new BigDecimal("-23.0000000"),
                new BigDecimal("-46.0000000"),
                WebhookEventType.PARKED
        );

        ParkingTicket ticket = new ParkingTicket(
                "ABC1D23",
                "A",
                1L,
                Instant.parse("2026-05-13T10:00:00Z"),
                new BigDecimal("1.00")
        );

        ParkingSpot spot = new ParkingSpot(
                99L,
                "A",
                new BigDecimal("-23.0000000"),
                new BigDecimal("-46.0000000")
        );

        when(parkingTicketRepository.findFirstByLicensePlateAndStatusInOrderByEntryTimeDesc(
                eq("ABC1D23"),
                any()
        )).thenReturn(Optional.of(ticket));

        when(parkingSpotRepository.findByLatitudeAndLongitude(
                new BigDecimal("-23.0000000"),
                new BigDecimal("-46.0000000")
        )).thenReturn(Optional.of(spot));

        parkingEventProcessor.process(request);

        assertThat(ticket.getStatus()).isEqualTo(ParkingTicketStatus.PARKED);
        assertThat(ticket.getSpotId()).isEqualTo(99L);
        assertThat(ticket.getParkedTime()).isNotNull();
    }

    @Test
    @DisplayName("Deve lançar exceção ao processar PARKED sem ticket aberto")
    void shouldThrowExceptionWhenParkedTicketDoesNotExist() {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                null,
                null,
                null,
                null,
                WebhookEventType.PARKED
        );

        when(parkingTicketRepository.findFirstByLicensePlateAndStatusInOrderByEntryTimeDesc(
                eq("ABC1D23"),
                any()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parkingEventProcessor.process(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Open ticket not found");
    }

    @Test
    @DisplayName("Deve processar EXIT fechando ticket, calculando valor, liberando vaga e decrementando setor")
    void shouldProcessExit() {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                null,
                Instant.parse("2026-05-13T12:00:00Z"),
                null,
                null,
                WebhookEventType.EXIT
        );

        ParkingTicket ticket = new ParkingTicket(
                "ABC1D23",
                "A",
                1L,
                Instant.parse("2026-05-13T10:00:00Z"),
                new BigDecimal("1.00")
        );

        GarageSector sector = new GarageSector("A", new BigDecimal("10.00"), 10);
        sector.setOccupiedCount(1);

        ParkingSpot spot = new ParkingSpot(1L, "A", BigDecimal.ONE, BigDecimal.TEN);
        spot.occupy("ABC1D23");

        when(parkingTicketRepository.findFirstByLicensePlateAndStatusInOrderByEntryTimeDesc(
                eq("ABC1D23"),
                any()
        )).thenReturn(Optional.of(ticket));

        when(garageSectorRepository.findWithLockBySector("A"))
                .thenReturn(Optional.of(sector));

        when(pricingService.calculate(
                eq(sector),
                eq(Instant.parse("2026-05-13T10:00:00Z")),
                eq(Instant.parse("2026-05-13T12:00:00Z")),
                eq(new BigDecimal("1.00"))
        )).thenReturn(new BigDecimal("20.00"));

        when(parkingSpotRepository.findById(1L))
                .thenReturn(Optional.of(spot));

        parkingEventProcessor.process(request);

        assertThat(ticket.getStatus()).isEqualTo(ParkingTicketStatus.CLOSED);
        assertThat(ticket.getExitTime()).isEqualTo(Instant.parse("2026-05-13T12:00:00Z"));
        assertThat(ticket.getAmount()).isEqualByComparingTo(new BigDecimal("20.00"));

        assertThat(spot.getStatus()).isEqualTo(ParkingSpotStatus.AVAILABLE);
        assertThat(spot.getOccupiedByLicensePlate()).isNull();

        assertThat(sector.getOccupiedCount()).isZero();
    }

    @Test
    @DisplayName("Deve lançar exceção ao processar EXIT sem ticket aberto")
    void shouldThrowExceptionWhenExitTicketDoesNotExist() {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                null,
                Instant.parse("2026-05-13T12:00:00Z"),
                null,
                null,
                WebhookEventType.EXIT
        );

        when(parkingTicketRepository.findFirstByLicensePlateAndStatusInOrderByEntryTimeDesc(
                eq("ABC1D23"),
                any()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parkingEventProcessor.process(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Open ticket not found");
    }
}
