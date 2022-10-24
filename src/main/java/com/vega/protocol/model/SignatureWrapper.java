package com.vega.protocol.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SignatureWrapper {
    private String signature;
    private String message;
    private String queryString;
    private String nonce;
}