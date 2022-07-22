package com.vega.protocol.exception;

public class TradingException extends RuntimeException {
    public TradingException(String error) {
        super(error);
    }
}