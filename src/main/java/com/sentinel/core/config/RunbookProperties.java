package com.sentinel.core.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Validated
@ConfigurationProperties(prefix = "runbooks")
public record RunbookProperties(
        @NotNull Path path,
        @Min(1) @Max(20) int topK,
        @DecimalMin("-1.0") @DecimalMax("1.0") double minimumSimilarity
) {
}
