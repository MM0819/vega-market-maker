package com.vega.protocol.controller;

import com.vega.protocol.store.VegaStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vega.Vega;

import java.util.List;

@RestController
@RequestMapping("/position")
public class PositionController {

    private final VegaStore vegaStore;

    public PositionController(VegaStore vegaStore) {
        this.vegaStore = vegaStore;
    }

    @GetMapping
    public ResponseEntity<List<Vega.Position>> get() {
        return ResponseEntity.ok(vegaStore.getPositions());
    }
}