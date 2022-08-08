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

    private void getBidDistribution(int expectedSize, int maxSize, double scalingFactor,
                                    double quoteRange, double inventoryRange) {
        double bidPoolSize = 50000d;
        List<DistributionStep> distribution = pricingUtils.getBidDistribution(
                scalingFactor, bidPoolSize, 2.5d, quoteRange, inventoryRange, maxSize);
        Assertions.assertEquals(distribution.size(), expectedSize);
        double volume = distribution.stream().map(d -> d.getPrice() * d.getSize()).mapToDouble(d -> d).sum();
        if(quoteRange < inventoryRange) {
            Assertions.assertTrue(volume < bidPoolSize * scalingFactor);
        } else {
            Assertions.assertEquals(volume, bidPoolSize * scalingFactor, bidPoolSize / 20d);
        }
    }

    private void getAskDistribution(int expectedSize, int maxSize, double scalingFactor,
                                    double quoteRange, double inventoryRange) {
        double askPoolSize = 2.5d;
        List<DistributionStep> distribution = pricingUtils.getAskDistribution(
                scalingFactor, 50000d, askPoolSize, quoteRange, inventoryRange, maxSize);
        Assertions.assertEquals(distribution.size(), expectedSize);
        double volume = distribution.stream().map(DistributionStep::getSize).mapToDouble(d -> d).sum();
        if(quoteRange < inventoryRange) {
            Assertions.assertTrue(volume < askPoolSize * scalingFactor);
        } else {
            Assertions.assertEquals(volume, askPoolSize * scalingFactor, askPoolSize / 20d);
        }
    }

    @Test
    public void testGetBidDistribution() {
        getBidDistribution(10, 10, 1.0, 0.05, 0.999);
    }

    @Test
    public void testGetAskDistribution() {
        getAskDistribution(10, 10, 1.0, 0.05, 2.0);
    }

    @Test
    public void testGetBidDistributionMissingConfig() {
        Mockito.when(appConfigStore.get()).thenReturn(Optional.empty());
        try {
            getBidDistribution(10, 10, 1.0, 0.999, 0.999);
            Assertions.fail();
        } catch(TradingException e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.APP_CONFIG_NOT_FOUND);
        }
    }

    @Test
    public void testGetAskDistributionMissingConfig() {
        Mockito.when(appConfigStore.get()).thenReturn(Optional.empty());
        try {
            getAskDistribution(10, 10, 1.0, 2.0, 2.0);
            Assertions.fail();
        } catch(TradingException e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.APP_CONFIG_NOT_FOUND);
        }
    }

    @Test
    public void testGetBidDistributionWithScalingApplied() {
        getBidDistribution(10, 10, 0.5, 0.999, 0.999);
    }

    @Test
    public void testGetAskDistributionWithScalingApplied() {
        getAskDistribution(10, 10, 0.5, 2.0, 2.0);
    }

    @Test
    public void testGetBidDistributionOutOfRange() {
        getBidDistribution(10, 10, 1.0, 0.999999, 0.999);
    }

    @Test
    public void testGetAskDistributionOutOfRange() {
        getAskDistribution(10, 10, 1.0, 100.0, 2.0);
    }

    @Test
    public void testGetBidDistributionPartialRange() {
        getBidDistribution(10, 10, 1.0, 0.25, 0.5);
    }

    @Test
    public void testGetBidDistributionCustomRange() {
        getBidDistribution(10, 10, 1.0, 0.5, 0.5);
    }

    @Test
    public void testGetAskDistributionPartialRange() {
        getAskDistribution(10, 10, 1.0, 0.5, 1.0);
    }

    @Test
    public void testGetBidDistributionWithoutAggregation() {
        getBidDistribution(5521, 10000, 1.0, 0.999, 0.999);
    }

    @Test
    public void testGetAskDistributionWithoutAggregation() {
        getAskDistribution(846, 10000, 1.0, 2.0, 2.0);
    }
}