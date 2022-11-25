package com.vega.protocol.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vega.Vega;

@Slf4j
public class QuantUtilsTest {

    private QuantUtils quantUtils;

    @BeforeEach
    public void setup() {
        quantUtils = new QuantUtils();
    }

    @Test
    public void testGetProbabilityOfTradingForBuyOrder() {
        double mu = 0;
        double tau = 1.0 / 365.25;
        double bestBid = 100;
        double sigma = 1.2;
        double lowerBound = 95.0;
        double upperBound = 100.0;
        Vega.Side side = Vega.Side.SIDE_BUY;
        double price = 99.999;
        double result = quantUtils.getProbabilityOfTrading(mu, sigma, bestBid, tau, lowerBound, upperBound, price, side);
        Assertions.assertEquals(result, 0.9997857769969329);
    }

    @Test
    public void testGetProbabilityOfTradingForSellOrder() {
        double mu = 0;
        double tau = 1.0 / 365.25;
        double bestAsk = 95;
        double sigma = 1.2;
        double lowerBound = 95.0;
        double upperBound = 100.0;
        Vega.Side side = Vega.Side.SIDE_SELL;
        double price = 95.001;
        double result = quantUtils.getProbabilityOfTrading(mu, sigma, bestAsk, tau, lowerBound, upperBound, price, side);
        Assertions.assertEquals(result, 0.9997689694868312);
    }

    @Test
    public void testGetProbabilityOfTradingAboveUpperBound() {
        double mu = 0;
        double tau = 1.0 / 365.25;
        double bestAsk = 95;
        double sigma = 1.2;
        double lowerBound = 95.0;
        double upperBound = 100.0;
        Vega.Side side = Vega.Side.SIDE_SELL;
        double price = 101;
        double result = quantUtils.getProbabilityOfTrading(mu, sigma, bestAsk, tau, lowerBound, upperBound, price, side);
        Assertions.assertEquals(result, 0);
    }

    @Test
    public void testGetProbabilityOfTradingBelowLowerBound() {
        double mu = 0;
        double tau = 1.0 / 365.25;
        double bestAsk = 95;
        double sigma = 1.2;
        double lowerBound = 95.0;
        double upperBound = 100.0;
        Vega.Side side = Vega.Side.SIDE_SELL;
        double price = 94;
        double result = quantUtils.getProbabilityOfTrading(mu, sigma, bestAsk, tau, lowerBound, upperBound, price, side);
        Assertions.assertEquals(result, 0);
    }
}