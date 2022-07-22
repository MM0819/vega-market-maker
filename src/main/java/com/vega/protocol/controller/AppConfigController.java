package com.vega.protocol.controller;

import com.vega.protocol.model.AppConfig;
import com.vega.protocol.store.AppConfigStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app-config")
public class AppConfigController {

    private final AppConfigStore appConfigStore;

    public AppConfigController(AppConfigStore appConfigStore) {
        this.appConfigStore = appConfigStore;
    }

    @GetMapping
    public ResponseEntity<AppConfig> get() {
        return ResponseEntity.of(appConfigStore.get());
    }

    // TODO - add endpoint to edit config (some validation will be needed?)
}