package com.vega.protocol.controller;

import com.vega.protocol.model.LiquidityCommitment;
import com.vega.protocol.store.vega.LiquidityCommitmentStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/liquidity-commitment")
public class LiquidityCommitmentController {

    private final LiquidityCommitmentStore liquidityCommitmentStore;

    public LiquidityCommitmentController(LiquidityCommitmentStore liquidityCommitmentStore) {
        this.liquidityCommitmentStore = liquidityCommitmentStore;
    }

    @GetMapping
    public ResponseEntity<List<LiquidityCommitment>> get() {
        return ResponseEntity.ok(liquidityCommitmentStore.getItems());
    }
}