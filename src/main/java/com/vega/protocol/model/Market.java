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
    private double targetStake;
    private double suppliedStake;
    private double markPrice;
    private double bestBidPrice;
    private double bestAskPrice;
    private double bestBidSize;
    private double bestAskSize;
    private double openInterest;
    private double tau;
    private double mu;
    private double sigma;
    private double minValidPrice;
    private double maxValidPrice;
    private double liquidityFee;
}