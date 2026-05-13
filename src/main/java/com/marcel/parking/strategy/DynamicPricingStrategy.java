package com.marcel.parking.strategy;

import com.marcel.parking.entity.GarageSector;

import java.math.BigDecimal;

public interface DynamicPricingStrategy {

    BigDecimal resolveMultiplier(GarageSector sector);
}