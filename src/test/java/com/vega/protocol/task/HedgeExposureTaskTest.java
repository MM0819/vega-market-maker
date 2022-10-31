package com.vega.protocol.task;

import com.vega.protocol.api.BinanceApiClient;
import com.vega.protocol.api.IGApiClient;
import com.vega.protocol.constant.ReferencePriceSource;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.SleepUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

public class HedgeExposureTaskTest {

    private HedgeExposureTask hedgeExposureTask;
    private DataInitializer dataInitializer;
    private WebSocketInitializer webSocketInitializer;
    private PositionService positionService;
    private IGApiClient igApiClient;
    private BinanceApiClient binanceApiClient;
    private ReferencePriceStore referencePriceStore;
    private SleepUtils sleepUtils;

    private static final String MARKET_ID = "1";

    private HedgeExposureTask getHedgeExposureTask(
            final boolean enabled
    ) {
        return new HedgeExposureTask(dataInitializer, webSocketInitializer, MARKET_ID, enabled,
                ReferencePriceSource.BINANCE, "AAPL.CASH", "BTCUSDT",
                positionService, igApiClient, binanceApiClient, referencePriceStore, sleepUtils);
    }

    @BeforeEach
    public void setup() {
        dataInitializer = Mockito.mock(DataInitializer.class);
        webSocketInitializer = Mockito.mock(WebSocketInitializer.class);
        positionService = Mockito.mock(PositionService.class);
        igApiClient = Mockito.mock(IGApiClient.class);
        binanceApiClient = Mockito.mock(BinanceApiClient.class);
        referencePriceStore = Mockito.mock(ReferencePriceStore.class);
        sleepUtils = Mockito.mock(SleepUtils.class);
        hedgeExposureTask = getHedgeExposureTask(true);
    }

    @Test
    public void testExecute() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        hedgeExposureTask.execute();
    }

    @Test
    public void testExecuteNotInitialized() {
        hedgeExposureTask.execute();
        Mockito.when(dataInitializer.isInitialized()).thenReturn(false);
    }

    @Test
    public void testExecuteDisabled() {
        hedgeExposureTask = getHedgeExposureTask(false);
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        hedgeExposureTask.execute();
    }

    @Test
    public void testGetCronExpression() {
        String cron = hedgeExposureTask.getCronExpression();
        Assertions.assertEquals(cron, "0 * * * * *");
    }
}