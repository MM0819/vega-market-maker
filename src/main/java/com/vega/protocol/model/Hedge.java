package com.vega.protocol.model;

import com.vega.protocol.constant.MarketSide;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class Hedge extends UniqueItem {
    private String id;
    private BigDecimal entryPrice;
    private BigDecimal size;
    private MarketSide side;
}
