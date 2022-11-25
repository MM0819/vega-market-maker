package com.vega.protocol.task;

import com.vega.protocol.api.BinanceApiClient;
import com.vega.protocol.api.IGApiClient;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.ReferencePriceSource;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.trading.ReferencePrice;
import com.vega.protocol.service.OrderService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.ReferencePriceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class HedgeExposureTaskTest {

    private HedgeExposureTask hedgeExposureTask;
    private DataInitializer dataInitializer;
    private WebSocketInitializer webSocketInitializer;
    private PositionService positionService;
    private OrderService orderService;
    private IGApiClient igApiClient;
    private BinanceApiClient binanceApiClient;
    private ReferencePriceStore referencePriceStore;

    private static final String MARKET_ID = "1";
    private static final String PARTY_ID = "1";

    private HedgeExposureTask getHedgeExposureTask() {
        return new HedgeExposureTask(dataInitializer, webSocketInitializer, positionService, igApiClient,
                binanceApiClient, referencePriceStore, orderService);
    }

    private ReferencePrice referencePrice() {
        return new ReferencePrice()
                .setAskPrice(1.0)
                .setBidPrice(1.0)
                .setAskSize(1.0)
                .setBidSize(1.0)
                .setMidPrice(1.0);
    }

    @BeforeEach
    public void setup() {
        dataInitializer = Mockito.mock(DataInitializer.class);
        webSocketInitializer = Mockito.mock(WebSocketInitializer.class);
        positionService = Mockito.mock(PositionService.class);
        igApiClient = Mockito.mock(IGApiClient.class);
        binanceApiClient = Mockito.mock(BinanceApiClient.class);
        referencePriceStore = Mockito.mock(ReferencePriceStore.class);
        orderService = Mockito.mock(OrderService.class);
        hedgeExposureTask = getHedgeExposureTask();
    }

    @Test
    public void testExecute() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(positionService.getExposure(MARKET_ID, PARTY_ID)).thenReturn(1.0);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(referencePrice()));
        MarketConfig marketConfig = new MarketConfig()
                .setMarketId(MARKET_ID)
                .setReferencePriceSource(ReferencePriceSource.BINANCE);
        hedgeExposureTask.execute(marketConfig);
    }

    @Test
    public void testExecuteWithZeroExposure() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(positionService.getExposure(MARKET_ID, PARTY_ID)).thenReturn(0.0);
        hedgeExposureTask.execute(new MarketConfig());
        Mockito.verify(binanceApiClient, Mockito.times(0))
                .submitMarketOrder(Mockito.anyString(), Mockito.anyDouble(), Mockito.any(MarketSide.class));
        Mockito.verify(igApiClient, Mockito.times(0))
                .submitMarketOrder(Mockito.anyString(), Mockito.anyDouble(), Mockito.any(MarketSide.class));
    }

    @Test
    public void testExecuteNotInitialized() {
        hedgeExposureTask.execute(new MarketConfig());
        Mockito.when(dataInitializer.isInitialized()).thenReturn(false);
        Mockito.verify(binanceApiClient, Mockito.times(0))
                .submitMarketOrder(Mockito.anyString(), Mockito.anyDouble(), Mockito.any(MarketSide.class));
        Mockito.verify(igApiClient, Mockito.times(0))
                .submitMarketOrder(Mockito.anyString(), Mockito.anyDouble(), Mockito.any(MarketSide.class));
    }

    @Test
    public void testExecuteDisabled() {
        hedgeExposureTask = getHedgeExposureTask();
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        hedgeExposureTask.execute(new MarketConfig());
        Mockito.verify(binanceApiClient, Mockito.times(0))
                .submitMarketOrder(Mockito.anyString(), Mockito.anyDouble(), Mockito.any(MarketSide.class));
        Mockito.verify(igApiClient, Mockito.times(0))
                .submitMarketOrder(Mockito.anyString(), Mockito.anyDouble(), Mockito.any(MarketSide.class));
    }
}