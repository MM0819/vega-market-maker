package com.vega.protocol.controller;

import com.vega.protocol.store.VegaStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vega.Vega;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final VegaStore vegaStore;

    public OrderController(VegaStore vegaStore) {
        this.vegaStore = vegaStore;
    }

    @GetMapping
    public ResponseEntity<List<Vega.Order>> get() {
        return ResponseEntity.ok(vegaStore.getOrders());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Vega.Order>> getByStatus(
            @PathVariable("status") Vega.Order.Status status
    ) {
        return ResponseEntity.ok(vegaStore.getOrders().stream()
                .filter(o -> o.getStatus().equals(status))
                .collect(Collectors.toList()));
    }
}