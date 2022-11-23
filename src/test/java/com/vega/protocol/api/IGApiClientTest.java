package com.vega.protocol.api;

import com.vega.protocol.constant.MarketSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class IGApiClientTest {

    private IGApiClient igApiClient;

    @BeforeEach
    public void setup() {
        igApiClient = new IGApiClient();
    }

    @Test
    public void testSubmitMarketOrder() {
        igApiClient.submitMarketOrder("AAPL", 1.0, MarketSide.BUY);
    }

    @Test
    public void testGetPosition() {
        igApiClient.getPosition("AAPL");
    }
}