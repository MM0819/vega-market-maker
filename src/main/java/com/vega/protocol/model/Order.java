package com.vega.protocol.model;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.constant.OrderStatus;
import com.vega.protocol.constant.OrderType;
import com.vega.protocol.constant.TimeInForce;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class Order extends UniqueItem {
    private String id;
    private String partyId;
    private Market market;
    private BigDecimal price;
    private BigDecimal size;
    private BigDecimal remainingSize;
    private MarketSide side;
    private OrderStatus status;
    private OrderType type;
    private TimeInForce timeInForce;
}