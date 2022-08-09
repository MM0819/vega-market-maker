package com.vega.protocol.initializer;

import com.vega.protocol.constant.ReferencePriceSource;
import com.vega.protocol.service.OrderService;
import com.vega.protocol.store.*;
import com.vega.protocol.utils.DecimalUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class WebSocketInitializerTest {

    private WebSocketInitializer webSocketInitializer;
    private final ReferencePriceStore referencePriceStore = Mockito.mock(ReferencePriceStore.class);
    private final MarketStore marketStore = Mockito.mock(MarketStore.class);
    private final OrderStore orderStore = Mockito.mock(OrderStore.class);
    private final PositionStore positionStore = Mockito.mock(PositionStore.class);
    private final AccountStore accountStore = Mockito.mock(AccountStore.class);
    private final AssetStore assetStore = Mockito.mock(AssetStore.class);
    private final LiquidityCommitmentStore liquidityCommitmentStore = Mockito.mock(LiquidityCommitmentStore.class);
    private final DecimalUtils decimalUtils = Mockito.mock(DecimalUtils.class);
    private final OrderService orderService = Mockito.mock(OrderService.class);
    private static final String PARTY_ID = "1";
    private static final String MARKET_ID = "1";

    private WebSocketInitializer getWebSocketInitializer(boolean enabled, ReferencePriceSource source) {
        return new WebSocketInitializer(
                "wss://lb.testnet.vega.xyz/query",
                "wss://stream.binance.com:9443/stream",
                "wss://socket.polygon.io/stocks",
                enabled, enabled, enabled, "BTCUSDT", source, PARTY_ID, MARKET_ID,
                referencePriceStore, marketStore, orderStore, positionStore, accountStore, assetStore,
                liquidityCommitmentStore, decimalUtils, orderService
        );
    }

    @BeforeEach
    public void setup() {
        webSocketInitializer = getWebSocketInitializer(true, ReferencePriceSource.BINANCE);
    }

    @Test
    public void testInitializeBinance() throws InterruptedException {
        webSocketInitializer.initialize();
        Thread.sleep(2000L);
        Assertions.assertNull(webSocketInitializer.getPolygonWebSocketClient());
        Assertions.assertTrue(webSocketInitializer.getVegaWebSocketClient().isOpen());
        Assertions.assertTrue(webSocketInitializer.getBinanceWebSocketClient().isOpen());
        Assertions.assertTrue(webSocketInitializer.isBinanceWebSocketInitialized());
    }

    @Test
    public void testInitializePolygon() throws InterruptedException {
        webSocketInitializer = getWebSocketInitializer(true, ReferencePriceSource.POLYGON);
        webSocketInitializer.initialize();
        Thread.sleep(1000L);
        Assertions.assertNull(webSocketInitializer.getBinanceWebSocketClient());
        Assertions.assertTrue(webSocketInitializer.getVegaWebSocketClient().isOpen());
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
        Assertions.assertNull(webSocketInitializer.getVegaWebSocketClient());
        Assertions.assertFalse(webSocketInitializer.isVegaWebSocketsInitialized());
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

    @Test
    public void testKeepAliveVegaClosed() throws InterruptedException {
        webSocketInitializer = getWebSocketInitializer(true, ReferencePriceSource.BINANCE);
        webSocketInitializer.initialize();
        Thread.sleep(500L);
        webSocketInitializer.getVegaWebSocketClient().close();
        Thread.sleep(500L);
        webSocketInitializer.keepWebSocketsAlive();
    }
}