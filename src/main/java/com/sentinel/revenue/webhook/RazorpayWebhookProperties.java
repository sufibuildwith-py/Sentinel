package com.sentinel.revenue.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentinel.razorpay.webhook")
public record RazorpayWebhookProperties(String secret) {
    public RazorpayWebhookProperties { secret = secret == null ? "" : secret; }
    public boolean configured() { return !secret.isBlank(); }
}
