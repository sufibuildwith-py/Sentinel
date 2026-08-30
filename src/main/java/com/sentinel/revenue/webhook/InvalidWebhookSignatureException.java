package com.sentinel.revenue.webhook;

public class InvalidWebhookSignatureException extends RuntimeException {
    public InvalidWebhookSignatureException() { super("Webhook signature is invalid"); }
}
