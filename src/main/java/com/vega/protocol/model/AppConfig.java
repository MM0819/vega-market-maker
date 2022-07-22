package com.vega.protocol.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AppConfig {
    private Double bidQuoteRange = 0.05;
    private Double askQuoteRange = 0.05;
    private Double bidSizeFactor = 1.0;
    private Double askSizeFactor = 1.0;
    private Integer orderCount = 10;
    private Double spread = 0.005;
}