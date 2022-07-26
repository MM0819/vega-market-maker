package com.vega.protocol.utils;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DecimalUtils {

    /**
     * Convert number to decimal version
     *
     * @param decimalPlaces number of decimal places
     * @param number integer representation
     *
     * @return decimal representation
     */
    public BigDecimal convertToDecimals(
            final int decimalPlaces,
            final BigDecimal number
    ) {
        return number.divide(BigDecimal.valueOf(Math.pow(10, decimalPlaces)), decimalPlaces, RoundingMode.HALF_DOWN)
                .setScale(decimalPlaces, RoundingMode.HALF_DOWN);
    }

    /**
     * Convert number from decimal version
     *
     * @param decimalPlaces number of decimal places
     * @param number decimal representation
     *
     * @return integer representation
     */
    public BigDecimal convertFromDecimals(
            final int decimalPlaces,
            final BigDecimal number
    ) {
        return BigDecimal.valueOf(Math.pow(10, decimalPlaces)).multiply(number)
                .setScale(0, RoundingMode.HALF_DOWN);
    }
}