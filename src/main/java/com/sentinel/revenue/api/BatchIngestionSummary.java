package com.sentinel.revenue.api;

import java.util.List;

public record BatchIngestionSummary(
        int count,
        int duplicatesSkipped,
        List<BatchValidationError> validationErrors) {

    public BatchIngestionSummary {
        validationErrors = List.copyOf(validationErrors);
    }
}
