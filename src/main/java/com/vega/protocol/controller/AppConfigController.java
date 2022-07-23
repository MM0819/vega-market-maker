package com.vega.protocol.controller;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.store.AppConfigStore;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
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
        if(ObjectUtils.isEmpty(config.getAskSizeFactor())) {
            throw new TradingException(ErrorCode.ASK_SIZE_FACTOR_MANDATORY);
        }
        if(ObjectUtils.isEmpty(config.getBidSizeFactor())) {
            throw new TradingException(ErrorCode.BID_SIZE_FACTOR_MANDATORY);
        }
        if(ObjectUtils.isEmpty(config.getAskQuoteRange())) {
            throw new TradingException(ErrorCode.ASK_QUOTE_RANGE_MANDATORY);
        }
        if(ObjectUtils.isEmpty(config.getBidQuoteRange())) {
            throw new TradingException(ErrorCode.BID_QUOTE_RANGE_MANDATORY);
        }
        if(ObjectUtils.isEmpty(config.getFee())) {
            throw new TradingException(ErrorCode.FEE_MANDATORY);
        }
        if(ObjectUtils.isEmpty(config.getSpread())) {
            throw new TradingException(ErrorCode.SPREAD_MANDATORY);
        }
        if(ObjectUtils.isEmpty(config.getOrderCount())) {
            throw new TradingException(ErrorCode.ORDER_COUNT_MANDATORY);
        }
        appConfigStore.update(config);
        return ResponseEntity.of(Optional.of(config));
    }
}