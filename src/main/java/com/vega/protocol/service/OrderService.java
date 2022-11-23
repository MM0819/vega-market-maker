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
import java.math.BigInteger;
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
            final int decimalPlaces
    ) throws JSONException {
        List<LiquidityCommitmentOffset> liquidityOrders = new ArrayList<>();
        for(int i=0; i<ordersArray.length(); i++) {
            JSONObject object = ordersArray.getJSONObject(i).getJSONObject("liquidityOrder");
            int proportion = new BigInteger(object.getString("proportion")).intValue();
            BigDecimal offset = new BigDecimal(object.getString("offset"));
            PeggedReference reference = PeggedReference.valueOf(object.getString("reference")
                        .replace("PEGGED_REFERENCE_", ""));
            LiquidityCommitmentOffset bid = new LiquidityCommitmentOffset()
                    .setOffset(decimalUtils.convertToDecimals(decimalPlaces, offset))
                    .setProportion(proportion)
                    .setReference(reference);
            liquidityOrders.add(bid);
        }
        return liquidityOrders;
    }

    /**
     * Build liquidity orders JSON
     *
     * @param offsets {@link List<LiquidityCommitmentOffset>}
     * @param decimalPlaces market decimal places
     *
     * @return {@link JSONArray}
     */
    public JSONArray buildLiquidityOrders(
            final int decimalPlaces,
            final List<LiquidityCommitmentOffset> offsets
    ) throws JSONException {
        JSONArray array = new JSONArray();
        for(LiquidityCommitmentOffset offset : offsets) {
            JSONObject order = new JSONObject()
                    .put("offset", decimalUtils.convertFromDecimals(decimalPlaces, offset.getOffset())
                            .toBigInteger().toString())
                    .put("proportion", String.valueOf(offset.getProportion()))
                    .put("reference", String.format("PEGGED_REFERENCE_%s", offset.getReference().name()));
            array.put(order);
        }
        return array;
    }
}