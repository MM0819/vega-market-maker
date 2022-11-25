package com.vega.protocol.service;

import com.vega.protocol.constant.ErrorCode;
import com.vega.protocol.exception.TradingException;
import com.vega.protocol.store.VegaStore;
import org.springframework.stereotype.Service;
import vega.Assets;

@Service
public class AssetService {
    private final VegaStore vegaStore;

    public AssetService(VegaStore vegaStore) {
        this.vegaStore = vegaStore;
    }

    /**
     * Get asset by ID
     *
     * @return {@link Assets.Asset}
     */
    public Assets.Asset getById(
            final String id
    ) {
        return vegaStore.getAssetById(id)
                .orElseThrow(() -> new TradingException(ErrorCode.ASSET_NOT_FOUND));
    }
}
