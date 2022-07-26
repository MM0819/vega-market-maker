package com.vega.protocol.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class ReferencePrice {
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private BigDecimal bidSize;
    private BigDecimal askSize;
    private BigDecimal midPrice;
}