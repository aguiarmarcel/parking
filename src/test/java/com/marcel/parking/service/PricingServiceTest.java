package com.marcel.parking.service;

import com.marcel.parking.entity.GarageSector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    @Test
    @DisplayName("Deve retornar zero quando permanência for menor ou igual a 30 minutos")
    void shouldReturnZeroWhenParkingTimeIsLessThanOrEqualToThirtyMinutes() {
        GarageSector sector = new GarageSector("A", new BigDecimal("10.00"), 10);

        BigDecimal amount = pricingService.calculate(
                sector,
                Instant.parse("2026-05-13T10:00:00Z"),
                Instant.parse("2026-05-13T10:30:00Z"),
                new BigDecimal("1.00")
        );

        assertThat(amount).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("Deve cobrar uma hora quando permanência passar de 30 minutos")
    void shouldChargeOneHourWhenParkingTimeIsGreaterThanThirtyMinutes() {
        GarageSector sector = new GarageSector("A", new BigDecimal("10.00"), 10);

        BigDecimal amount = pricingService.calculate(
                sector,
                Instant.parse("2026-05-13T10:00:00Z"),
                Instant.parse("2026-05-13T10:31:00Z"),
                new BigDecimal("1.00")
        );

        assertThat(amount).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Deve arredondar hora parcial para cima")
    void shouldRoundPartialHourUp() {
        GarageSector sector = new GarageSector("A", new BigDecimal("10.00"), 10);

        BigDecimal amount = pricingService.calculate(
                sector,
                Instant.parse("2026-05-13T10:00:00Z"),
                Instant.parse("2026-05-13T11:01:00Z"),
                new BigDecimal("1.00")
        );

        assertThat(amount).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    @DisplayName("Deve aplicar multiplicador de preço dinâmico")
    void shouldApplyDynamicPriceMultiplier() {
        GarageSector sector = new GarageSector("A", new BigDecimal("10.00"), 10);

        BigDecimal amount = pricingService.calculate(
                sector,
                Instant.parse("2026-05-13T10:00:00Z"),
                Instant.parse("2026-05-13T12:00:00Z"),
                new BigDecimal("1.25")
        );

        assertThat(amount).isEqualByComparingTo(new BigDecimal("25.00"));
    }
}
