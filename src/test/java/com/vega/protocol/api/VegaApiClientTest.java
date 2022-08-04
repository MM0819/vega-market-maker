package com.vega.protocol.api;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.model.LiquidityProvision;
import com.vega.protocol.model.Order;
import com.vega.protocol.utils.SleepUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class VegaApiClientTest {

    private final SleepUtils sleepUtils = new SleepUtils();

    private static final String WALLET_URL = "http://localhost:1789";
    private static final String WALLET_USER = "trading";
    private static final String WALLET_PASSWORD = "password123";
    private static final String NODE_URL = "https://lb.testnet.vega.xyz/datanode/rest";
    private static final String MARKET_ID = "10c4b1114d2f6fda239b73d018bca55888b6018f0ac70029972a17fea0a6a56e";
    private static final String PARTY_ID = "6817f2b4d9464716c6756d2827d893872b1d33839e211c27a650629e428dc35c";

    private final VegaApiClient vegaApiClient = new VegaApiClient(
            WALLET_URL, WALLET_USER, WALLET_PASSWORD, NODE_URL, MARKET_ID
    );

    private void submitOrder() {
        Order order = new Order()
                .setSide(MarketSide.BUY)
                .setSize(BigDecimal.ONE)
                .setPrice(BigDecimal.ONE)
                .setType(OrderType.LIMIT);
        Optional<String> txHash = vegaApiClient.submitOrder(order, PARTY_ID);
        Assertions.assertTrue(txHash.isPresent());
    }

    private void cancelOrders() {
        List<Order> orders = vegaApiClient.getOpenOrders(PARTY_ID);
        for(Order order : orders) {
            Optional<String> txHash = vegaApiClient.cancelOrder(order.getId(), PARTY_ID);
            Assertions.assertTrue(txHash.isPresent());
        }
    }

    @Test
    public void testSubmitAndCancelOrders() {
        submitOrder();
        sleepUtils.sleep(5000L);
        cancelOrders();
        sleepUtils.sleep(5000L);
        List<Order> orders = vegaApiClient.getOpenOrders(PARTY_ID);
        Assertions.assertEquals(orders.size(), 0);
    }

    @Test
    public void testSubmitLiquidityProvision() {
        vegaApiClient.submitLiquidityProvision(new LiquidityProvision(), PARTY_ID);
    }

    @Test
    public void testAmendLiquidityProvision() {
        vegaApiClient.amendLiquidityProvision(new LiquidityProvision(), PARTY_ID);
    }
}
