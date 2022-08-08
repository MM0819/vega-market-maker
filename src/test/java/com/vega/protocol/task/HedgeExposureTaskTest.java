package com.vega.protocol.task;

import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.LiquidityCommitment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class HedgeExposureTaskTest {

    private HedgeExposureTask hedgeExposureTask;
    private DataInitializer dataInitializer;
    private WebSocketInitializer webSocketInitializer;

    @BeforeEach
    public void setup() {
        dataInitializer = Mockito.mock(DataInitializer.class);
        webSocketInitializer = Mockito.mock(WebSocketInitializer.class);
        hedgeExposureTask = new HedgeExposureTask(dataInitializer, webSocketInitializer, true);
    }

    @Test
    public void testExecute() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        hedgeExposureTask.execute();
    }

    @Test
    public void testExecuteNotInitialized() {
        hedgeExposureTask.execute();
        Mockito.when(dataInitializer.isInitialized()).thenReturn(false);
    }

    @Test
    public void testExecuteDisabled() {
        hedgeExposureTask = new HedgeExposureTask(dataInitializer, webSocketInitializer, false);
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        hedgeExposureTask.execute();
    }

    @Test
    public void testGetCronExpression() {
        String cron = hedgeExposureTask.getCronExpression();
        Assertions.assertEquals(cron, "0 */10 * * * *");
    }
}