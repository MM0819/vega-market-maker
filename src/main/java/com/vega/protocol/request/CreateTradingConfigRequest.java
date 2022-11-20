package com.vega.protocol.request;

import com.vega.protocol.constant.ReferencePriceSource;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class CreateTradingConfigRequest {
    private String marketId;
    private String partyId;
    private BigDecimal allocatedMargin;
    private ReferencePriceSource referencePriceSource;
    private String referencePriceSymbol;
    private String hedgeSymbol;
}