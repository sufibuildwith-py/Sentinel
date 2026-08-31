package com.sentinel.revenue.investigation;

import com.sentinel.revenue.model.ClaimValidationStatus;

import java.math.BigDecimal;
import java.util.List;

public record ClaimValidationResult(ClaimValidationStatus status,
                                    BigDecimal effectiveConfidence,
                                    List<String> errors) {
    public ClaimValidationResult { errors = List.copyOf(errors); }
}
