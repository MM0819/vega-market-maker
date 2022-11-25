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
    public Double convertToDecimals(
            final long decimalPlaces,
            final BigDecimal number
    ) {
        if(number == null) return null;
        return number.divide(BigDecimal.valueOf(Math.pow(10, decimalPlaces)), (int) decimalPlaces, RoundingMode.HALF_DOWN)
                .setScale((int) decimalPlaces, RoundingMode.HALF_DOWN).doubleValue();
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
            final long decimalPlaces,
            final Double number
    ) {
        if(number == null) return null;
        return BigDecimal.valueOf(Math.pow(10, decimalPlaces)).multiply(BigDecimal.valueOf(number))
                .setScale(0, RoundingMode.HALF_DOWN);
    }
}