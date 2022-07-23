package com.vega.protocol.model;

import com.vega.protocol.constant.ErrorCode;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class AppConfig {
    @NotNull(message = ErrorCode.BID_QUOTE_RANGE_MANDATORY)
    private Double bidQuoteRange = 0.05;
    @NotNull(message = ErrorCode.ASK_QUOTE_RANGE_MANDATORY)
    private Double askQuoteRange = 0.05;
    @NotNull(message = ErrorCode.BID_SIZE_FACTOR_MANDATORY)
    private Double bidSizeFactor = 1.0;
    @NotNull(message = ErrorCode.ASK_SIZE_FACTOR_MANDATORY)
    private Double askSizeFactor = 1.0;
    @NotNull(message = ErrorCode.ORDER_COUNT_MANDATORY)
    private Integer orderCount = 10;
    @NotNull(message = ErrorCode.SPREAD_MANDATORY)
    private Double spread = 0.005;
    @NotNull(message = ErrorCode.FEE_MANDATORY)
    private Double fee = 0.001;
}