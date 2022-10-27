package com.vega.protocol.controller;

import com.vega.protocol.constant.OrderStatus;
import com.vega.protocol.model.Order;
import com.vega.protocol.store.vega.OrderStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getByStatus(
            @PathVariable("status") OrderStatus status
    ) {
        return ResponseEntity.ok(orderStore.getItems().stream()
                .filter(o -> o.getStatus().equals(status))
                .collect(Collectors.toList()));
    }
}