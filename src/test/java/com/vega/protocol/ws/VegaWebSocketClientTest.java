package com.vega.protocol.ws;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.constant.TimeInForce;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.store.MarketStore;
import com.vega.protocol.store.OrderStore;
import org.java_websocket.handshake.HandshakeImpl1Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.net.URI;

@EnabledIfEnvironmentVariable(named = "INT_TESTING_ENABLED", matches = "true")
public class VegaWebSocketClientTest {

    private VegaWebSocketClient vegaWebSocketClient;
    private VegaApiClient vegaApiClient;
    private MarketStore marketStore;
    private OrderStore orderStore;
    private static final String WALLET_URL = "http://localhost:1789";
    private static final String WALLET_USER = "trading";
    private static final String WALLET_PASSWORD = "password123";
    private static final String NODE_URL = "https://lb.testnet.vega.xyz/datanode/rest";
    private static final String MARKET_ID = "10c4b1114d2f6fda239b73d018bca55888b6018f0ac70029972a17fea0a6a56e";
    private static final String PARTY_ID = "6817f2b4d9464716c6756d2827d893872b1d33839e211c27a650629e428dc35c";

    @BeforeEach
    public void setup() {
        marketStore = Mockito.mock(MarketStore.class);
        orderStore = Mockito.mock(OrderStore.class);
        vegaWebSocketClient = new VegaWebSocketClient(
                PARTY_ID, marketStore, orderStore, URI.create("wss://lb.testnet.vega.xyz/query"));
        vegaApiClient = new VegaApiClient(WALLET_URL, WALLET_USER, WALLET_PASSWORD, NODE_URL, MARKET_ID, marketStore);
    }

    private void connectAndClose() throws InterruptedException {
        vegaWebSocketClient.connect();
        Thread.sleep(10000L);
        vegaApiClient.submitOrder(new Order()
                .setSide(MarketSide.BUY)
                .setPrice(BigDecimal.ONE)
                .setSize(BigDecimal.ONE)
                .setType(OrderType.LIMIT)
                .setTimeInForce(TimeInForce.GTC), PARTY_ID);
        Thread.sleep(10000L);
        Mockito.verify(marketStore, Mockito.atLeast(1)).update(Mockito.any(Market.class));
        Mockito.verify(orderStore, Mockito.atLeast(1)).update(Mockito.any(Order.class));
        vegaApiClient.getOpenOrders(PARTY_ID).forEach(o -> vegaApiClient.cancelOrder(o.getId(), PARTY_ID));
        Thread.sleep(10000L);
        vegaWebSocketClient.close();
        Thread.sleep(1000L);
    }

    @Test
    public void testConnectAndClose() throws InterruptedException {
        connectAndClose();
    }

    @Test
    public void testHandleErrors() throws InterruptedException {
        Mockito.doThrow(RuntimeException.class).when(marketStore).update(Mockito.any(Market.class));
        Mockito.doThrow(RuntimeException.class).when(orderStore).update(Mockito.any(Order.class));
        connectAndClose();
    }

    @Test
    public void testOnMessageWithInvalidJSON() {
        vegaWebSocketClient.onMessage("invalid JSON");
    }

    @Test
    public void testOnError() {
        vegaWebSocketClient.onError(new RuntimeException());
    }

    @Test
    public void testOnOpenError() {
        vegaWebSocketClient.onOpen(new HandshakeImpl1Server());
    }
}