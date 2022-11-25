package com.vega.protocol.controller;

import com.vega.protocol.store.VegaStore;
import datanode.api.v2.TradingData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final VegaStore vegaStore;

    public AccountController(VegaStore vegaStore) {
        this.vegaStore = vegaStore;
    }

    @GetMapping
    public ResponseEntity<List<TradingData.AccountBalance>> get() {
        return ResponseEntity.ok(vegaStore.getAccounts());
    }
}