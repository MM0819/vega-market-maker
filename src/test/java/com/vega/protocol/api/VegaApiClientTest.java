package com.vega.protocol.api;

import com.mashape.unirest.http.Unirest;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.model.LiquidityProvision;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.store.MarketStore;
import com.vega.protocol.utils.SleepUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@DisabledIfEnvironmentVariable(named = "TRAVIS_CI", matches = "true")
public class VegaApiClientTest {

    private final SleepUtils sleepUtils = new SleepUtils();

    private static final String WALLET_URL = "http://localhost:1789";
    private static final String WALLET_USER = "trading";
    private static final String WALLET_PASSWORD = "password123";
    private static final String NODE_URL = "https://lb.testnet.vega.xyz/datanode/rest";
    private static final String MARKET_ID = "10c4b1114d2f6fda239b73d018bca55888b6018f0ac70029972a17fea0a6a56e";
    private static final String PARTY_ID = "6817f2b4d9464716c6756d2827d893872b1d33839e211c27a650629e428dc35c";
    private final MarketStore marketStore = Mockito.mock(MarketStore.class);

    private final VegaApiClient vegaApiClient = new VegaApiClient(
            WALLET_URL, WALLET_USER, WALLET_PASSWORD, NODE_URL, MARKET_ID, marketStore
    );

    // TODO - we should run the wallet from within this test so that it works on CI

    private Optional<String> submitOrder() {
        Order order = new Order()
                .setSide(MarketSide.BUY)
                .setSize(BigDecimal.ONE)
                .setPrice(BigDecimal.ONE)
                .setType(OrderType.LIMIT);
        return vegaApiClient.submitOrder(order, PARTY_ID);
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
        Optional<String> txHash = submitOrder();
        Assertions.assertTrue(txHash.isPresent());
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

    @Test
    public void testGetMarkets() {
        List<Market> markets = vegaApiClient.getMarkets();
        Assertions.assertTrue(markets.size() > 0);
    }

    @Test
    public void testGetTokenWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            Assertions.assertNotNull(mockStatic);
            Optional<String> token = vegaApiClient.getToken();
            Assertions.assertTrue(token.isEmpty());
        }
    }

    @Test
    public void testSubmitOrderWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            Assertions.assertNotNull(mockStatic);
            Optional<String> txHash = submitOrder();
            Assertions.assertTrue(txHash.isEmpty());
        }
    }

    @Test
    public void testCancelOrderWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            Assertions.assertNotNull(mockStatic);
            Optional<String> txHash = vegaApiClient.cancelOrder("1", PARTY_ID);
            Assertions.assertTrue(txHash.isEmpty());
        }
    }

    @Test
    public void testGetOrdersWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            Assertions.assertNotNull(mockStatic);
            List<Order> orders = vegaApiClient.getOpenOrders(PARTY_ID);
            Assertions.assertEquals(0, orders.size());
        }
    }

    @Test
    public void testGetMarketsWithError() {
        try(MockedStatic<Unirest> mockStatic = Mockito.mockStatic(Unirest.class)) {
            Assertions.assertNotNull(mockStatic);
            List<Market> markets = vegaApiClient.getMarkets();
            Assertions.assertEquals(0, markets.size());
        }
    }
}
