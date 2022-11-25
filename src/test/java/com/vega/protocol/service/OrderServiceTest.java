package com.vega.protocol.service;

import com.vega.protocol.store.VegaStore;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vega.Vega;

@Slf4j
public class OrderServiceTest {

    private OrderService orderService;
    private VegaStore store;

    @BeforeEach
    public void setup() {
        store = Mockito.mock(VegaStore.class);
        orderService = new OrderService(store);
    }

    @Test
    public void testGetOtherSide() {
        Assertions.assertEquals(Vega.Side.SIDE_SELL, orderService.getOtherSide(Vega.Side.SIDE_BUY));
        Assertions.assertEquals(Vega.Side.SIDE_BUY, orderService.getOtherSide(Vega.Side.SIDE_SELL));
    }
}