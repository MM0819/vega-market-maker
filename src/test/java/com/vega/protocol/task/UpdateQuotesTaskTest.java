package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.PositionService;
import com.vega.protocol.store.AppConfigStore;
import com.vega.protocol.store.OrderStore;
import com.vega.protocol.store.ReferencePriceStore;
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
    private static final String USDT = "USDT";

    private UpdateQuotesTask updateQuotesTask;
    private final ReferencePriceStore referencePriceStore = Mockito.mock(ReferencePriceStore.class);
    private final AppConfigStore appConfigStore = Mockito.mock(AppConfigStore.class);
    private final OrderStore orderStore = Mockito.mock(OrderStore.class);
    private final VegaApiClient vegaApiClient = Mockito.mock(VegaApiClient.class);
    private final MarketService marketService = Mockito.mock(MarketService.class);
    private final AccountService accountService = Mockito.mock(AccountService.class);
    private final PositionService positionService = Mockito.mock(PositionService.class);

    @BeforeEach
    public void setup() {
        updateQuotesTask = new UpdateQuotesTask(MARKET_ID, referencePriceStore, appConfigStore, orderStore,
                vegaApiClient, marketService, accountService, positionService);
    }

    private void execute(int orderCount, int createdOrders) {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        Mockito.when(positionService.getExposure(MARKET_ID)).thenReturn(BigDecimal.ZERO);
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig().setOrderCount(orderCount)));
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
        updateQuotesTask.execute();
        Mockito.verify(vegaApiClient, Mockito.times(createdOrders)).submitOrder(Mockito.any(Order.class));
        for(Order order : currentOrders.stream().filter(o -> o.getSide().equals(MarketSide.BUY)).toList()) {
            Mockito.verify(vegaApiClient, Mockito.times(1)).cancelOrder(order.getId());
        }
        for(Order order : currentOrders.stream().filter(o -> o.getSide().equals(MarketSide.SELL)).toList()) {
            Mockito.verify(vegaApiClient, Mockito.times(1)).cancelOrder(order.getId());
        }
    }

    @Test
    public void testExecute() {
        execute(10, 20);
    }

    @Test
    public void testExecuteWithOneHundredOrders() {
        execute(100, 103);
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
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig()));
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