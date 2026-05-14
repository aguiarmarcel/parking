package com.marcel.parking.service;

import com.marcel.parking.dto.RevenueRequest;
import com.marcel.parking.dto.RevenueResponse;
import com.marcel.parking.repository.ParkingTicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RevenueServiceTest {

    @Mock
    private ParkingTicketRepository parkingTicketRepository;

    @InjectMocks
    private RevenueService revenueService;

    @Test
    @DisplayName("Deve consultar faturamento do setor considerando início e fim do dia em UTC")
    void shouldGetRevenueUsingStartAndEndOfDayInUtc() {
        RevenueRequest request = new RevenueRequest(
                LocalDate.of(2026, 5, 13),
                "A"
        );

        ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> endCaptor = ArgumentCaptor.forClass(Instant.class);

        when(parkingTicketRepository.sumRevenueBySectorAndExitTimeBetween(
                eq("A"),
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(new BigDecimal("75.50"));

        RevenueResponse response = revenueService.getRevenue(request);

        verify(parkingTicketRepository).sumRevenueBySectorAndExitTimeBetween(
                eq("A"),
                startCaptor.capture(),
                endCaptor.capture()
        );

        assertThat(startCaptor.getValue()).isEqualTo(Instant.parse("2026-05-13T00:00:00Z"));
        assertThat(endCaptor.getValue()).isEqualTo(Instant.parse("2026-05-14T00:00:00Z"));

        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("75.50"));
        assertThat(response.currency()).isEqualTo("BRL");
        assertThat(response.timestamp()).isNotNull();
    }
}
