package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.store.VegaStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vega.Vega;

@Slf4j
@Service
public class NetworkParameterService {

    private final VegaStore vegaStore;

    public NetworkParameterService(VegaStore vegaStore) {
        this.vegaStore = vegaStore;
    }

    public Vega.NetworkParameter getParam(
            final String key
    ) {
        return vegaStore.getNetworkParameterByKey(key)
                .orElseThrow(() -> new TradingException(ErrorCode.NETWORK_PARAMETER_NOT_FOUND));
    }

    public double getAsDouble(
            final String id
    ) {
        return Double.parseDouble(getAsString(id));
    }

    public int getAsInt(
            final String id
    ) {
        return Integer.parseInt(getAsString(id));
    }

    public String getAsString(
            final String id
    ) {
        return getParam(id).getValue();
    }
}
