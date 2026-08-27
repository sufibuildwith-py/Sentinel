package com.sentinel.revenue.detection;

import com.sentinel.revenue.model.PaymentEvent;

import java.time.Instant;
import java.util.List;

public record FailureCluster(
        FailureClusterKey key,
        Instant windowStart,
        Instant windowEnd,
        List<PaymentEvent> contributingEvents) {

    public FailureCluster {
        contributingEvents = List.copyOf(contributingEvents);
    }

    public String fingerprint() {
        return "%s|%s|%s|%s|%s".formatted(
                key.method(), key.issuer(), key.errorCode(), key.merchantId(), windowStart);
    }
}
