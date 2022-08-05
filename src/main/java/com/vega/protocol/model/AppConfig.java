package com.vega.protocol.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AppConfig {
    private Double bidQuoteRange;
    private Double askQuoteRange;
    private Double bidSizeFactor;
    private Double askSizeFactor;
    private Integer orderCount;
    private Double spread;
    private Double fee;
    private Double pricingStepSize;
}