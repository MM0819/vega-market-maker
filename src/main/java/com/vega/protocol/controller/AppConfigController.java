package com.vega.protocol.controller;

import com.vega.protocol.model.AppConfig;
import com.vega.protocol.service.AppConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/app-config")
public class AppConfigController {

    private final AppConfigService appConfigService;

    public AppConfigController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @GetMapping
    public ResponseEntity<AppConfig> get() {
        return ResponseEntity.of(Optional.of(appConfigService.get()));
    }

    @PutMapping
    public ResponseEntity<AppConfig> update(
            @RequestBody AppConfig config
    ) {
        return ResponseEntity.of(Optional.of(appConfigService.update(config)));
    }
}