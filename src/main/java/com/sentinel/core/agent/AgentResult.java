package com.sentinel.core.agent;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AgentResult<O>(
        String agentName,
        String summary,
        Confidence confidence,
        List<Evidence> evidence,
        List<Recommendation> recommendations,
        Instant startedAt,
        Instant completedAt,
        AgentStatus status,
        O output
) {
    public AgentResult {
        if (agentName == null || agentName.isBlank()) {
            throw new IllegalArgumentException("agentName must not be blank");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        confidence = Objects.requireNonNull(confidence, "confidence");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        completedAt = Objects.requireNonNull(completedAt, "completedAt");
        status = Objects.requireNonNull(status, "status");
        if (completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("completedAt must not be before startedAt");
        }
    }
}
