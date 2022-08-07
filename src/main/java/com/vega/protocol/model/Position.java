package com.vega.protocol.model;

import com.vega.protocol.constant.MarketSide;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class Position extends UniqueItem {
    private String id;
    private String partyId;
    private Market market;
    private BigDecimal size;
    private MarketSide side;
    private BigDecimal entryPrice;
    private BigDecimal unrealisedPnl;
    private BigDecimal realisedPnl;
}