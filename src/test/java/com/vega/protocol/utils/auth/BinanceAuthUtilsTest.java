package com.vega.protocol.utils.auth;

import com.vega.protocol.model.SignatureWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class BinanceAuthUtilsTest {

    @Test
    public void testInstantiate() {
        BinanceAuthUtils authUtils = new BinanceAuthUtils();
        Assertions.assertNotNull(authUtils);
    }

    @Test
    public void testGetSignature() {
        Map<String, String> params = new HashMap<>();
        params.put("test", "hello");
        SignatureWrapper wrapper = BinanceAuthUtils.getSignature(params, "xxx");
        Assertions.assertNotNull(wrapper);
    }
}