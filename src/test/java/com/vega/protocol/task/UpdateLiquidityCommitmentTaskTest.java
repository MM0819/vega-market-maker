package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketState;
import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.initializer.DataInitializer;
import com.vega.protocol.initializer.WebSocketInitializer;
import com.vega.protocol.model.LiquidityCommitment;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.repository.TradingConfigRepository;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.LiquidityCommitmentStore;
import com.vega.protocol.store.ReferencePriceStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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
    private final LiquidityCommitmentStore liquidityCommitmentStore = Mockito.mock(LiquidityCommitmentStore.class);
    private final VegaApiClient vegaApiClient = Mockito.mock(VegaApiClient.class);
    private final DataInitializer dataInitializer = Mockito.mock(DataInitializer.class);
    private final WebSocketInitializer webSocketInitializer = Mockito.mock(WebSocketInitializer.class);
    private final TradingConfigRepository tradingConfigRepository = Mockito.mock(TradingConfigRepository.class);

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
        return new UpdateLiquidityCommitmentTask(marketService, accountService, positionService, vegaApiClient,
                referencePriceStore, liquidityCommitmentStore, dataInitializer, webSocketInitializer,
                tradingConfigRepository);
    }

    @BeforeEach
    public void setup() {
        updateLiquidityCommitmentTask = getTask();
    }

    @Test
    public void testExecute() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT)
                .setTargetStake(BigDecimal.ONE).setSuppliedStake(BigDecimal.ONE)
                .setState(MarketState.ACTIVE));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setBidPrice(BigDecimal.valueOf(19999))
                        .setAskPrice(BigDecimal.valueOf(20001)).setMidPrice(BigDecimal.valueOf(20000))));
        Mockito.when(liquidityCommitmentStore.getItems()).thenReturn(Collections.emptyList());
        MarketConfig marketConfig = getMarketConfig();
        TradingConfig tradingConfig = getTradingConfig();
        Mockito.when(tradingConfigRepository.findByMarketConfig(marketConfig)).thenReturn(Optional.of(tradingConfig));
        updateLiquidityCommitmentTask.execute(marketConfig);
        Mockito.verify(vegaApiClient, Mockito.times(1)).submitLiquidityCommitment(
                Mockito.any(LiquidityCommitment.class), Mockito.anyString(), Mockito.anyBoolean()); // TODO - fix assertion
    }

    @Test
    public void testExecuteZeroBalance() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.ZERO);
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        updateLiquidityCommitmentTask.execute(new MarketConfig());
        Mockito.verify(vegaApiClient, Mockito.times(0)).submitLiquidityCommitment(
                Mockito.any(LiquidityCommitment.class), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testExecuteLongPosition() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT)
                .setTargetStake(BigDecimal.valueOf(9000)).setSuppliedStake(BigDecimal.ONE)
                .setState(MarketState.ACTIVE));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.valueOf(1));
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setBidPrice(BigDecimal.valueOf(19999))
                        .setAskPrice(BigDecimal.valueOf(20001)).setMidPrice(BigDecimal.valueOf(20000))));
        Mockito.when(liquidityCommitmentStore.getItems()).thenReturn(Collections.emptyList());
        MarketConfig marketConfig = getMarketConfig();
        TradingConfig tradingConfig = getTradingConfig();
        Mockito.when(tradingConfigRepository.findByMarketConfig(marketConfig)).thenReturn(Optional.of(tradingConfig));
        updateLiquidityCommitmentTask.execute(marketConfig);
        Mockito.verify(vegaApiClient, Mockito.times(1)).submitLiquidityCommitment(
                Mockito.any(LiquidityCommitment.class), Mockito.anyString(), Mockito.anyBoolean()); // TODO - fix assertion
    }

    @Test
    public void testExecuteShortPosition() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT)
                .setTargetStake(BigDecimal.valueOf(1000000000)).setSuppliedStake(BigDecimal.ONE)
                .setState(MarketState.ACTIVE));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.valueOf(-1));
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setBidPrice(BigDecimal.valueOf(19999))
                        .setAskPrice(BigDecimal.valueOf(20001)).setMidPrice(BigDecimal.valueOf(20000))));
        Mockito.when(liquidityCommitmentStore.getItems()).thenReturn(
                List.of(new LiquidityCommitment()
                        .setCommitmentAmount(BigDecimal.ONE)
                        .setMarket(new Market().setId(MARKET_ID))));
        MarketConfig marketConfig = getMarketConfig();
        TradingConfig tradingConfig = getTradingConfig();
        Mockito.when(tradingConfigRepository.findByMarketConfig(marketConfig)).thenReturn(Optional.of(tradingConfig));
        updateLiquidityCommitmentTask.execute(marketConfig);
        Mockito.verify(vegaApiClient, Mockito.times(1)).submitLiquidityCommitment(
                Mockito.any(LiquidityCommitment.class), Mockito.anyString(), Mockito.anyBoolean()); // TODO - fix assertion
    }

    @Test
    public void testExecuteTradingConfigNotFound() {
        Mockito.when(dataInitializer.isInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isVegaWebSocketsInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isBinanceWebSocketInitialized()).thenReturn(true);
        Mockito.when(webSocketInitializer.isPolygonWebSocketInitialized()).thenReturn(true);
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setBidPrice(BigDecimal.valueOf(19999))
                        .setAskPrice(BigDecimal.valueOf(20001)).setMidPrice(BigDecimal.valueOf(20000))));
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT).setState(MarketState.ACTIVE));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
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
        Mockito.verify(vegaApiClient, Mockito.times(0))
                .submitLiquidityCommitment(Mockito.any(LiquidityCommitment.class),
                        Mockito.anyString(), Mockito.anyBoolean());
    }
}
