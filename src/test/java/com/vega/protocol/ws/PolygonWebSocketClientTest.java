package com.vega.protocol.ws;

import com.vega.protocol.store.ReferencePriceStore;
import org.java_websocket.handshake.HandshakeImpl1Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;

public class PolygonWebSocketClientTest {

    private static final String SYMBOL = "AAPL";
    private PolygonWebSocketClient polygonWebSocketClient;
    private final ReferencePriceStore referencePriceStore = Mockito.mock(ReferencePriceStore.class);

    @BeforeEach
    public void setup() {
        polygonWebSocketClient = new PolygonWebSocketClient(
                URI.create("wss://socket.polygon.io/stocks"), SYMBOL, referencePriceStore);
    }

    @Test
    public void testConnectAndClose() throws InterruptedException {
        polygonWebSocketClient.connect();
        Thread.sleep(5000L);
        polygonWebSocketClient.close();
        Thread.sleep(1000L);
    }

    @Test
    public void testOnError() {
        polygonWebSocketClient.onError(new RuntimeException());
    }

    @Test
    public void testOnOpenError() {
        polygonWebSocketClient.onOpen(new HandshakeImpl1Server());
    }
}