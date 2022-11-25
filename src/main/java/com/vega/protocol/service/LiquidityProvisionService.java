package com.vega.protocol.service;

import com.vega.protocol.store.VegaStore;
import org.springframework.stereotype.Service;
import vega.Vega;

import java.util.Optional;

@Service
public class LiquidityProvisionService {
    private final VegaStore vegaStore;

    public LiquidityProvisionService(VegaStore vegaStore) {
        this.vegaStore = vegaStore;
    }

    /**
     * Get liquidity provision by market ID and party ID
     *
     * @return {@link Vega.LiquidityProvision}
     */
    public Optional<Vega.LiquidityProvision> getByMarketIdAndPartyId(
            final String marketId,
            final String partyId
    ) {
        return vegaStore.getLiquidityProvisionByMarketIdAndPartyId(marketId, partyId);
    }
}
