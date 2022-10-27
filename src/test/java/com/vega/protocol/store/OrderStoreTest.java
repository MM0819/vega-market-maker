package com.vega.protocol.store;

import com.vega.protocol.model.Order;
import com.vega.protocol.store.vega.OrderStore;

public class OrderStoreTest extends MultipleItemStoreTest<Order, OrderStore> {

    private final OrderStore store = new OrderStore();

    @Override
    public OrderStore getStore() {
        return store;
    }

    @Override
    public Order getItem() {
        return new Order();
    }
}
