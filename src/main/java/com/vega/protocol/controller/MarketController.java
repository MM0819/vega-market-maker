package com.vega.protocol.controller;

import com.vega.protocol.model.Market;
import com.vega.protocol.store.vega.MarketStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/market")
public class MarketController {

    private final MarketStore marketStore;

    public MarketController(MarketStore marketStore) {
        this.marketStore = marketStore;
    }

    @GetMapping
    public ResponseEntity<List<Market>> get() {
        return ResponseEntity.ok(marketStore.getItems());
    }
}