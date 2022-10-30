package com.vega.protocol.model;

import com.vega.protocol.constant.MarketState;
import com.vega.protocol.constant.MarketTradingMode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

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
    private BigDecimal targetStake;
    private BigDecimal suppliedStake;
    private BigDecimal markPrice;
    private BigDecimal bestBidPrice;
    private BigDecimal bestAskPrice;
    private BigDecimal bestBidSize;
    private BigDecimal bestAskSize;
    private BigDecimal openInterest;
    private double tau;
    private double mu;
    private double sigma;
    private BigDecimal minValidPrice;
    private BigDecimal maxValidPrice;
}