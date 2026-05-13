package com.marcel.parking.service;

import com.marcel.parking.entity.GarageSector;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Service
public class PricingService {

    private static final long FREE_MINUTES = 30;

    public BigDecimal calculate(
            GarageSector sector,
            Instant entryTime,
            Instant exitTime,
            BigDecimal priceMultiplier
    ) {
        long parkedMinutes = Duration.between(entryTime, exitTime).toMinutes();

        if (parkedMinutes <= FREE_MINUTES) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long hours = (long) Math.ceil(parkedMinutes / 60.0);

        return sector.getBasePrice()
                .multiply(BigDecimal.valueOf(hours))
                .multiply(priceMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
