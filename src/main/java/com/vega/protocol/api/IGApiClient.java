package com.vega.protocol.api;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.model.Position;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Component
public class IGApiClient implements ExchangeApiClient {

    /**
     * {@inheritDoc}
     */
    @Override
    public void submitMarketOrder(
            final String symbol,
            final BigDecimal size,
            final MarketSide side
    ) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Position> getPosition(
            final String symbol
    ) {
        return Optional.empty();
    }
}