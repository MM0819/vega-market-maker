package com.vega.protocol.controller;

import com.vega.protocol.model.AppConfig;
import com.vega.protocol.store.AppConfigStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

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

    @PutMapping
    public ResponseEntity<AppConfig> update(
            @RequestBody AppConfig config
    ) {
        appConfigStore.update(config);
        return ResponseEntity.of(Optional.of(config));
    }
}