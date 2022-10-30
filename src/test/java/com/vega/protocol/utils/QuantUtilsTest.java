package com.vega.protocol.utils;

import com.vega.protocol.constant.MarketSide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QuantUtilsTest {

    private QuantUtils quantUtils;

    @BeforeEach
    public void setup() {
        quantUtils = new QuantUtils();
    }

    @Test
    public void testGetProbabilityOfTrading() {
        double mu = 0;
        double tau = 1.0 / 365.25 / 24.0;
        double s = 100;
        double sigma = 1.2;
        double lowerBound = 95.0;
        double upperBound = 100.0;
        MarketSide side = MarketSide.BUY;
        double price = 99.999;
        double result = quantUtils.getProbabilityOfTrading(mu, sigma, s, tau, lowerBound, upperBound, price, side);
        Assertions.assertEquals(result, 0.0);
    }
}