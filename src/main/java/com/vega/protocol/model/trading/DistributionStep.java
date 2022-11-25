package com.vega.protocol.model.trading;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DistributionStep {
    private double price;
    private double size;
}