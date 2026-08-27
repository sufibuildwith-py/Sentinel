package com.sentinel.core.agent;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record Evidence(
        String source,
        String description,
        Instant observedAt,
        Map<String, Object> attributes
) {
    public Evidence {
        source = requireText(source, "source");
        description = requireText(description, "description");
        observedAt = Objects.requireNonNull(observedAt, "observedAt");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
