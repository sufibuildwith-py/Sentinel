package com.sentinel.revenue.detection;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "sentinel.detection")
public record DetectionProperties(
        @NotNull Duration evaluationWindow,
        @NotNull Duration clusterWindow,
        @NotNull Duration baselineWindow,
        @NotNull Duration baselineBucket,
        @Min(1) int minimumVolume,
        @DecimalMin("0.0") @DecimalMax("1.0") double minimumSuccessRateDrop,
        @DecimalMin("0.0") double minimumBaselineDeviation,
        @Min(0) long minimumAmountAtRiskMinor,
        @DecimalMin("0.0") @DecimalMax("1.0") double defaultBaselineSuccessRate,
        @DecimalMin(value = "0.0", inclusive = false)
        double minimumBaselineStandardDeviation,
        @NotEmpty Set<@NotBlank String> successStatuses,
        @NotBlank String merchantMetadataKey) {

    public DetectionProperties {
        requirePositive(evaluationWindow, "evaluation-window");
        requirePositive(clusterWindow, "cluster-window");
        requirePositive(baselineWindow, "baseline-window");
        requirePositive(baselineBucket, "baseline-bucket");
        successStatuses = successStatuses == null ? Set.of() : Set.copyOf(successStatuses);
    }

    private static void requirePositive(Duration duration, String property) {
        if (duration != null && (duration.isZero() || duration.isNegative())) {
            throw new IllegalArgumentException(
                    "sentinel.detection." + property + " must be positive");
        }
    }
}
