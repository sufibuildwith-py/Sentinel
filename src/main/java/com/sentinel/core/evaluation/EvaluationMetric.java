package com.sentinel.core.evaluation;

public record EvaluationMetric(String name, double value, long sampleCount) {

    public EvaluationMetric {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite");
        }
        if (sampleCount < 0) {
            throw new IllegalArgumentException("sampleCount must not be negative");
        }
    }
}
