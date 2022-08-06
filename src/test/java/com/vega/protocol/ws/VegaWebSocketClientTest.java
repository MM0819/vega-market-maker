package com.vega.protocol.ws;

import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.store.MarketStore;
import com.vega.protocol.store.OrderStore;
import org.apache.commons.io.IOUtils;
import org.java_websocket.handshake.HandshakeImpl1Server;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class VegaWebSocketClientTest {

    private VegaWebSocketClient vegaWebSocketClient;
    private MarketStore marketStore;
    private OrderStore orderStore;
    private static final String PARTY_ID = "6817f2b4d9464716c6756d2827d893872b1d33839e211c27a650629e428dc35c";

    @BeforeEach
    public void setup() {
        marketStore = Mockito.mock(MarketStore.class);
        orderStore = Mockito.mock(OrderStore.class);
        vegaWebSocketClient = new VegaWebSocketClient(
                PARTY_ID, marketStore, orderStore, URI.create("wss://lb.testnet.vega.xyz/query"));
    }

//    private void connectAndClose() throws InterruptedException {
//        vegaWebSocketClient.connect();
//        Thread.sleep(10000L);
//        vegaApiClient.submitOrder(new Order()
//                .setSide(MarketSide.BUY)
//                .setPrice(BigDecimal.ONE)
//                .setSize(BigDecimal.ONE)
//                .setType(OrderType.LIMIT)
//                .setTimeInForce(TimeInForce.GTC), PARTY_ID);
//        Thread.sleep(10000L);
//        Mockito.verify(marketStore, Mockito.atLeast(1)).update(Mockito.any(Market.class));
//        Mockito.verify(orderStore, Mockito.atLeast(1)).update(Mockito.any(Order.class));
//        vegaApiClient.getOpenOrders(PARTY_ID).forEach(o -> vegaApiClient.cancelOrder(o.getId(), PARTY_ID));
//        Thread.sleep(10000L);
//        vegaWebSocketClient.close();
//        Thread.sleep(1000L);
//    }
//
//    @Test
//    public void testConnectAndClose() throws InterruptedException {
//        connectAndClose();
//    }
//
//    @Test
//    public void testHandleErrors() throws InterruptedException {
//        Mockito.doThrow(RuntimeException.class).when(marketStore).update(Mockito.any(Market.class));
//        Mockito.doThrow(RuntimeException.class).when(orderStore).update(Mockito.any(Order.class));
//        connectAndClose();
//    }

    @Test
    public void testHandleMarkets() {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-markets-ws.json")) {
            String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(marketsJson);
            Mockito.verify(marketStore, Mockito.times(1)).update(Mockito.any(Market.class));
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testHandleOrders() {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-orders-ws.json")) {
            String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(marketsJson);
            Mockito.verify(orderStore, Mockito.times(1)).update(Mockito.any(Order.class));
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testHandleMarketsWithError() {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-markets-ws-invalid.json")) {
            String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(marketsJson);
            Mockito.verify(marketStore, Mockito.times(0)).update(Mockito.any(Market.class));
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testHandleOrdersWithError() {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-orders-ws-invalid.json")) {
            String marketsJson = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            vegaWebSocketClient.onMessage(marketsJson);
            Mockito.verify(orderStore, Mockito.times(0)).update(Mockito.any(Order.class));
        } catch (Exception e) {
            Assertions.fail();
        }
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