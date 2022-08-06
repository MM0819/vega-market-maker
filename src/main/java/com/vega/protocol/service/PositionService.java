package com.vega.protocol.service;

import com.vega.protocol.model.Position;
import com.vega.protocol.store.PositionStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PositionService {

    private final PositionStore positionStore;

    public PositionService(PositionStore positionStore) {
        this.positionStore = positionStore;
    }

    /**
     * Get the current exposure (negative for short positions)
     *
     * @return exposure for given market ID
     */
    public BigDecimal getExposure(
            final String marketId
    ) {
        Optional<Position> positionOptional = positionStore.getItems().stream()
                .filter(p -> p.getMarketId().equals(marketId)).findFirst();
        return positionOptional.orElse(new Position().setSize(BigDecimal.ZERO)).getSize();
    }
}