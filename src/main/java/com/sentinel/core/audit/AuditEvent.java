package com.sentinel.core.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AuditEvent(
        String eventId,
        String correlationId,
        Instant timestamp,
        String actor,
        String action,
        Map<String, Object> details
) {
    public AuditEvent {
        eventId = requireText(eventId, "eventId");
        correlationId = requireText(correlationId, "correlationId");
        timestamp = Objects.requireNonNull(timestamp, "timestamp");
        actor = requireText(actor, "actor");
        action = requireText(action, "action");
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
