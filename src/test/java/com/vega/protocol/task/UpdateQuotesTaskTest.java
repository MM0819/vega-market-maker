package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.MarketTradingMode;
import com.vega.protocol.constant.OrderStatus;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.*;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.LiquidityCommitmentStore;
import com.vega.protocol.store.NetworkParameterStore;
import com.vega.protocol.store.OrderStore;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.PricingUtils;
import com.vega.protocol.utils.QuantUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UpdateQuotesTaskTest {

    private static final String TAU_SCALING_PARAM = "market.liquidity.probabilityOfTrading.tau.scaling";
    private static final String MAX_BATCH_SIZE_PARAM = "spam.protection.max.batchSize";
    private static final String STAKE_TO_SISKAS_PARAM = "market.liquidity.stakeToCcySiskas";

    private static final String MARKET_ID = "1";
    private static final String PARTY_ID = "1";
    private static final String USDT = "USDT";

    private UpdateQuotesTask updateQuotesTask;
    private final ReferencePriceStore referencePriceStore = Mockito.mock(ReferencePriceStore.class);
    private final OrderStore orderStore = Mockito.mock(OrderStore.class);
    private final VegaApiClient vegaApiClient = Mockito.mock(VegaApiClient.class);
    private final MarketService marketService = Mockito.mock(MarketService.class);
    private final AccountService accountService = Mockito.mock(AccountService.class);
    private final PositionService positionService = Mockito.mock(PositionService.class);
    private final PricingUtils pricingUtils = Mockito.mock(PricingUtils.class);
    private final QuantUtils quantUtils = Mockito.mock(QuantUtils.class);
    private final DataInitializer dataInitializer = Mockito.mock(DataInitializer.class);
    private final WebSocketInitializer webSocketInitializer = Mockito.mock(WebSocketInitializer.class);
    private final LiquidityCommitmentStore liquidityCommitmentStore = Mockito.mock(LiquidityCommitmentStore.class);
    private final NetworkParameterStore networkParameterStore = Mockito.mock(NetworkParameterStore.class);
    private final TradingConfigRepository tradingConfigRepository = Mockito.mock(TradingConfigRepository.class);

    private UpdateQuotesTask getTask(
            final boolean enabled
    ) {
        return new UpdateQuotesTask(referencePriceStore, orderStore,
                liquidityCommitmentStore, networkParameterStore, vegaApiClient, marketService, accountService,
                positionService, pricingUtils, quantUtils, dataInitializer, webSocketInitializer,
                tradingConfigRepository);
    }

    @BeforeEach
    public void setup() {
        updateQuotesTask = getTask(true);
    }

    private void execute(
            final BigDecimal exposure,
            final BigDecimal balance,
            final MarketTradingMode tradingMode,
            final int bidDistributionSize,
            final int askDistributionSize
    ) {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market()
                .setSettlementAsset(USDT)
                .setTradingMode(tradingMode));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(balance);
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(exposure);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(new ReferencePrice()
                .setAskPrice(BigDecimal.valueOf(20001))
                .setBidPrice(BigDecimal.valueOf(19999))
                .setMidPrice(BigDecimal.valueOf(20000))));
        Mockito.when(networkParameterStore.getById(MAX_BATCH_SIZE_PARAM))
                .thenReturn(Optional.of(new NetworkParameter().setValue("100").setId(MAX_BATCH_SIZE_PARAM)));
        List<Order> currentOrders = new ArrayList<>();
        for(int i=0; i<4; i++) {
            currentOrders.add(new Order()
                    .setSide(MarketSide.SELL)
                    .setId(String.valueOf(i+1))
                    .setPrice(BigDecimal.ONE)
                    .setSize(BigDecimal.TEN)
                    .setIsPeggedOrder(false)
                    .setStatus(i % 2 == 0 ? OrderStatus.ACTIVE : OrderStatus.CANCELLED));
        }
        for(int i=0; i<4; i++) {
            currentOrders.add(new Order()
                    .setSide(MarketSide.BUY)
                    .setId(String.valueOf(i+4))
                    .setPrice(BigDecimal.ONE)
                    .setSize(BigDecimal.TEN)
                    .setIsPeggedOrder(false)
                    .setStatus(i % 2 == 0 ? OrderStatus.ACTIVE : OrderStatus.CANCELLED));
        }
        List<DistributionStep> bidDistribution = new ArrayList<>();
        List<DistributionStep> askDistribution = new ArrayList<>();
        for(int i=0; i<bidDistributionSize; i++) {
            bidDistribution.add(new DistributionStep().setPrice(3d).setSize(1d));
        }
        for(int i=0; i<askDistributionSize; i++) {
            askDistribution.add(new DistributionStep().setPrice(4d).setSize(1d));
        }
        Mockito.when(orderStore.getItems()).thenReturn(currentOrders);
        if(exposure.doubleValue() > 0) {
            Mockito.when(pricingUtils.getDistribution(19999d, 0.1d, 0.05d, MarketSide.BUY, 10))
                    .thenReturn(bidDistribution);
        } else {
            Mockito.when(pricingUtils.getDistribution(19999d, 0.2d, 0.05d, MarketSide.BUY, 10))
                    .thenReturn(bidDistribution);
        }
        if(exposure.doubleValue() < 0) {
            Mockito.when(pricingUtils.getDistribution(20001d, 0.1d, 0.05d, MarketSide.SELL, 10))
                    .thenReturn(askDistribution);
        } else {
            Mockito.when(pricingUtils.getDistribution(20001d, 0.2d, 0.05d, MarketSide.SELL, 10))
                    .thenReturn(askDistribution);
        }
        updateQuotesTask.execute(new MarketConfig());
        int modifier = 1;
        if(balance.doubleValue() == 0 || bidDistributionSize == 0 || askDistributionSize == 0) {
            modifier = 0;
        }
        Mockito.verify(vegaApiClient, Mockito.times(modifier))
                .submitBulkInstruction(Mockito.anyList(), Mockito.anyList(),
                        Mockito.any(Market.class), Mockito.anyString());
    }

    @Test
    public void testExecuteDisabled() {
        updateQuotesTask = getTask(false);
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        updateQuotesTask.execute(new MarketConfig());
        Mockito.verify(vegaApiClient, Mockito.times(0)).submitLiquidityCommitment(
                Mockito.any(LiquidityCommitment.class), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testExecute() {
        execute(BigDecimal.ZERO, BigDecimal.valueOf(100000),
                MarketTradingMode.CONTINUOUS, 3, 1);
    }

    @Test
    public void testExecuteMissingBidDistribution() {
        execute(BigDecimal.ZERO, BigDecimal.valueOf(100000),
                MarketTradingMode.CONTINUOUS, 0, 1);
    }

    @Test
    public void testExecuteMissingAskDistribution() {
        execute(BigDecimal.ZERO, BigDecimal.valueOf(100000),
                MarketTradingMode.CONTINUOUS, 3, 0);
    }

    @Test
    public void testExecuteZeroBalance() {
        execute(BigDecimal.ZERO, BigDecimal.ZERO,
                MarketTradingMode.CONTINUOUS, 3, 1);
    }

    @Test
    public void testExecuteLongPosition() {
        execute(BigDecimal.valueOf(1), BigDecimal.valueOf(100000),
                MarketTradingMode.MONITORING_AUCTION, 3, 1);
    }

    @Test
    public void testExecuteShortPosition() {
        execute(BigDecimal.valueOf(-1), BigDecimal.valueOf(100000),
                MarketTradingMode.CONTINUOUS, 3, 1);
    }

    @Test
    public void testExecuteTradingConfigNotFound() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market()
                .setSettlementAsset(USDT)
                .setTradingMode(MarketTradingMode.CONTINUOUS));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        try {
            updateQuotesTask.execute(new MarketConfig());
            Assertions.fail();
        } catch(Exception e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.TRADING_CONFIG_NOT_FOUND);
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
        try {
            updateQuotesTask.execute(new MarketConfig());
            Assertions.fail();
        } catch(Exception e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.REFERENCE_PRICE_NOT_FOUND);
        }
    }

    @Test
    public void testExecuteNotInitialized() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(false);
        updateQuotesTask.execute(new MarketConfig());
        Mockito.verify(vegaApiClient, Mockito.times(0))
                .submitOrder(Mockito.any(Order.class), Mockito.anyString());
    }
}