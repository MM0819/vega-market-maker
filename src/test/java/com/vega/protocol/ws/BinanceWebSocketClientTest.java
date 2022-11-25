package com.vega.protocol.ws;

import com.vega.protocol.store.ReferencePriceStore;
import org.java_websocket.handshake.HandshakeImpl1Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;

public class BinanceWebSocketClientTest {

    private BinanceWebSocketClient binanceWebSocketClient;
    private final ReferencePriceStore referencePriceStore = Mockito.mock(ReferencePriceStore.class);

    @BeforeEach
    public void setup() {
        binanceWebSocketClient = new BinanceWebSocketClient(
                URI.create("wss://stream.binance.com:9443/stream"), referencePriceStore);
    }

    @Test
    public void testOnError() {
        binanceWebSocketClient.onError(new RuntimeException());
    }

    @Test
    public void testOnOpenError() {
        binanceWebSocketClient.onOpen(new HandshakeImpl1Server());
    }

    @Test
    public void testOnMessageError() {
        binanceWebSocketClient.onMessage("");
    }
}