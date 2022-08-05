package com.vega.protocol.ws;

import com.vega.protocol.store.MarketStore;
import org.java_websocket.handshake.HandshakeImpl1Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;

public class VegaWebSocketClientTest {

    private VegaWebSocketClient vegaWebSocketClient;
    private final MarketStore marketStore = Mockito.mock(MarketStore.class);

    @BeforeEach
    public void setup() {
        vegaWebSocketClient = new VegaWebSocketClient(marketStore, URI.create("wss://lb.testnet.vega.xyz/query"));
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