package com.sentinel.revenue.investigation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RootCauseResult(
        @NotBlank String rootCause,
        @DecimalMin("0.0") @DecimalMax("1.0") double confidence,
        @NotEmpty @Size(max = 20) List<@NotBlank String> evidence,
        @NotNull @Size(max = 10) List<@NotBlank String> alternativeHypotheses,
        boolean llmUnavailable) {
    public RootCauseResult {
        evidence = evidence == null ? null : List.copyOf(evidence);
        alternativeHypotheses = alternativeHypotheses == null
                ? null : List.copyOf(alternativeHypotheses);
    }
}
