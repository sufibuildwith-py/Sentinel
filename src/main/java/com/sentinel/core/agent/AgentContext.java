package com.sentinel.core.agent;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AgentContext(
        String correlationId,
        Instant startedAt,
        Instant deadline,
        Map<String, Object> attributes
) {
    public AgentContext {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId must not be blank");
        }
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        deadline = Objects.requireNonNull(deadline, "deadline");
        if (deadline.isBefore(startedAt)) {
            throw new IllegalArgumentException("deadline must not be before startedAt");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
