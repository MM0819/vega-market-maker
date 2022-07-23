package com.vega.protocol.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PricingUtilsTest {

    private final PricingUtils pricingUtils = new PricingUtils();

    @Test
    public void testGetBidScalingFactor() {
        double scalingFactor = pricingUtils.getBidScalingFactor(0L, 0L);
        Assertions.assertEquals(scalingFactor, 1.0);
    }

    @Test
    public void testGetAskScalingFactor() {
        double scalingFactor = pricingUtils.getAskScalingFactor(0L, 0L);
        Assertions.assertEquals(scalingFactor, 1.0);
    }
}