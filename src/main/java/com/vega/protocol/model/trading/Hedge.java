package com.vega.protocol.model.trading;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.model.UniqueItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class Hedge extends UniqueItem {
    private String id;
    private double entryPrice;
    private double size;
    private MarketSide side;
}
