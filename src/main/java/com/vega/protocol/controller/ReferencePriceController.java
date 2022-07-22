package com.vega.protocol.controller;

import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.store.ReferencePriceStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reference-price")
public class ReferencePriceController {

    private final ReferencePriceStore referencePriceStore;

    public ReferencePriceController(ReferencePriceStore referencePriceStore) {
        this.referencePriceStore = referencePriceStore;
    }

    @GetMapping
    public ResponseEntity<ReferencePrice> get() {
        return ResponseEntity.of(referencePriceStore.get());
    }
}