package com.sentinel.revenue.detection;

public record FailureClusterKey(
        String method,
        String issuer,
        String errorCode,
        String merchantId) {
}
