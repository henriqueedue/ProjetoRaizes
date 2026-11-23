package com.raizes.raizesback.infrastructure.payment;

public class GatewayIndisponivelException extends RuntimeException {
    public GatewayIndisponivelException(String message) { super(message); }
}
