package com.sentinel.core.agent;

public record Recommendation(String action, String rationale) {

    public Recommendation {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("rationale must not be blank");
        }
    }
}
