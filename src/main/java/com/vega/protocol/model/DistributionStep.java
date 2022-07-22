package com.vega.protocol.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DistributionStep {
    private Double price;
    private Double size;
}