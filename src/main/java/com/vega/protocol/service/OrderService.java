package com.vega.protocol.service;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.PeggedReference;
import com.vega.protocol.model.LiquidityCommitmentOffset;
import com.vega.protocol.utils.DecimalUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final DecimalUtils decimalUtils;

    public OrderService(DecimalUtils decimalUtils) {
        this.decimalUtils = decimalUtils;
    }

    /**
     * Get the opposite {@link MarketSide}
     *
     * @param side {@link MarketSide}
     *
     * @return opposite {@link MarketSide}
     */
    public MarketSide getOtherSide(
            MarketSide side
    ) {
        return side.equals(MarketSide.BUY) ? MarketSide.SELL : MarketSide.BUY;
    }

    /**
     * Parse liquidity orders JSON
     *
     * @param ordersArray {@link JSONArray}
     * @param decimalPlaces market decimal places
     *
     * @return {@link List < LiquidityCommitmentOffset >}
     */
    public List<LiquidityCommitmentOffset> parseLiquidityOrders(
            final JSONArray ordersArray,
            final int decimalPlaces,
            final boolean useRegex
    ) throws JSONException {
        List<LiquidityCommitmentOffset> liquidityOrders = new ArrayList<>();
        for(int i=0; i<ordersArray.length(); i++) {
            JSONObject object = ordersArray.getJSONObject(i).getJSONObject("liquidityOrder");
            Integer proportion = object.getInt("proportion");
            BigDecimal offset = BigDecimal.valueOf(object.getDouble("offset"));
            PeggedReference reference;
            if(useRegex) {
                reference = PeggedReference.valueOf(object.getString("reference")
                        .replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase());
            } else {
                reference = PeggedReference.valueOf(object.getString("reference")
                        .replace("PEGGED_REFERENCE_", ""));
            }
            LiquidityCommitmentOffset bid = new LiquidityCommitmentOffset()
                    .setOffset(decimalUtils.convertToDecimals(decimalPlaces, offset))
                    .setProportion(proportion)
                    .setReference(reference);
            liquidityOrders.add(bid);
        }
        return liquidityOrders;
    }
}