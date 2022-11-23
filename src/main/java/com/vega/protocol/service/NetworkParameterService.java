package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.model.NetworkParameter;
import com.vega.protocol.store.NetworkParameterStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NetworkParameterService {

    private final NetworkParameterStore networkParameterStore;

    public NetworkParameterService(NetworkParameterStore networkParameterStore) {
        this.networkParameterStore = networkParameterStore;
    }

    public NetworkParameter getParam(
            final String id
    ) {
        return networkParameterStore.getById(id)
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
