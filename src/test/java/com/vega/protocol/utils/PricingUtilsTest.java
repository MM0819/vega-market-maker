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

    private void getBidDistribution(int expectedSize, double expectedVolume,
                                    int maxSize, double scalingFactor, double quoteRange) {
        double bidPoolSize = 50000d;
        List<DistributionStep> distribution = pricingUtils.getBidDistribution(
                scalingFactor, bidPoolSize, 2.5d, quoteRange, maxSize);
        Assertions.assertEquals(distribution.size(), expectedSize);
        double volume = distribution.stream().map(d -> d.getPrice() * d.getSize()).mapToDouble(d -> d).sum();
        Assertions.assertEquals(volume, expectedVolume, 10d);
    }

    private void getAskDistribution(int expectedSize, double expectedVolume,
                                    int maxSize, double scalingFactor, double quoteRange) {
        double askPoolSize = 2.5d;
        List<DistributionStep> distribution = pricingUtils.getAskDistribution(
                scalingFactor, 50000d, askPoolSize, quoteRange, maxSize);
        Assertions.assertEquals(distribution.size(), expectedSize);
        double volume = distribution.stream().map(DistributionStep::getSize).mapToDouble(d -> d).sum();
        Assertions.assertEquals(volume, expectedVolume, 0.01d);
    }

    @Test
    public void testGetBidDistribution() {
        getBidDistribution(10, 1250, 10, 1.0, 0.05);
    }

    @Test
    public void testGetAskDistribution() {
        getAskDistribution(10, 0.059, 10, 1.0, 0.05);
    }

    @Test
    public void testGetBidDistributionMissingConfig() {
        Mockito.when(appConfigStore.get()).thenReturn(Optional.empty());
        try {
            getBidDistribution(10, 1.0, 10, 1.0, 0.999);
            Assertions.fail();
        } catch(TradingException e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.APP_CONFIG_NOT_FOUND);
        }
    }

    @Test
    public void testGetAskDistributionMissingConfig() {
        Mockito.when(appConfigStore.get()).thenReturn(Optional.empty());
        try {
            getAskDistribution(10, 1.0, 10, 1.0, 2.0);
            Assertions.fail();
        } catch(TradingException e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.APP_CONFIG_NOT_FOUND);
        }
    }

    @Test
    public void testGetBidDistributionWithScalingApplied() {
        getBidDistribution(10, 24740, 10, 0.5, 0.999);
    }

    @Test
    public void testGetAskDistributionWithScalingApplied() {
        getAskDistribution(10, 0.76, 10, 0.5, 2.0);
    }

    @Test
    public void testGetBidDistributionWithoutAggregation() {
        getBidDistribution(5521, 48420, 10000, 1.0, 0.999);
    }

    @Test
    public void testGetAskDistributionWithoutAggregation() {
        getAskDistribution(846, 1.05, 10000, 1.0, 2.0);
    }
}