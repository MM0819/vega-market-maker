package com.vega.protocol.utils;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.model.DistributionStep;
import com.vega.protocol.store.AppConfigStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Comparator;
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
    public void testGetBidDistributionV2() {
        double count = 60.0;
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig().setOrderCount((int) count)));
        double range = 0.02;
        List<DistributionStep> distribution = pricingUtils.getDistribution(
                100.0, 2000.0, range, MarketSide.BUY);
        double totalVolume = distribution.stream().mapToDouble(DistributionStep::getSize).sum();
        double bestBid = distribution.stream().max(Comparator.comparing(DistributionStep::getPrice))
                .orElse(new DistributionStep().setPrice(0.0)).getPrice();
        double worstBid = distribution.stream().min(Comparator.comparing(DistributionStep::getPrice))
                .orElse(new DistributionStep().setPrice(0.0)).getPrice();
        Assertions.assertEquals(Math.round(totalVolume), 2000.0);
        Assertions.assertEquals(bestBid, Math.round((100.0 - ((range * 100.0) / count)) * 10000.0) / 10000.0);
        Assertions.assertEquals(worstBid, 100.0 - (range * 100.0));
    }

    @Test
    public void testGetAskDistributionV2() {
        double count = 25.0;
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig().setOrderCount((int) count)));
        double range = 0.02;
        List<DistributionStep> distribution = pricingUtils.getDistribution(
                100.0, 2000.0, range, MarketSide.SELL);
        double totalVolume = distribution.stream().mapToDouble(DistributionStep::getSize).sum();
        double bestAsk = distribution.stream().min(Comparator.comparing(DistributionStep::getPrice))
                .orElse(new DistributionStep().setPrice(0.0)).getPrice();
        double worstAsk = distribution.stream().max(Comparator.comparing(DistributionStep::getPrice))
                .orElse(new DistributionStep().setPrice(0.0)).getPrice();
        Assertions.assertEquals(Math.round(totalVolume), 2000.0);
        Assertions.assertEquals(bestAsk, Math.round((100.0 + ((range * 100.0) / count)) * 10000.0) / 10000.0);
        Assertions.assertEquals(worstAsk, 100.0 + (range * 100.0));
    }

    @Test
    public void testGetDistributionWithMissingConfig() {
        Mockito.when(appConfigStore.get()).thenReturn(Optional.empty());
        try {
            pricingUtils.getDistribution(100.0, 2000.0, 0.02, MarketSide.SELL);
            Assertions.fail();
        } catch (TradingException e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.APP_CONFIG_NOT_FOUND);
        }
    }
}