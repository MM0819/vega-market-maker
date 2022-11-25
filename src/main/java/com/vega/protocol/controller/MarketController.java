package com.vega.protocol.controller;

import com.vega.protocol.store.VegaStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vega.Markets;

import java.util.List;

@RestController
@RequestMapping("/market")
public class MarketController {

    private final VegaStore vegaStore;

    public MarketController(VegaStore vegaStore) {
        this.vegaStore = vegaStore;
    }

    @GetMapping
    public ResponseEntity<List<Markets.Market>> get() {
        return ResponseEntity.ok(vegaStore.getMarkets());
    }
}