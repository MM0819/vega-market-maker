package com.vega.protocol.utils;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {

    public static final String HMAC256 = "HmacSHA256";
    public static final String HMAC512 = "HmacSHA512";
    public static final String HMAC384 = "HmacSHA384";

    public static String hmac(byte[] key, byte[] data, String method) {
        try {
            Mac sha_HMAC = Mac.getInstance(method);
            SecretKeySpec secret_key = new SecretKeySpec(key, method);
            sha_HMAC.init(secret_key);
            return Hex.encodeHexString(sha_HMAC.doFinal(data));
        } catch(Exception e) {
            return "";
        }
    }
}
