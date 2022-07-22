package com.vega.protocol.task;

import com.vega.protocol.api.VegaApiClient;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.model.Market;
import com.vega.protocol.model.Order;
import com.vega.protocol.service.AccountService;
import com.vega.protocol.service.MarketService;
import com.vega.protocol.service.OrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

public class NaiveFlowTaskTest {

    private static final String MARKET_ID = "1";
    private static final String PARTY_ID = "1";
    private static final String USDT = "USDT";

    private NaiveFlowTask naiveFlowTask;
    private final VegaApiClient vegaApiClient = Mockito.mock(VegaApiClient.class);
    private final MarketService marketService = Mockito.mock(MarketService.class);
    private final AccountService accountService = Mockito.mock(AccountService.class);
    private final OrderService orderService = Mockito.mock(OrderService.class);

    private NaiveFlowTask getNaiveFlowTask(
            boolean enabled
    ) {
        return new NaiveFlowTask(MARKET_ID, enabled, PARTY_ID,
                vegaApiClient, marketService, accountService, orderService);
    }

    @BeforeEach
    public void setup() {
        naiveFlowTask = getNaiveFlowTask(true);
    }

    @Test
    public void testExecute() {
        Mockito.when(marketService.getById(MARKET_ID)).thenReturn(new Market().setSettlementAsset(USDT));
        Mockito.when(accountService.getTotalBalance(USDT)).thenReturn(BigDecimal.valueOf(100000));
        int count = 20;
        for(int i=0; i<count; i++) {
            Mockito.when(orderService.getOtherSide(Mockito.any()))
                    .thenReturn(i % 2 == 0 ? MarketSide.BUY : MarketSide.SELL);
            naiveFlowTask.execute();
        }
        Mockito.verify(vegaApiClient, Mockito.times(count)).submitOrder(Mockito.any(Order.class));
    }

    @Test
    public void testExecuteDisabled() {
        naiveFlowTask = getNaiveFlowTask(false);
        naiveFlowTask.execute();
        Mockito.verify(vegaApiClient, Mockito.times(0)).submitOrder(Mockito.any(Order.class));
    }

    @Test
    public void testGetCronExpression() {
        Assertions.assertEquals("*/5 * * * * *", naiveFlowTask.getCronExpression());
    }

    @Test
    public void testInitialized() {
        naiveFlowTask.initialize();
    }
}