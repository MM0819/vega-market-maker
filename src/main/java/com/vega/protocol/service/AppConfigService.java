package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.AppConfig;
import com.vega.protocol.store.AppConfigStore;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class AppConfigService {

    private final AppConfigStore appConfigStore;

    public AppConfigService(AppConfigStore appConfigStore) {
        this.appConfigStore = appConfigStore;
    }

    /**
     * Get the app config
     *
     * @return {@link AppConfig}
     */
    public AppConfig get() {
        return appConfigStore.get().orElseThrow(() -> new TradingException(ErrorCode.APP_CONFIG_NOT_FOUND));
    }

    /**
     * Update app config
     *
     * @param config {@link AppConfig}
     *
     * @return {@link AppConfig}
     */
    public AppConfig update(
            final AppConfig config
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
        if(ObjectUtils.isEmpty(config.getPricingStepSize())) {
            throw new TradingException(ErrorCode.PRICING_STEP_SIZE_MANDATORY);
        }
        appConfigStore.update(config);
        return config;
    }
}