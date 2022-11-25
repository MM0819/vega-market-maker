package com.vega.protocol.initializer;

import com.vega.protocol.constant.ReferencePriceSource;
import com.vega.protocol.store.ReferencePriceStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class WebSocketInitializerTest {

    private WebSocketInitializer webSocketInitializer;
    private final ReferencePriceStore referencePriceStore = Mockito.mock(ReferencePriceStore.class);

    private WebSocketInitializer getWebSocketInitializer(boolean enabled, ReferencePriceSource source) {
        return new WebSocketInitializer(
                "wss://stream.binance.com:9443/stream",
                "wss://socket.polygon.io/stocks",
                enabled, enabled, referencePriceStore
        );
    }

    @BeforeEach
    public void setup() {
        webSocketInitializer = getWebSocketInitializer(true, ReferencePriceSource.BINANCE);
    }

    @Test
    public void testInitializeBinance() throws InterruptedException {
        webSocketInitializer.initialize();
        Thread.sleep(3000L);
        Assertions.assertNull(webSocketInitializer.getPolygonWebSocketClient());
        Assertions.assertTrue(webSocketInitializer.getBinanceWebSocketClient().isOpen());
        Assertions.assertTrue(webSocketInitializer.isBinanceWebSocketInitialized());
    }

    @Test
    public void testInitializePolygon() throws InterruptedException {
        webSocketInitializer = getWebSocketInitializer(true, ReferencePriceSource.POLYGON);
        webSocketInitializer.initialize();
        Thread.sleep(2000L);
        Assertions.assertNull(webSocketInitializer.getBinanceWebSocketClient());
        Assertions.assertTrue(webSocketInitializer.getPolygonWebSocketClient().isOpen());
        Assertions.assertTrue(webSocketInitializer.isPolygonWebSocketInitialized());
    }

    @Test
    public void testInitializeDisabled() throws InterruptedException {
        webSocketInitializer = getWebSocketInitializer(false, ReferencePriceSource.BINANCE);
        webSocketInitializer.initialize();
        Thread.sleep(200L);
        Assertions.assertNull(webSocketInitializer.getPolygonWebSocketClient());
        Assertions.assertNull(webSocketInitializer.getBinanceWebSocketClient());
    }

    @Test
    public void testKeepAliveNotInitialized() {
        webSocketInitializer.keepWebSocketsAlive();
    }

    @Test
    public void testKeepAliveInitializedButNotClosedBinance() {
        webSocketInitializer.initialize();
        webSocketInitializer.keepWebSocketsAlive();
    }

    @Test
    public void testKeepAliveInitializedButNotClosedPolygon() {
        webSocketInitializer = getWebSocketInitializer(true, ReferencePriceSource.POLYGON);
        webSocketInitializer.initialize();
        webSocketInitializer.keepWebSocketsAlive();
    }

    @Test
    public void testKeepAlivePolygonClosed() throws InterruptedException {
        webSocketInitializer = getWebSocketInitializer(true, ReferencePriceSource.POLYGON);
        webSocketInitializer.initialize();
        Thread.sleep(500L);
        webSocketInitializer.getPolygonWebSocketClient().close();
        Thread.sleep(500L);
        webSocketInitializer.keepWebSocketsAlive();
    }

    @Test
    public void testKeepAliveBinanceClosed() throws InterruptedException {
        webSocketInitializer = getWebSocketInitializer(true, ReferencePriceSource.BINANCE);
        webSocketInitializer.initialize();
        Thread.sleep(2000L);
        webSocketInitializer.getBinanceWebSocketClient().close();
        Thread.sleep(2000L);
        webSocketInitializer.keepWebSocketsAlive();
    }
}