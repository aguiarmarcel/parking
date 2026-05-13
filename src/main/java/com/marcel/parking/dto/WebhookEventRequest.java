package com.marcel.parking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marcel.parking.enumtype.WebhookEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record WebhookEventRequest(
        @NotBlank
        @JsonProperty("license_plate")
        String licensePlate,

        @JsonProperty("entry_time")
        Instant entryTime,

        @JsonProperty("exit_time")
        Instant exitTime,

        BigDecimal lat,

        BigDecimal lng,

        @NotNull
        @JsonProperty("event_type")
        WebhookEventType eventType
) {
}
