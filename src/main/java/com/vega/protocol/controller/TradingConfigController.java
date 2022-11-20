package com.vega.protocol.controller;

import com.vega.protocol.entity.TradingConfig;
import com.vega.protocol.request.CreateTradingConfigRequest;
import com.vega.protocol.request.UpdateTradingConfigRequest;
import com.vega.protocol.service.TradingConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trading-config")
public class TradingConfigController {

    private final TradingConfigService tradingConfigService;

    public TradingConfigController(TradingConfigService tradingConfigService) {
        this.tradingConfigService = tradingConfigService;
    }

    @GetMapping
    public ResponseEntity<List<TradingConfig>> get() {
        return ResponseEntity.ok(tradingConfigService.get());
    }

    @PostMapping
    public ResponseEntity<TradingConfig> create(
            @RequestBody CreateTradingConfigRequest request
    ) {
        return ResponseEntity.ok(tradingConfigService.create(request));
    }

    @PutMapping
    public ResponseEntity<TradingConfig> update(
            @RequestBody UpdateTradingConfigRequest request
    ) {
        return ResponseEntity.ok(tradingConfigService.update(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> update(
            @PathVariable(value = "id") String id
    ) {
        return ResponseEntity.ok(tradingConfigService.delete(id));
    }
}