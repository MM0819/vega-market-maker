package com.vega.protocol.api;

import com.vega.protocol.constant.MarketSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class BinanceApiClientTest {

    private BinanceApiClient binanceApiClient;

    @BeforeEach
    public void setup() {
        binanceApiClient = new BinanceApiClient();
    }

    @Test
    public void testSubmitMarketOrder() {
        binanceApiClient.submitMarketOrder("BTCUSDT", 1.0, MarketSide.BUY);
    }

    @Test
    public void testGetPosition() {
        binanceApiClient.getPosition("BTCUSDT");
    }
}