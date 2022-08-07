package com.vega.protocol.controller;

import com.vega.protocol.model.Order;
import com.vega.protocol.store.OrderStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderStore orderStore;

    public OrderController(OrderStore orderStore) {
        this.orderStore = orderStore;
    }

    @GetMapping
    public ResponseEntity<List<Order>> get() {
        return ResponseEntity.ok(orderStore.getItems());
    }
}