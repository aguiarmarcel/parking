package com.marcel.parking.strategy;

import com.marcel.parking.entity.GarageSector;
import org.hibernate.annotations.Comment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OccupancyBasedPricingStrategy implements DynamicPricingStrategy{

    private static final BigDecimal TWENTY_FIVE_PERCENT = new BigDecimal("0.25");
    private static final BigDecimal FIFTY_PERCENT = new BigDecimal("0.50");
    private static final BigDecimal SEVENTY_FIVE_PERCENT = new BigDecimal("0.75");

    private static final BigDecimal TEN_PERCENT_DISCOUNT = new BigDecimal("0.90");
    private static final BigDecimal NORMAL_PRICE = new BigDecimal("1.00");
    private static final BigDecimal TEN_PERCENT_INCREASE = new BigDecimal("1.10");
    private static final BigDecimal TWENTY_FIVE_PERCENT_INCREASE = new BigDecimal("1.25");

    @Override
    public BigDecimal resolveMultiplier(GarageSector sector) {
        BigDecimal occupancyRate = sector.occupancyRate();

        if (occupancyRate.compareTo(TWENTY_FIVE_PERCENT) < 0) {
            return TEN_PERCENT_DISCOUNT;
        }

        if (occupancyRate.compareTo(FIFTY_PERCENT) <= 0) {
            return NORMAL_PRICE;
        }

        if (occupancyRate.compareTo(SEVENTY_FIVE_PERCENT) <= 0) {
            return TEN_PERCENT_INCREASE;
        }

        return TWENTY_FIVE_PERCENT_INCREASE;
    }
}
