package com.vega.protocol.model;

import com.vega.protocol.constant.MarketState;
import com.vega.protocol.constant.MarketTradingMode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class Market extends UniqueItem {
    private String id;
    private String name;
    private MarketState state;
    private MarketTradingMode tradingMode;
    private String settlementAsset;
    private int decimalPlaces;
    private int positionDecimalPlaces;
}