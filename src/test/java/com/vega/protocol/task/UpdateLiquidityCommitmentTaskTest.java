package com.vega.protocol.task;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.grpc.client.VegaGrpcClient;
import com.vega.protocol.helper.TestingHelper;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.service.*;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.DecimalUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vega.Markets;

import java.util.Optional;

public class UpdateLiquidityCommitmentTaskTest {

    private static final String MARKET_ID = "1";
    private static final String PARTY_ID = "1";
    private static final String USDT = "USDT";

    private UpdateLiquidityCommitmentTask updateLiquidityCommitmentTask;
    private final ReferencePriceStore referencePriceStore = Mockito.mock(ReferencePriceStore.class);
    private final MarketService marketService = Mockito.mock(MarketService.class);
    private final AccountService accountService = Mockito.mock(AccountService.class);
    private final PositionService positionService = Mockito.mock(PositionService.class);
    private final LiquidityProvisionService liquidityProvisionService = Mockito.mock(LiquidityProvisionService.class);
    private final AssetService assetService = Mockito.mock(AssetService.class);
    private final VegaGrpcClient vegaGrpcClient = Mockito.mock(VegaGrpcClient.class);
    private final DataInitializer dataInitializer = Mockito.mock(DataInitializer.class);
    private final WebSocketInitializer webSocketInitializer = Mockito.mock(WebSocketInitializer.class);
    private final TradingConfigRepository tradingConfigRepository = Mockito.mock(TradingConfigRepository.class);
    private final DecimalUtils decimalUtils = Mockito.mock(DecimalUtils.class);

    private MarketConfig getMarketConfig() {
        return new MarketConfig()
                .setMarketId(MARKET_ID)
                .setPartyId(PARTY_ID);
    }

    private TradingConfig getTradingConfig() {
        return new TradingConfig()
                .setCommitmentBalanceRatio(0.1)
                .setStakeBuffer(0.2)
                .setCommitmentOrderCount(10)
                .setCommitmentSpread(0.03)
                .setFee(0.001);
    }

    private UpdateLiquidityCommitmentTask getTask() {
        return new UpdateLiquidityCommitmentTask(marketService, accountService, positionService, assetService,
                liquidityProvisionService, vegaGrpcClient, referencePriceStore, dataInitializer, webSocketInitializer,
                decimalUtils, tradingConfigRepository);
    }

    @BeforeEach
    public void setup() {
        updateLiquidityCommitmentTask = getTask();
    }

    @Test
    public void testExecute() {
        var market = TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, USDT);
        var marketData = TestingHelper.getMarketData("1", "1");
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(market);
        Mockito.when(marketService.getDataById(MARKET_ID)).thenReturn(marketData);
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(100000.0);
        Mockito.when(positionService.getExposure(MARKET_ID, PARTY_ID)).thenReturn(0.0);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setBidPrice(19999).setAskPrice(20001).setMidPrice(20000)));
        Mockito.when(liquidityProvisionService.getByMarketIdAndPartyId(MARKET_ID, PARTY_ID))
                .thenReturn(Optional.empty());
        MarketConfig marketConfig = getMarketConfig();
        TradingConfig tradingConfig = getTradingConfig();
        Mockito.when(tradingConfigRepository.findByMarketConfig(marketConfig)).thenReturn(Optional.of(tradingConfig));
        updateLiquidityCommitmentTask.execute(marketConfig);
        Mockito.verify(vegaGrpcClient, Mockito.times(1)).submitLiquidityProvision(
                Mockito.anyList(), Mockito.anyList(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString()); // TODO - fix assertion
    }

    @Test
    public void testExecuteZeroBalance() {
        var market = TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, USDT);
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(market);
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(0.0);
        Mockito.when(positionService.getExposure(MARKET_ID, PARTY_ID)).thenReturn(0.0);
        updateLiquidityCommitmentTask.execute(new MarketConfig());
        Mockito.verify(vegaGrpcClient, Mockito.times(0)).submitLiquidityProvision(
                Mockito.anyList(), Mockito.anyList(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString()); // TODO - fix assertion
    }

    @Test
    public void testExecuteLongPosition() {
        var market = TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, USDT);
        var marketData = TestingHelper.getMarketData("9000", "1");
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(market);
        Mockito.when(marketService.getDataById(MARKET_ID)).thenReturn(marketData);
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(100000.0);
        Mockito.when(positionService.getExposure(MARKET_ID, PARTY_ID)).thenReturn(1.0);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setBidPrice(19999)
                        .setAskPrice(20001).setMidPrice(20000)));
        Mockito.when(liquidityProvisionService.getByMarketIdAndPartyId(MARKET_ID, PARTY_ID))
                .thenReturn(Optional.empty());
        MarketConfig marketConfig = getMarketConfig();
        TradingConfig tradingConfig = getTradingConfig();
        Mockito.when(tradingConfigRepository.findByMarketConfig(marketConfig)).thenReturn(Optional.of(tradingConfig));
        updateLiquidityCommitmentTask.execute(marketConfig);
        Mockito.verify(vegaGrpcClient, Mockito.times(1)).submitLiquidityProvision(
                Mockito.anyList(), Mockito.anyList(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString()); // TODO - fix assertion
    }

    @Test
    public void testExecuteShortPosition() {
        var market = TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, USDT);
        var marketData = TestingHelper.getMarketData("1000000000", "1");
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(market);
        Mockito.when(marketService.getDataById(MARKET_ID)).thenReturn(marketData);
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(100000.0);
        Mockito.when(positionService.getExposure(MARKET_ID, PARTY_ID)).thenReturn(-1.0);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setBidPrice(19999).setAskPrice(20001).setMidPrice(20000)));
        Mockito.when(liquidityProvisionService.getByMarketIdAndPartyId(MARKET_ID, PARTY_ID)).thenReturn(
                Optional.of(TestingHelper.getLiquidityProvision("1", market)));
        MarketConfig marketConfig = getMarketConfig();
        TradingConfig tradingConfig = getTradingConfig();
        Mockito.when(tradingConfigRepository.findByMarketConfig(marketConfig)).thenReturn(Optional.of(tradingConfig));
        updateLiquidityCommitmentTask.execute(marketConfig);
        Mockito.verify(vegaGrpcClient, Mockito.times(1)).submitLiquidityProvision(
                Mockito.anyList(), Mockito.anyList(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString()); // TODO - fix assertion
    }

    @Test
    public void testExecuteTradingConfigNotFound() {
        var market = TestingHelper.getMarket(Markets.Market.State.STATE_ACTIVE,
                Markets.Market.TradingMode.TRADING_MODE_CONTINUOUS, USDT);
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isPolygonWebSocketInitialized()).thenReturn(true);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setBidPrice(19999)
                        .setAskPrice(20001).setMidPrice(20000)));
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(market);
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(100000.0);
        Mockito.when(positionService.getExposure(MARKET_ID, PARTY_ID)).thenReturn(0.0);
        MarketConfig marketConfig = getMarketConfig();
        Mockito.when(tradingConfigRepository.findByMarketConfig(marketConfig)).thenReturn(Optional.empty());
        try {
            updateLiquidityCommitmentTask.execute(marketConfig);
            Assertions.fail();
        } catch(Exception e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.TRADING_CONFIG_NOT_FOUND);
        }
    }

    @Test
    public void testExecuteNotInitialized() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(false);
        updateLiquidityCommitmentTask.execute(new MarketConfig());
        Mockito.verify(vegaGrpcClient, Mockito.times(0))
                .submitLiquidityProvision(Mockito.anyList(), Mockito.anyList(),
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }
}
