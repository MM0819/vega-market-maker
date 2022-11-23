package com.vega.protocol.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class ReferencePrice {
    private double bidPrice;
    private double askPrice;
    private double bidSize;
    private double askSize;
    private double midPrice;
}