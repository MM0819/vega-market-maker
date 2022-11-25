package com.vega.protocol.utils;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.model.trading.DistributionStep;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Comparator;
import java.util.List;

public class PricingUtilsTest {

    private PricingUtils pricingUtils;
    private DecimalUtils decimalUtils;

    @BeforeEach
    public void setup() {
        decimalUtils = Mockito.mock(DecimalUtils.class);
        pricingUtils = new PricingUtils(decimalUtils);
    }


    @Test
    public void testGetBidDistributionV2() {
        int count = 60;
        double range = 0.02;
        List<DistributionStep> distribution = pricingUtils.getDistribution(
                100.0, 2000.0, range, MarketSide.BUY, count);
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
        int count = 25;
        double range = 0.02;
        List<DistributionStep> distribution = pricingUtils.getDistribution(
                100.0, 2000.0, range, MarketSide.SELL, count);
        double totalVolume = distribution.stream().mapToDouble(DistributionStep::getSize).sum();
        double bestAsk = distribution.stream().min(Comparator.comparing(DistributionStep::getPrice))
                .orElse(new DistributionStep().setPrice(0.0)).getPrice();
        double worstAsk = distribution.stream().max(Comparator.comparing(DistributionStep::getPrice))
                .orElse(new DistributionStep().setPrice(0.0)).getPrice();
        Assertions.assertEquals(Math.round(totalVolume), 2000.0);
        Assertions.assertEquals(bestAsk, Math.round((100.0 + ((range * 100.0) / count)) * 10000.0) / 10000.0);
        Assertions.assertEquals(worstAsk, 100.0 + (range * 100.0));
    }
}