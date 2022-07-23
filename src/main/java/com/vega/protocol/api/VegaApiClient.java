package com.vega.protocol.api;

import com.vega.protocol.model.LiquidityProvision;
import com.vega.protocol.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VegaApiClient {

    /**
     * Cancel an order
     *
     * @param id the order ID
     */
    public void cancelOrder(
            final String id
    ) {

    }

    /**
     * Submit a new order
     *
     * @param order {@link Order}
     */
    public void submitOrder(
            final Order order
    ) {

    }

    /**
     * Submit a new liquidity commitment
     *
     * @param liquidityProvision {@link LiquidityProvision}
     */
    public void submitLiquidityProvision(
            LiquidityProvision liquidityProvision
    ) {

    }

    /**
     * Amend existing liquidity commitment
     *
     * @param liquidityProvision {@link LiquidityProvision}
     */
    public void amendLiquidityProvision(
            LiquidityProvision liquidityProvision
    ) {

    }
}