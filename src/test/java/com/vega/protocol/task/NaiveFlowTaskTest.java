package com.vega.protocol.task;

import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.grpc.client.VegaGrpcClient;
import com.vega.protocol.helper.TestingHelper;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.OrderService;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.DecimalUtils;
import com.vega.protocol.utils.SleepUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vega.Markets;
import vega.Vega;

import java.math.BigDecimal;
import java.util.Optional;

public class NaiveFlowTaskTest {

    private static final String MARKET_ID = "1";
    private static final String PARTY_ID = "1";
    private static final String USDT = "USDT";

    private NaiveFlowTask naiveFlowTask;
    private final VegaGrpcClient vegaGrpcClient = Mockito.mock(VegaGrpcClient.class);
    private final MarketService marketService = Mockito.mock(MarketService.class);
    private final OrderService orderService = Mockito.mock(OrderService.class);
    private final ReferencePriceStore referencePriceStore = Mockito.mock(ReferencePriceStore.class);
    private final DataInitializer dataInitializer = Mockito.mock(DataInitializer .class);
    private final WebSocketInitializer webSocketInitializer = Mockito.mock(WebSocketInitializer.class);
    private final SleepUtils sleepUtils = Mockito.mock(SleepUtils.class);
    private final DecimalUtils decimalUtils = Mockito.mock(DecimalUtils.class);

    private NaiveFlowTask getNaiveFlowTask(
            boolean enabled
    ) {
        return new NaiveFlowTask(PARTY_ID, vegaGrpcClient, marketService,
                orderService, referencePriceStore, dataInitializer, webSocketInitializer, sleepUtils, decimalUtils);
    }

    @BeforeEach
    public void setup() {
        naiveFlowTask = getNaiveFlowTask(true);
    }

    @Test
    public void testExecute() {
        var market = TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, USDT);
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isPolygonWebSocketInitialized()).thenReturn(true);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(new ReferencePrice()));
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(market);
        Mockito.when(decimalUtils.convertFromDecimals(Mockito.anyLong(), Mockito.anyDouble()))
                .thenReturn(new BigDecimal("1"));
        int count = 20;
        for(int i=0; i<count; i++) {
            Mockito.when(orderService.getOtherSide(Mockito.any()))
                    .thenReturn(i % 2 == 0 ? Vega.Side.SIDE_BUY : Vega.Side.SIDE_SELL);
            MarketConfig marketConfig = new MarketConfig().setMarketId(MARKET_ID);
            naiveFlowTask.execute(marketConfig);
        }
        Mockito.verify(vegaGrpcClient, Mockito.times(count)).submitOrder(
                Mockito.isNull(), Mockito.anyLong(),
                Mockito.any(Vega.Side.class), Mockito.any(Vega.Order.TimeInForce.class),
                Mockito.any(Vega.Order.Type.class), Mockito.anyString(), Mockito.anyString()
        );
    }

    @Test
    public void testExecuteDisabled() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        naiveFlowTask = getNaiveFlowTask(false);
        naiveFlowTask.execute(new MarketConfig());
        Mockito.verify(vegaGrpcClient, Mockito.times(0)).submitOrder(
                Mockito.anyString(), Mockito.anyLong(),
                Mockito.any(Vega.Side.class), Mockito.any(Vega.Order.TimeInForce.class),
                Mockito.any(Vega.Order.Type.class), Mockito.anyString(), Mockito.anyString()
        );
    }

    @Test
    public void testExecuteNotInitialized() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(false);
        naiveFlowTask = getNaiveFlowTask(false);
        naiveFlowTask.execute(new MarketConfig());
        Mockito.verify(vegaGrpcClient, Mockito.times(0)).submitOrder(
                Mockito.anyString(), Mockito.anyLong(),
                Mockito.any(Vega.Side.class), Mockito.any(Vega.Order.TimeInForce.class),
                Mockito.any(Vega.Order.Type.class), Mockito.anyString(), Mockito.anyString()
        );
    }

    @Test
    public void testInitializedFalseWhenDataNotInitialized() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(false);
        naiveFlowTask = getNaiveFlowTask(true);
        boolean result = naiveFlowTask.isInitialized();
        Assertions.assertFalse(result);
    }

    @Test
    public void testInitializedFalseWhenBinanceNotInitialized() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(false);
        naiveFlowTask = getNaiveFlowTask(true);
        boolean result = naiveFlowTask.isInitialized();
        Assertions.assertFalse(result);
    }

    @Test
    public void testInitializedTrueWhenPolygonInitialized() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isPolygonWebSocketInitialized()).thenReturn(true);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(new ReferencePrice()));
        naiveFlowTask = getNaiveFlowTask(true);
        boolean result = naiveFlowTask.isInitialized();
        Assertions.assertTrue(result);
    }

    @Test
    public void testInitializedTrueWhenBinanceInitialized() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(new ReferencePrice()));
        naiveFlowTask = getNaiveFlowTask(true);
        boolean result = naiveFlowTask.isInitialized();
        Assertions.assertTrue(result);
    }

    @Test
    public void testInitializedTrueWhenBinanceAndPolygonInitialized() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isPolygonWebSocketInitialized()).thenReturn(true);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(new ReferencePrice()));
        naiveFlowTask = getNaiveFlowTask(true);
        boolean result = naiveFlowTask.isInitialized();
        Assertions.assertTrue(result);
    }
    @Test
    public void testInitializedTrueWhenPolygonInitializedAndDataNotInitialized() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(false);
        Mockito.when(webSocketInitializer.isPolygonWebSocketInitialized()).thenReturn(true);
        naiveFlowTask = getNaiveFlowTask(true);
        boolean result = naiveFlowTask.isInitialized();
        Assertions.assertFalse(result);
    }

    @Test
    public void testInitializedTrueWhenBinanceInitializedAndDataNotInitialized() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(false);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        naiveFlowTask = getNaiveFlowTask(true);
        boolean result = naiveFlowTask.isInitialized();
        Assertions.assertFalse(result);
    }

    @Test
    public void testInitializedTrueWhenBinanceAndPolygonInitializedAndDataNotInitialized() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(false);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isPolygonWebSocketInitialized()).thenReturn(true);
        naiveFlowTask = getNaiveFlowTask(true);
        boolean result = naiveFlowTask.isInitialized();
        Assertions.assertFalse(result);
    }
}