package com.sentinel.revenue.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PaymentEventBatchRequest(
        @NotEmpty
        @Size(max = 1_000)
        List<PaymentEventRequest> events) {
}
