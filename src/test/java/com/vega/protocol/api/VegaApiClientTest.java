package com.vega.protocol.api;

import com.vega.protocol.model.LiquidityProvision;
import com.vega.protocol.model.Order;
import org.junit.jupiter.api.Test;

public class VegaApiClientTest {

    private final VegaApiClient vegaApiClient = new VegaApiClient();

    @Test
    public void testSubmitOrder() {
        vegaApiClient.submitOrder(new Order());
    }

    @Test
    public void testCancelOrder() {
        vegaApiClient.cancelOrder("1");
    }

    @Test
    public void testSubmitLiquidityProvision() {
        vegaApiClient.submitLiquidityProvision(new LiquidityProvision());
    }

    @Test
    public void testAmendLiquidityProvision() {
        vegaApiClient.amendLiquidityProvision(new LiquidityProvision());
    }
}
