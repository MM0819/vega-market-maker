package com.vega.protocol.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class DecimalUtilsTest {

    private DecimalUtils decimalUtils;

    @BeforeEach
    public void setup() {
        decimalUtils = new DecimalUtils();
    }

    @Test
    public void testToDecimal() {
        double number = decimalUtils.convertToDecimals(5, BigDecimal.valueOf(1234567899));
        Assertions.assertEquals(number, 12345.67899);
    }

    @Test
    public void testFromDecimal() {
        BigDecimal number = decimalUtils.convertFromDecimals(5, 12345.67899);
        Assertions.assertEquals(number, BigDecimal.valueOf(1234567899));
    }
}