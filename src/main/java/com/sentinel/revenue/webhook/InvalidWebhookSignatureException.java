package com.sentinel.revenue.webhook;

public final class InvalidWebhookSignatureException extends RuntimeException {
    public InvalidWebhookSignatureException() { super("Webhook signature is invalid"); }
}
