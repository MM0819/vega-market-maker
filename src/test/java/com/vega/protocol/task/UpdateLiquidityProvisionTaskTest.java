package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.model.*;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.AppConfigStore;
import com.vega.protocol.store.LiquidityProvisionStore;
import com.vega.protocol.store.ReferencePriceStore;
import com.vega.protocol.utils.PricingUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class UpdateLiquidityProvisionTaskTest {

    private static final String MARKET_ID = "1";
    private static final String USDT = "USDT";

    private UpdateLiquidityProvisionTask updateLiquidityProvisionTask;
    private final ReferencePriceStore referencePriceStore = Mockito.mock(ReferencePriceStore.class);
    private final AppConfigStore appConfigStore = Mockito.mock(AppConfigStore.class);
    private final MarketService marketService = Mockito.mock(MarketService.class);
    private final AccountService accountService = Mockito.mock(AccountService.class);
    private final PositionService positionService = Mockito.mock(PositionService.class);
    private final LiquidityProvisionStore liquidityProvisionStore = Mockito.mock(LiquidityProvisionStore.class);
    private final VegaApiClient vegaApiClient = Mockito.mock(VegaApiClient.class);
    private final PricingUtils pricingUtils = Mockito.mock(PricingUtils.class);

    @BeforeEach
    public void setup() {
        updateLiquidityProvisionTask = new UpdateLiquidityProvisionTask(MARKET_ID, marketService, accountService,
                positionService, appConfigStore, vegaApiClient,
                referencePriceStore, liquidityProvisionStore, pricingUtils);
    }

    @Test
    public void testExecute() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig()));
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setMidPrice(BigDecimal.valueOf(20000))));
        Mockito.when(liquidityProvisionStore.get()).thenReturn(Optional.empty());
        Mockito.when(pricingUtils.getBidScalingFactor(Mockito.anyLong(), Mockito.anyDouble())).thenReturn(1d);
        Mockito.when(pricingUtils.getAskScalingFactor(Mockito.anyLong(), Mockito.anyDouble())).thenReturn(1d);
        Mockito.when(pricingUtils.getBidDistribution(
                Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyInt()))
                .thenReturn(List.of(new DistributionStep().setPrice(1d).setSize(1d)));
        Mockito.when(pricingUtils.getAskDistribution(
                Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyInt()))
                .thenReturn(List.of(new DistributionStep().setPrice(1d).setSize(1d)));
        updateLiquidityProvisionTask.execute();
        Mockito.verify(vegaApiClient, Mockito.times(1))
                .submitLiquidityProvision(Mockito.any(LiquidityProvision.class)); // TODO - fix assertion
    }

    @Test
    public void testExecuteLongPosition() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.valueOf(2000));
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig()));
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setMidPrice(BigDecimal.valueOf(20000))));
        Mockito.when(liquidityProvisionStore.get()).thenReturn(Optional.empty());
        updateLiquidityProvisionTask.execute();
        Mockito.verify(vegaApiClient, Mockito.times(1))
                .submitLiquidityProvision(Mockito.any(LiquidityProvision.class)); // TODO - fix assertion
    }

    @Test
    public void testExecuteShortPosition() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.valueOf(-2000));
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig()));
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setMidPrice(BigDecimal.valueOf(20000))));
        Mockito.when(liquidityProvisionStore.get()).thenReturn(Optional.of(new LiquidityProvision()));
        updateLiquidityProvisionTask.execute();
        Mockito.verify(vegaApiClient, Mockito.times(1))
                .amendLiquidityProvision(Mockito.any(LiquidityProvision.class)); // TODO - fix assertion
    }

    @Test
    public void testExecuteAppConfigNotFound() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        try {
            updateLiquidityProvisionTask.execute();
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
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig()));
        try {
            updateLiquidityProvisionTask.execute();
            Assertions.fail();
        } catch(Exception e) {
            Assertions.assertEquals(e.getMessage(), ErrorCode.REFERENCE_PRICE_NOT_FOUND);
        }
    }

    @Test
    public void testGetCronExpression() {
        Assertions.assertEquals("0 * * * * *", updateLiquidityProvisionTask.getCronExpression());
    }
}
