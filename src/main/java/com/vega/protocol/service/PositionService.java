package com.vega.protocol.service;

import com.vega.protocol.store.VegaStore;
import com.vega.protocol.utils.DecimalUtils;
import org.springframework.stereotype.Service;
import vega.Markets;
import vega.Vega;

import java.math.BigDecimal;

@Service
public class PositionService {

    private final VegaStore vegaStore;
    private final DecimalUtils decimalUtils;

    public PositionService(VegaStore vegaStore,
                           DecimalUtils decimalUtils) {
        this.vegaStore = vegaStore;
        this.decimalUtils = decimalUtils;
    }

    /**
     * Get the current exposure (negative for short positions)
     *
     * @return exposure for given market ID
     */
    public double getExposure(
            final String marketId,
            final String partyId
    ) {
        var positionOptional = vegaStore.getPositionByMarketIdAndPartyId(marketId, partyId);
        var marketOptional = vegaStore.getMarketById(marketId);
        if(positionOptional.isPresent() && marketOptional.isPresent()) {
            Vega.Position position = positionOptional.get();
            Markets.Market market = marketOptional.get();
            double exposure = decimalUtils.convertToDecimals(market.getPositionDecimalPlaces(),
                    new BigDecimal(position.getOpenVolume()));
            if(exposure == 0) return 0.0;
            return exposure;
        }
        return 0.0;
    }
}