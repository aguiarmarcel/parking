package com.marcel.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RevenueRequest(
        @NotNull
        LocalDate date,

        @NotBlank
        String sector
) {
}
