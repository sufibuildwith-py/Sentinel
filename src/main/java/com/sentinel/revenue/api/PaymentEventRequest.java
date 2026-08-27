package com.sentinel.revenue.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public record PaymentEventRequest(
        @NotBlank @Size(max = 128) String paymentId,
        @NotBlank @Size(max = 128) String orderId,
        @NotBlank @Size(max = 128) String customerId,
        @Positive long amountMinor,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotBlank @Size(max = 64) String method,
        @Size(max = 128) String issuerBank,
        @NotBlank @Size(max = 64) String status,
        @Size(max = 128) String errorCode,
        String errorDescription,
        @NotNull Instant timestamp,
        @Min(1) int attemptNumber,
        @Size(max = 64) String previousSuccessfulMethod,
        @PositiveOrZero int previousFailureCount,
        @Size(max = 128) String subscriptionId,
        Map<String, Object> metadata) {
}
