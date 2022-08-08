package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.MarketTradingMode;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.*;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.AppConfigStore;
import com.vega.protocol.store.OrderStore;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.PricingUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UpdateQuotesTaskTest {

    private static final String MARKET_ID = "1";
    private static final String PARTY_ID = "1";
    private static final String USDT = "USDT";

    private UpdateQuotesTask updateQuotesTask;
    private final ReferencePriceStore referencePriceStore = Mockito.mock(ReferencePriceStore.class);
    private final AppConfigStore appConfigStore = Mockito.mock(AppConfigStore.class);
    private final OrderStore orderStore = Mockito.mock(OrderStore.class);
    private final VegaApiClient vegaApiClient = Mockito.mock(VegaApiClient.class);
    private final MarketService marketService = Mockito.mock(MarketService.class);
    private final AccountService accountService = Mockito.mock(AccountService.class);
    private final PositionService positionService = Mockito.mock(PositionService.class);
    private final PricingUtils pricingUtils = Mockito.mock(PricingUtils.class);
    private final DataInitializer dataInitializer = Mockito.mock(DataInitializer.class);
    private final WebSocketInitializer webSocketInitializer = Mockito.mock(WebSocketInitializer.class);

    private AppConfig getAppConfig() {
        return new AppConfig()
                .setFee(0.001)
                .setSpread(0.005)
                .setOrderCount(10)
                .setBidSizeFactor(1.0)
                .setBidQuoteRange(0.05)
                .setAskLiquidityRange(1.0)
                .setBidLiquidityRange(0.999)
                .setAskSizeFactor(1.0)
                .setAskQuoteRange(0.05)
                .setPricingStepSize(0.1);
    }

    private UpdateQuotesTask getTask(
            final boolean enabled
    ) {
        return new UpdateQuotesTask(MARKET_ID, enabled, PARTY_ID, referencePriceStore,
                appConfigStore, orderStore, vegaApiClient, marketService, accountService, positionService,
                pricingUtils, dataInitializer, webSocketInitializer);
    }

    @BeforeEach
    public void setup() {
        updateQuotesTask = getTask(true);
    }

    private void execute(
            final BigDecimal exposure,
            final BigDecimal balance,
            final MarketTradingMode tradingMode
    ) {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market()
                .setSettlementAsset(USDT)
                .setTradingMode(tradingMode));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(balance);
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(exposure);
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(getAppConfig()));
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setMidPrice(BigDecimal.valueOf(20000))));
        List<Order> currentOrders = new ArrayList<>();
        for(int i=0; i<3; i++) {
            currentOrders.add(new Order()
                    .setSide(MarketSide.SELL)
                    .setId(String.valueOf(i+1))
                    .setPrice(BigDecimal.ONE));
        }
        for(int i=0; i<30; i++) {
            currentOrders.add(new Order()
                    .setSide(MarketSide.BUY)
                    .setId(String.valueOf(i+4))
                    .setPrice(BigDecimal.ONE));
        }
        Mockito.when(orderStore.getItems()).thenReturn(currentOrders);
        Mockito.when(pricingUtils.getScalingFactor(Mockito.anyDouble())).thenReturn(1d);
        Mockito.when(pricingUtils.getBidDistribution(
                        Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(),
                        Mockito.anyDouble(), Mockito.anyInt()))
                .thenReturn(List.of(new DistributionStep().setPrice(1d).setSize(1d)));
        Mockito.when(pricingUtils.getAskDistribution(
                        Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(),
                        Mockito.anyDouble(), Mockito.anyInt()))
                .thenReturn(List.of(
                        new DistributionStep().setPrice(1d).setSize(1d),
                        new DistributionStep().setPrice(1d).setSize(1d),
                        new DistributionStep().setPrice(1d).setSize(1d),
                        new DistributionStep().setPrice(1d).setSize(1d)
                ));
        updateQuotesTask.execute();
        int modifier = 1;
        if(balance.doubleValue() == 0) {
            modifier = 0;
        }
        Mockito.verify(vegaApiClient, Mockito.times(5 * modifier)).submitOrder(Mockito.any(Order.class), Mockito.anyString()); // TODO - fix assertion
        for(Order order : currentOrders.stream().filter(o -> o.getSide().equals(MarketSide.BUY)).toList()) {
            Mockito.verify(vegaApiClient, Mockito.times(modifier)).cancelOrder(order.getId(), PARTY_ID);
        }
        for(Order order : currentOrders.stream().filter(o -> o.getSide().equals(MarketSide.SELL)).toList()) {
            Mockito.verify(vegaApiClient, Mockito.times(modifier)).cancelOrder(order.getId(), PARTY_ID);
        }
    }

    @Test
    public void testExecuteDisabled() {
        updateQuotesTask = getTask(false);
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        updateQuotesTask.execute();
        Mockito.verify(vegaApiClient, Mockito.times(0)).submitLiquidityCommitment(
                Mockito.any(LiquidityCommitment.class), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testExecute() {
        execute(BigDecimal.ZERO, BigDecimal.valueOf(100000), MarketTradingMode.CONTINUOUS);
    }

    @Test
    public void testExecuteZerBalance() {
        execute(BigDecimal.ZERO, BigDecimal.ZERO, MarketTradingMode.CONTINUOUS);
    }

    @Test
    public void testExecuteLongPosition() {
        execute(BigDecimal.valueOf(1000L), BigDecimal.valueOf(100000), MarketTradingMode.MONITORING_AUCTION);
    }

    @Test
    public void testExecuteShortPosition() {
        execute(BigDecimal.valueOf(-1000L), BigDecimal.valueOf(100000), MarketTradingMode.CONTINUOUS);
    }

    @Test
    public void testExecuteAppConfigNotFound() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market()
                .setSettlementAsset(USDT)
                .setTradingMode(MarketTradingMode.CONTINUOUS));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        try {
            updateQuotesTask.execute();
            Assertions.fail();
        } catch(Exception e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.APP_CONFIG_NOT_FOUND);
        }
    }

    @Test
    public void testExecuteReferencePriceNotFound() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market()
                .setSettlementAsset(USDT)
                .setTradingMode(MarketTradingMode.CONTINUOUS));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(getAppConfig()));
        try {
            updateQuotesTask.execute();
            Assertions.fail();
        } catch(Exception e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.REFERENCE_PRICE_NOT_FOUND);
        }
    }

    @Test
    public void testExecuteNotInitialized() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(false);
        updateQuotesTask.execute();
        Mockito.verify(vegaApiClient, Mockito.times(0)).submitOrder(Mockito.any(Order.class), Mockito.anyString());
    }

    @Test
    public void testGetCronExpression() {
        Assertions.assertEquals("*/15 * * * * *", updateQuotesTask.getCronExpression());
    }
}