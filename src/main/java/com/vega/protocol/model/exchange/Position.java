package com.vega.protocol.model.exchange;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Position {
    private double price;
    private double size;
    private String symbol;
}