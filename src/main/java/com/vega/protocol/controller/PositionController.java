package com.vega.protocol.controller;

import com.vega.protocol.model.Position;
import com.vega.protocol.store.vega.PositionStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/position")
public class PositionController {

    private final PositionStore positionStore;

    public PositionController(PositionStore positionStore) {
        this.positionStore = positionStore;
    }

    @GetMapping
    public ResponseEntity<List<Position>> get() {
        return ResponseEntity.ok(positionStore.getItems());
    }
}