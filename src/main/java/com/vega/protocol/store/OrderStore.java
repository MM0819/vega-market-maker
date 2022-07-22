package com.vega.protocol.store;

import com.vega.protocol.model.Order;
import org.springframework.stereotype.Repository;

@Repository
public class OrderStore extends MultipleItemStore<Order> {
}