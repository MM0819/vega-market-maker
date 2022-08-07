package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.model.*;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.AppConfigStore;
import com.vega.protocol.store.LiquidityCommitmentStore;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.PricingUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class UpdateLiquidityCommitmentTaskTest {

    private static final String MARKET_ID = "1";
    private static final String PARTY_ID = "1";
    private static final String USDT = "USDT";

    private UpdateLiquidityCommitmentTask updateLiquidityCommitmentTask;
    private final ReferencePriceStore referencePriceStore = Mockito.mock(ReferencePriceStore.class);
    private final AppConfigStore appConfigStore = Mockito.mock(AppConfigStore.class);
    private final MarketService marketService = Mockito.mock(MarketService.class);
    private final AccountService accountService = Mockito.mock(AccountService.class);
    private final PositionService positionService = Mockito.mock(PositionService.class);
    private final LiquidityCommitmentStore liquidityCommitmentStore = Mockito.mock(LiquidityCommitmentStore.class);
    private final VegaApiClient vegaApiClient = Mockito.mock(VegaApiClient.class);
    private final PricingUtils pricingUtils = Mockito.mock(PricingUtils.class);

    private AppConfig getAppConfig() {
        return new AppConfig()
                .setFee(0.001)
                .setSpread(0.005)
                .setOrderCount(10)
                .setBidSizeFactor(1.0)
                .setBidQuoteRange(0.05)
                .setAskSizeFactor(1.0)
                .setAskQuoteRange(0.05)
                .setPricingStepSize(0.1);
    }

    @BeforeEach
    public void setup() {
        updateLiquidityCommitmentTask = new UpdateLiquidityCommitmentTask(MARKET_ID, PARTY_ID, marketService,
                accountService, positionService, appConfigStore, vegaApiClient,
                referencePriceStore, liquidityCommitmentStore, pricingUtils);
    }

    @Test
    public void testExecute() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(getAppConfig()));
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setMidPrice(BigDecimal.valueOf(20000))));
        Mockito.when(liquidityCommitmentStore.get()).thenReturn(Optional.empty());
        Mockito.when(pricingUtils.getScalingFactor(Mockito.anyDouble())).thenReturn(1d);
        Mockito.when(pricingUtils.getBidDistribution(
                Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyInt()))
                .thenReturn(List.of(new DistributionStep().setPrice(1d).setSize(1d)));
        Mockito.when(pricingUtils.getAskDistribution(
                Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyInt()))
                .thenReturn(List.of(new DistributionStep().setPrice(1d).setSize(1d)));
        updateLiquidityCommitmentTask.execute();
        Mockito.verify(vegaApiClient, Mockito.times(1))
                .submitLiquidityCommitment(Mockito.any(LiquidityCommitment.class), Mockito.anyString()); // TODO - fix assertion
    }

    @Test
    public void testExecuteZeroBalance() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.ZERO);
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        updateLiquidityCommitmentTask.execute();
        Mockito.verify(vegaApiClient, Mockito.times(0))
                .submitLiquidityCommitment(Mockito.any(LiquidityCommitment.class), Mockito.anyString());
    }

    @Test
    public void testExecuteLongPosition() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.valueOf(2000));
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(getAppConfig()));
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setMidPrice(BigDecimal.valueOf(20000))));
        Mockito.when(liquidityCommitmentStore.get()).thenReturn(Optional.empty());
        updateLiquidityCommitmentTask.execute();
        Mockito.verify(vegaApiClient, Mockito.times(1))
                .submitLiquidityCommitment(Mockito.any(LiquidityCommitment.class), Mockito.anyString()); // TODO - fix assertion
    }

    @Test
    public void testExecuteShortPosition() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.valueOf(-2000));
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(getAppConfig()));
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setMidPrice(BigDecimal.valueOf(20000))));
        Mockito.when(liquidityCommitmentStore.get()).thenReturn(Optional.of(new LiquidityCommitment()));
        updateLiquidityCommitmentTask.execute();
        Mockito.verify(vegaApiClient, Mockito.times(1))
                .amendLiquidityCommitment(Mockito.any(LiquidityCommitment.class), Mockito.anyString()); // TODO - fix assertion
    }

    @Test
    public void testExecuteAppConfigNotFound() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        try {
            updateLiquidityCommitmentTask.execute();
            Assertions.fail();
        } catch(Exception e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.APP_CONFIG_NOT_FOUND);
        }
    }

    @Test
    public void testExecuteReferencePriceNotFound() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(getAppConfig()));
        try {
            updateLiquidityCommitmentTask.execute();
            Assertions.fail();
        } catch(Exception e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.REFERENCE_PRICE_NOT_FOUND);
        }
    }

    @Test
    public void testGetCronExpression() {
        Assertions.assertEquals("0 * * * * *", updateLiquidityCommitmentTask.getCronExpression());
    }
}
