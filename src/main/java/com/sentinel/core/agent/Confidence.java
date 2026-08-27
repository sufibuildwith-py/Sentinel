package com.sentinel.core.agent;

public record Confidence(double value) {

    public Confidence {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }
}
