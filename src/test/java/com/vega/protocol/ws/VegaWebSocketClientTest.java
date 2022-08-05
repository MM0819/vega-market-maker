package com.vega.protocol.ws;

import com.vega.protocol.store.MarketStore;
import com.vega.protocol.store.OrderStore;
import org.java_websocket.handshake.HandshakeImpl1Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;

public class VegaWebSocketClientTest {

    private VegaWebSocketClient vegaWebSocketClient;
    private final MarketStore marketStore = Mockito.mock(MarketStore.class);
    private final OrderStore orderStore = Mockito.mock(OrderStore.class);
    private static final String PARTY_ID = "1";

    @BeforeEach
    public void setup() {
        vegaWebSocketClient = new VegaWebSocketClient(
                PARTY_ID, marketStore, orderStore, URI.create("wss://lb.testnet.vega.xyz/query"));
    }

    @Test
    public void testConnectAndClose() throws InterruptedException {
        vegaWebSocketClient.connect();
        Thread.sleep(5000L);
        vegaWebSocketClient.close();
        Thread.sleep(1000L);
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