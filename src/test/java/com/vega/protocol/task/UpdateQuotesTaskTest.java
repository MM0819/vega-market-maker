package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
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
        updateQuotesTask = new UpdateQuotesTask(MARKET_ID, PARTY_ID, referencePriceStore, appConfigStore, orderStore,
                vegaApiClient, marketService, accountService, positionService, pricingUtils);
    }

    private void execute(BigDecimal exposure) {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
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
                        Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyInt()))
                .thenReturn(List.of(new DistributionStep().setPrice(1d).setSize(1d)));
        Mockito.when(pricingUtils.getAskDistribution(
                        Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyInt()))
                .thenReturn(List.of(
                        new DistributionStep().setPrice(1d).setSize(1d),
                        new DistributionStep().setPrice(1d).setSize(1d),
                        new DistributionStep().setPrice(1d).setSize(1d),
                        new DistributionStep().setPrice(1d).setSize(1d)
                ));
        updateQuotesTask.execute();
        Mockito.verify(vegaApiClient, Mockito.times(5)).submitOrder(Mockito.any(Order.class), Mockito.anyString()); // TODO - fix assertion
        for(Order order : currentOrders.stream().filter(o -> o.getSide().equals(MarketSide.BUY)).toList()) {
            Mockito.verify(vegaApiClient, Mockito.times(1)).cancelOrder(order.getId(), PARTY_ID);
        }
        for(Order order : currentOrders.stream().filter(o -> o.getSide().equals(MarketSide.SELL)).toList()) {
            Mockito.verify(vegaApiClient, Mockito.times(1)).cancelOrder(order.getId(), PARTY_ID);
        }
    }

    @Test
    public void testExecute() {
        execute(BigDecimal.ZERO);
    }

    @Test
    public void testExecuteLongPosition() {
        execute(BigDecimal.valueOf(1000L));
    }

    @Test
    public void testExecuteShortPosition() {
        execute(BigDecimal.valueOf(-1000L));
    }

    @Test
    public void testExecuteAppConfigNotFound() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
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
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
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
    public void testGetCronExpression() {
        Assertions.assertEquals("*/15 * * * * *", updateQuotesTask.getCronExpression());
    }
}