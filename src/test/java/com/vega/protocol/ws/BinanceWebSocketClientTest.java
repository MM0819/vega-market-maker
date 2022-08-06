package com.vega.protocol.ws;

import com.vega.protocol.store.ReferencePriceStore;
import org.java_websocket.handshake.HandshakeImpl1Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mockito;

import java.net.URI;

@EnabledIfEnvironmentVariable(named = "INT_TESTING_ENABLED", matches = "true")
public class BinanceWebSocketClientTest {

    private static final String SYMBOL = "BTCUSDT";

    private BinanceWebSocketClient binanceWebSocketClient;
    private final ReferencePriceStore referencePriceStore = Mockito.mock(ReferencePriceStore.class);

    @BeforeEach
    public void setup() {
        binanceWebSocketClient = new BinanceWebSocketClient(
                URI.create("wss://stream.binance.com:9443/stream"), SYMBOL, referencePriceStore);
    }

    @Test
    public void testConnectAndClose() throws InterruptedException {
        binanceWebSocketClient.connect();
        Thread.sleep(5000L);
        binanceWebSocketClient.close();
        Thread.sleep(1000L);
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