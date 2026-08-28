package com.sentinel.evaluation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sentinel.evaluation")
public record EvaluationProperties(
        long seed,
        @Min(11) @Max(17) int scenariosPerCategory,
        @NotBlank String reportVersion) {
}
