package com.sentinel.revenue.api;

public record BatchValidationError(
        int index,
        String paymentId,
        String field,
        String message) {
}
