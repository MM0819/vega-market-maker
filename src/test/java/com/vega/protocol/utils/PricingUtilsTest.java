package com.vega.protocol.utils;

import com.vega.protocol.model.DistributionStep;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PricingUtilsTest {

    private final PricingUtils pricingUtils = new PricingUtils();

    @Test
    public void testGetScalingFactor() {
        double scalingFactor = pricingUtils.getScalingFactor(0L, 0);
        Assertions.assertEquals(scalingFactor, 1.0);
    }

    @Test
    public void testGetScalingFactorWithOpenVolume() {
        double scalingFactor = pricingUtils.getScalingFactor(10000L, 0.5);
        Assertions.assertEquals(scalingFactor, 0.5);
    }

    private void getBidDistribution(int expectedSize, int maxSize, double expectedVolume, double scalingFactor) {
        List<DistributionStep> distribution = pricingUtils.getBidDistribution(
                scalingFactor, 50000d, 2.5d, 0.05d, maxSize);
        Assertions.assertEquals(distribution.size(), expectedSize);
        double volume = distribution.stream().mapToDouble(DistributionStep::getSize).sum();
        Assertions.assertEquals(volume, expectedVolume, 0.01);
    }

    private void getAskDistribution(int expectedSize, int maxSize, double expectedVolume, double scalingFactor) {
        List<DistributionStep> distribution = pricingUtils.getAskDistribution(
                scalingFactor, 50000d, 2.5d, 0.05d, maxSize);
        Assertions.assertEquals(distribution.size(), expectedSize);
        double volume = distribution.stream().mapToDouble(DistributionStep::getSize).sum();
        Assertions.assertEquals(volume, expectedVolume, 0.01);
    }

    @Test
    public void testGetBidDistribution() {
        getBidDistribution(10, 10, 0.05, 1.0);
    }

    @Test
    public void testGetAskDistribution() {
        getAskDistribution(10, 10, 0.05, 1.0);
    }

    @Test
    public void testGetBidDistributionWithScalingApplied() {
        getBidDistribution(10, 10, 0.02, 0.5);
    }

    @Test
    public void testGetAskDistributionWithScalingApplied() {
        getAskDistribution(10, 10, 0.02, 0.5);
    }

    @Test
    public void testGetBidDistributionWithoutAggregation() {
        getBidDistribution(52, 100, 0.07, 1.0);
    }

    @Test
    public void testGetAskDistributionWithoutAggregation() {
        getAskDistribution(51, 100, 0.07, 1.0);
    }
}