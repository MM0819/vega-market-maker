package com.vega.protocol.utils.auth;

import com.vega.protocol.model.SignatureWrapper;
import com.vega.protocol.utils.CryptoUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BinanceAuthUtils {
    public static SignatureWrapper getSignature(Map<String, String> params, String secret) {
        String timestamp = String.valueOf(LocalDateTime.now()
                .minusHours(1).toEpochSecond(ZoneOffset.UTC) * 1000);
        params.put("recvWindow", "50000");
        params.put("timestamp", timestamp);
        List<NameValuePair> queryParams = params.entrySet()
                .stream()
                .map(es -> new BasicNameValuePair(es.getKey(), es.getValue()))
                .collect(Collectors.toList());
        String queryString = URLEncodedUtils.format(queryParams, StandardCharsets.UTF_8);
        String signature = CryptoUtils.hmac(secret.getBytes(StandardCharsets.UTF_8),
                queryString.getBytes(StandardCharsets.UTF_8), CryptoUtils.HMAC256);
        return new SignatureWrapper()
                .setQueryString(queryString)
                .setSignature(signature)
                .setMessage(queryString)
                .setNonce(timestamp);
    }
}
