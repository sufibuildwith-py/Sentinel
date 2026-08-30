package com.sentinel.core.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String requestId,
        String path,
        List<FieldViolation> violations
) {
    public ApiError {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }
}
