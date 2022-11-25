package com.vega.protocol.task;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.grpc.client.VegaGrpcClient;
import com.vega.protocol.helper.TestingHelper;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.DistributionStep;
import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.service.*;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.DecimalUtils;
import com.vega.protocol.utils.PricingUtils;
import com.vega.protocol.utils.QuantUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vega.Markets;
import vega.Vega;

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
    private final VegaGrpcClient vegaGrpcClient = Mockito.mock(VegaGrpcClient.class);
    private final MarketService marketService = Mockito.mock(MarketService.class);
    private final AccountService accountService = Mockito.mock(AccountService.class);
    private final PositionService positionService = Mockito.mock(PositionService.class);
    private final PricingUtils pricingUtils = Mockito.mock(PricingUtils.class);
    private final QuantUtils quantUtils = Mockito.mock(QuantUtils.class);
    private final DataInitializer dataInitializer = Mockito.mock(DataInitializer.class);
    private final WebSocketInitializer webSocketInitializer = Mockito.mock(WebSocketInitializer.class);
    private final LiquidityProvisionService liquidityProvisionService = Mockito.mock(LiquidityProvisionService.class);
    private final NetworkParameterService networkParameterService = Mockito.mock(NetworkParameterService.class);
    private final TradingConfigRepository tradingConfigRepository = Mockito.mock(TradingConfigRepository.class);
    private final AssetService assetService = Mockito.mock(AssetService.class);
    private final OrderService orderService = Mockito.mock(OrderService.class);
    private final DecimalUtils decimalUtils = Mockito.mock(DecimalUtils.class);

    private UpdateQuotesTask getTask() {
        return new UpdateQuotesTask(referencePriceStore, networkParameterService, vegaGrpcClient, marketService,
                accountService, positionService, orderService, assetService, pricingUtils, quantUtils, dataInitializer,
                webSocketInitializer, liquidityProvisionService, decimalUtils, tradingConfigRepository);
    }

    @BeforeEach
    public void setup() {
        updateQuotesTask = getTask();
    }

    private void execute(
            final double exposure,
            final double balance,
            final Markets.Market.TradingMode tradingMode,
            final int bidDistributionSize,
            final int askDistributionSize
    ) {
        var market = TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE, tradingMode, USDT);
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(market);
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
        Mockito.when(positionService.getExposure(MARKET_ID, PARTY_ID)).thenReturn(exposure);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(new ReferencePrice()
                .setAskPrice(20001)
                .setBidPrice(19999)
                .setMidPrice(20000)));
        Mockito.when(networkParameterService.getAsDouble(MAKER_FEE_PARAM)).thenReturn(0.0002);
        Mockito.when(networkParameterService.getAsInt(MAX_BATCH_SIZE_PARAM)).thenReturn(100);
        List<Vega.Order> currentOrders = new ArrayList<>();
        for(int i=0; i<4; i++) {
            String id = String.valueOf(i+1);
            Vega.Order.Status status = Vega.Order.Status.STATUS_ACTIVE;
            Vega.Order order = TestingHelper.getOrder(id, "1", 10L,
                    Vega.Side.SIDE_SELL, status, null);
            currentOrders.add(order);
        }
        for(int i=0; i<4; i++) {
            String id = String.valueOf(i+1);
            Vega.Order.Status status = Vega.Order.Status.STATUS_ACTIVE;
            Vega.Order order = TestingHelper.getOrder(id, "1", 10L,
                    Vega.Side.SIDE_BUY, status, null);
            currentOrders.add(order);
        }
        List<DistributionStep> bidDistribution = new ArrayList<>();
        List<DistributionStep> askDistribution = new ArrayList<>();
        for(int i=0; i<bidDistributionSize; i++) {
            bidDistribution.add(new DistributionStep().setPrice(3d).setSize(1d));
        }
        for(int i=0; i<askDistributionSize; i++) {
            askDistribution.add(new DistributionStep().setPrice(4d).setSize(1d));
        }
        Mockito.when(orderService.getByMarketIdAndStatus(MARKET_ID,
                Vega.Order.Status.STATUS_ACTIVE)).thenReturn(currentOrders);
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
        Mockito.verify(vegaGrpcClient, Mockito.times(modifier))
                .batchMarketInstruction(Mockito.anyList(), Mockito.anyList(),
                        Mockito.anyList(), Mockito.anyString());
    }

    @Test
    public void testExecuteDisabled() {
        updateQuotesTask = getTask();
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        updateQuotesTask.execute(new MarketConfig());
        Mockito.verify(vegaGrpcClient, Mockito.times(0)).submitLiquidityProvision(
                Mockito.anyList(), Mockito.anyList(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testExecute() {
        execute(0, 100000,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, 3, 1);
    }

    @Test
    public void testExecuteMissingBidDistribution() {
        try {
            execute(0, 100000,
                    Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, 0, 1);
            Assertions.fail();
        } catch(TradingException e) {
            Assertions.assertEquals(ErrorCode.EMPTY_DISTRIBUTION, e.getMessage());
        }
    }

    @Test
    public void testExecuteMissingAskDistribution() {
        try {
            execute(0, 100000,
                    Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, 3, 0);
            Assertions.fail();
        } catch(TradingException e) {
            Assertions.assertEquals(ErrorCode.EMPTY_DISTRIBUTION, e.getMessage());
        }
    }

    @Test
    public void testExecuteZeroBalance() {
        execute(0, 0,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, 3, 1);
    }

    @Test
    public void testExecuteLongPosition() {
        execute(1, 100000,
                Markets.Market.TradingMode.TRADING_MODE_MONITORING_AUCTION, 3, 1);
    }

    @Test
    public void testExecuteShortPosition() {
        execute(-1, 100000,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, 3, 1);
    }

    @Test
    public void testExecuteTradingConfigNotFound() {
        var market = TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, USDT);
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(new ReferencePrice()));
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(market);
        MarketConfig marketConfig = new MarketConfig().setMarketId(MARKET_ID);
        Mockito.when(tradingConfigRepository.findByMarketConfig(marketConfig)).thenReturn(Optional.empty());
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(100000.0);
        Mockito.when(positionService.getExposure(MARKET_ID, PARTY_ID)).thenReturn(0.0);
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
        Mockito.verify(vegaGrpcClient, Mockito.times(0))
                .submitOrder(Mockito.anyString(), Mockito.anyLong(), Mockito.any(Vega.Side.class),
                        Mockito.any(Vega.Order.TimeInForce.class), Mockito.any(Vega.Order.Type.class),
                        Mockito.anyString(), Mockito.anyString());
    }
}