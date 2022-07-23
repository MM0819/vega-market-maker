package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.AppConfigStore;
import com.vega.protocol.store.ReferencePriceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
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
    private final VegaApiClient vegaApiClient = Mockito.mock(VegaApiClient.class);

    @BeforeEach
    public void setup() {
        updateLiquidityProvisionTask = new UpdateLiquidityProvisionTask(MARKET_ID, marketService, accountService,
                positionService, appConfigStore, vegaApiClient, referencePriceStore);
    }

    @Test
    public void testExecute() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig()));
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setMidPrice(BigDecimal.valueOf(20000))));
        updateLiquidityProvisionTask.execute();
        // TODO - assertions
    }

    @Test
    public void testExecuteLongPosition() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.valueOf(2000));
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig()));
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setMidPrice(BigDecimal.valueOf(20000))));
        updateLiquidityProvisionTask.execute();
        // TODO - assertions
    }

    @Test
    public void testExecuteShortPosition() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.valueOf(-2000));
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig()));
        Mockito.when(referencePriceStore.get()).thenReturn(Optional.of(
                new ReferencePrice().setMidPrice(BigDecimal.valueOf(20000))));
        updateLiquidityProvisionTask.execute();
        // TODO - assertions
    }
}
