package com.vega.protocol.service;

import com.vega.protocol.constant.MarketSide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OrderServiceTest {

    private OrderService orderService;

    @BeforeEach
    public void setup() {
        orderService = new OrderService();
    }

    @Test
    public void testGetOtherSide() {
        Assertions.assertEquals(MarketSide.SELL, orderService.getOtherSide(MarketSide.BUY));
        Assertions.assertEquals(MarketSide.BUY, orderService.getOtherSide(MarketSide.SELL));
    }
}