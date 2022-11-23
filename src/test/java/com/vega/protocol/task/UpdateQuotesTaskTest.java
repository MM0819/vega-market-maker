package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.*;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.*;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.NetworkParameterService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.LiquidityCommitmentStore;
import com.vega.protocol.store.OrderStore;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.PricingUtils;
import com.vega.protocol.utils.QuantUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UpdateQuotesTaskTest {

    private static final String TAU_SCALING_PARAM = "market.liquidity.probabilityOfTrading.tau.scaling";
    private static final String MAX_BATCH_SIZE_PARAM = "spam.protection.max.batchSize";
    private static final String MAKER_FEE_PARAM = "market.fee.factors.makerFee";
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
    private final NetworkParameterService networkParameterService = Mockito.mock(NetworkParameterService.class);
    private final TradingConfigRepository tradingConfigRepository = Mockito.mock(TradingConfigRepository.class);

    private UpdateQuotesTask getTask(
            final boolean enabled
    ) {
        return new UpdateQuotesTask(referencePriceStore, orderStore,
                liquidityCommitmentStore, networkParameterService, vegaApiClient, marketService, accountService,
                positionService, pricingUtils, quantUtils, dataInitializer, webSocketInitializer,
                tradingConfigRepository);
    }

    @BeforeEach
    public void setup() {
        updateQuotesTask = getTask(true);
    }

    private void execute(
            final double exposure,
            final double balance,
            final MarketTradingMode tradingMode,
            final int bidDistributionSize,
            final int askDistributionSize
    ) {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market()
                .setSettlementAsset(USDT)
                .setTradingMode(tradingMode)
                .setState(MarketState.ACTIVE));
        MarketConfig marketConfig = new MarketConfig()
                .setMarketId(MARKET_ID)
                .setPartyId(PARTY_ID)
                .setTargetEdge(0.001)
                .setHedgeFee(0.0005);
        TradingConfig tradingConfig = new TradingConfig()
                .setMarketConfig(marketConfig)
                .setCommitmentBalanceRatio(0.1)
                .setStakeBuffer(0.2)
                .setCommitmentOrderCount(10)
                .setCommitmentSpread(0.03)
                .setFee(0.001)
                .setBidQuoteRange(0.05)
                .setAskQuoteRange(0.05)
                .setQuoteOrderCount(10)
                .setBidSizeFactor(1.0)
                .setAskSizeFactor(1.0);
        Mockito.when(tradingConfigRepository.findByMarketConfig(marketConfig)).thenReturn(Optional.of(tradingConfig));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(balance);
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(exposure);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(new ReferencePrice()
                .setAskPrice(20001)
                .setBidPrice(19999)
                .setMidPrice(20000)));
        Mockito.when(networkParameterService.getAsDouble(MAKER_FEE_PARAM)).thenReturn(0.0002);
        Mockito.when(networkParameterService.getAsInt(MAX_BATCH_SIZE_PARAM)).thenReturn(100);
        List<Order> currentOrders = new ArrayList<>();
        for(int i=0; i<4; i++) {
            currentOrders.add(new Order()
                    .setSide(MarketSide.SELL)
                    .setId(String.valueOf(i+1))
                    .setPrice(1)
                    .setSize(10)
                    .setPeggedOrder(false)
                    .setStatus(i % 2 == 0 ? OrderStatus.ACTIVE : OrderStatus.CANCELLED));
        }
        for(int i=0; i<4; i++) {
            currentOrders.add(new Order()
                    .setSide(MarketSide.BUY)
                    .setId(String.valueOf(i+4))
                    .setPrice(1)
                    .setSize(10)
                    .setPeggedOrder(false)
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
        if(exposure > 0) {
            Mockito.when(pricingUtils.getDistribution(19999d, 0.15d, tradingConfig.getBidQuoteRange(),
                            MarketSide.BUY, tradingConfig.getQuoteOrderCount()))
                    .thenReturn(bidDistribution);
        } else {
            Mockito.when(pricingUtils.getDistribution(19999d, 0.25d, tradingConfig.getBidQuoteRange(),
                            MarketSide.BUY, tradingConfig.getQuoteOrderCount()))
                    .thenReturn(bidDistribution);
        }
        if(exposure < 0) {
            Mockito.when(pricingUtils.getDistribution(20001d, 0.15d, tradingConfig.getAskQuoteRange(),
                            MarketSide.SELL, tradingConfig.getQuoteOrderCount()))
                    .thenReturn(askDistribution);
        } else {
            Mockito.when(pricingUtils.getDistribution(20001d, 0.25d, tradingConfig.getAskQuoteRange(),
                            MarketSide.SELL, tradingConfig.getQuoteOrderCount()))
                    .thenReturn(askDistribution);
        }
        updateQuotesTask.execute(marketConfig);
        int modifier = 1;
        if(balance == 0 || bidDistributionSize == 0 || askDistributionSize == 0) {
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
        execute(0, 100000, MarketTradingMode.CONTINUOUS, 3, 1);
    }

    @Test
    public void testExecuteMissingBidDistribution() {
        try {
            execute(0, 100000, MarketTradingMode.CONTINUOUS, 0, 1);
            Assertions.fail();
        } catch(TradingException e) {
            Assertions.assertEquals(ErrorCode.EMPTY_DISTRIBUTION, e.getMessage());
        }
    }

    @Test
    public void testExecuteMissingAskDistribution() {
        try {
            execute(0, 100000, MarketTradingMode.CONTINUOUS, 3, 0);
            Assertions.fail();
        } catch(TradingException e) {
            Assertions.assertEquals(ErrorCode.EMPTY_DISTRIBUTION, e.getMessage());
        }
    }

    @Test
    public void testExecuteZeroBalance() {
        execute(0, 0, MarketTradingMode.CONTINUOUS, 3, 1);
    }

    @Test
    public void testExecuteLongPosition() {
        execute(1, 100000, MarketTradingMode.MONITORING_AUCTION, 3, 1);
    }

    @Test
    public void testExecuteShortPosition() {
        execute(-1, 100000, MarketTradingMode.CONTINUOUS, 3, 1);
    }

    @Test
    public void testExecuteTradingConfigNotFound() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(new ReferencePrice()));
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market()
                .setSettlementAsset(USDT)
                .setTradingMode(MarketTradingMode.CONTINUOUS)
                .setState(MarketState.ACTIVE));
        MarketConfig marketConfig = new MarketConfig().setMarketId(MARKET_ID);
        Mockito.when(tradingConfigRepository.findByMarketConfig(marketConfig)).thenReturn(Optional.empty());
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(100000.0);
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(0.0);
        try {
            updateQuotesTask.execute(marketConfig);
            Assertions.fail();
        } catch(Exception e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.TRADING_CONFIG_NOT_FOUND);
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