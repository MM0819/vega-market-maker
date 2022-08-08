package com.vega.protocol.utils;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.model.DistributionStep;
import com.vega.protocol.store.AppConfigStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

public class PricingUtilsTest {

    private AppConfigStore appConfigStore;
    private PricingUtils pricingUtils;

    @BeforeEach
    public void setup() {
        appConfigStore = Mockito.mock(AppConfigStore.class);
        pricingUtils = new PricingUtils(appConfigStore);
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig().setPricingStepSize(0.1)));
    }

    @Test
    public void testGetScalingFactor() {
        double scalingFactor = pricingUtils.getScalingFactor(0);
        Assertions.assertEquals(scalingFactor, 1.0);
    }

    @Test
    public void testGetScalingFactorWithOpenVolume() {
        double scalingFactor = pricingUtils.getScalingFactor(0.5);
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
    public void testGetBidDistributionMissingConfig() {
        Mockito.when(appConfigStore.get()).thenReturn(Optional.empty());
        try {
            getBidDistribution(10, 10, 0.05, 1.0);
            Assertions.fail();
        } catch(TradingException e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.APP_CONFIG_NOT_FOUND);
        }
    }

    @Test
    public void testGetAskDistributionMissingConfig() {
        Mockito.when(appConfigStore.get()).thenReturn(Optional.empty());
        try {
            getAskDistribution(10, 10, 0.05, 1.0);
            Assertions.fail();
        } catch(TradingException e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.APP_CONFIG_NOT_FOUND);
        }
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

    // TODO - add tests to ensure that inventory is correctly distributed between zero and infinity
}