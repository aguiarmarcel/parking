package com.marcel.parking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record GarageConfigurationResponse(
        List<GarageSectorItem> garage,
        List<GarageSpotItem> spots
) {

    public record GarageSectorItem(
            String sector,
            BigDecimal basePrice,
            @JsonProperty("max_capacity")
            Integer maxCapacity
    ) {
    }

    public record GarageSpotItem(
            Long id,
            String sector,
            BigDecimal lat,
            BigDecimal lng
    ) {
    }
}
