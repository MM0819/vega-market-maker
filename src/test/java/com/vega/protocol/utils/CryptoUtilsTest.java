package com.vega.protocol.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class CryptoUtilsTest {

    @Test
    public void testInstantiate() {
        CryptoUtils utils = new CryptoUtils();
        Assertions.assertNotNull(utils);
    }

    @Test
    public void testHmac512() {
        String result = CryptoUtils.hmac("123".getBytes(StandardCharsets.UTF_8),
                "test".getBytes(StandardCharsets.UTF_8), CryptoUtils.HMAC512);
        Assertions.assertTrue(result.length() > 0);
    }

    @Test
    public void testHmac256() {
        String result = CryptoUtils.hmac("123".getBytes(StandardCharsets.UTF_8),
                "test".getBytes(StandardCharsets.UTF_8), CryptoUtils.HMAC256);
        Assertions.assertTrue(result.length() > 0);
    }

    @Test
    public void testHmac384() {
        String result = CryptoUtils.hmac("123".getBytes(StandardCharsets.UTF_8),
                "test".getBytes(StandardCharsets.UTF_8), CryptoUtils.HMAC384);
        Assertions.assertTrue(result.length() > 0);
    }

    @Test
    public void testHmacWithError() {
        String result = CryptoUtils.hmac(null, null, CryptoUtils.HMAC384);
        Assertions.assertEquals(0, result.length());
    }
}
