package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.model.Order;
import com.vega.protocol.store.AppConfigStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class OrderServiceTest {

    private OrderService orderService;
    private final AppConfigStore appConfigStore = Mockito.mock(AppConfigStore.class);

    @BeforeEach
    public void setup() {
        Mockito.when(appConfigStore.get()).thenReturn(Optional.of(new AppConfig()));
        orderService = new OrderService(appConfigStore);
    }

    @Test
    public void testGetMarketMakerBids() {
        List<Order> orders = orderService.getMarketMakerOrders(
                MarketSide.BUY, BigDecimal.valueOf(10000), BigDecimal.valueOf(20000));
        Assertions.assertEquals(10, orders.size());
        Assertions.assertEquals(19850, orders.get(0).getPrice().doubleValue());
        Assertions.assertEquals(18950, orders.get(9).getPrice().doubleValue());
    }

    @Test
    public void testGetMarketMakerAsks() {
        List<Order> orders = orderService.getMarketMakerOrders(
                MarketSide.SELL, BigDecimal.valueOf(10000), BigDecimal.valueOf(20000));
        Assertions.assertEquals(10, orders.size());
        Assertions.assertEquals(20150, orders.get(0).getPrice().doubleValue());
        Assertions.assertEquals(21050, orders.get(9).getPrice().doubleValue());
    }

    @Test
    public void testGetMarketMakerOrdersEmptyConfig() {
        Mockito.when(appConfigStore.get()).thenReturn(Optional.empty());
        try {
            orderService.getMarketMakerOrders(
                    MarketSide.SELL, BigDecimal.valueOf(10000), BigDecimal.valueOf(20000));
            Assertions.fail();
        } catch(TradingException e) {
            Assertions.assertEquals(ErrorCode.APP_CONFIG_NOT_FOUND, e.getMessage());
        }
    }

    @Test
    public void testGetOtherSide() {
        Assertions.assertEquals(MarketSide.SELL, orderService.getOtherSide(MarketSide.BUY));
        Assertions.assertEquals(MarketSide.BUY, orderService.getOtherSide(MarketSide.SELL));
    }
}