package com.sentinel.revenue.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Sanitized provider error details safe for persistence, logs, and operator views. */
public record ProviderError(int httpStatus, String code, String field, String description,
                            String source, String step, String reason) {

    public static ProviderError from(int httpStatus, JsonNode body) {
        JsonNode error = body == null ? null : body.path("error");
        if (error == null || error.isMissingNode()) error = body;
        return new ProviderError(httpStatus, text(error, "code", "HTTP_" + httpStatus),
                text(error, "field", null), text(error, "description", null),
                text(error, "source", null), text(error, "step", null),
                text(error, "reason", null));
    }

    public static ProviderError fromMessage(ObjectMapper mapper, String message) {
        if (message != null) {
            try {
                JsonNode body = mapper.readTree(message);
                if (body != null && body.isObject()) return from(statusFrom(body), body);
            } catch (Exception ignored) {
                // SDK messages are not guaranteed to be JSON; never retain the raw message.
            }
        }
        return new ProviderError(statusFrom(message), "SDK_FAILURE", null, null, null, null, null);
    }

    private static int statusFrom(JsonNode body) {
        return body != null && body.path("error").path("http_status_code").canConvertToInt()
                ? body.path("error").path("http_status_code").asInt() : 400;
    }

    private static int statusFrom(String message) {
        if (message != null && message.contains("400")) return 400;
        if (message != null && message.contains("401")) return 401;
        if (message != null && message.contains("429")) return 429;
        if (message != null && message.contains("500")) return 500;
        return 0;
    }

    public String safeCode() {
        return code == null || code.isBlank() ? "HTTP_" + httpStatus : code;
    }

    public String safeSummary() {
        StringBuilder summary = new StringBuilder(safeCode());
        append(summary, "field", field);
        append(summary, "reason", reason);
        append(summary, "source", source);
        append(summary, "step", step);
        append(summary, "description", description);
        return summary.toString();
    }

    private static String text(JsonNode node, String name, String fallback) {
        if (node == null) return fallback;
        String value = node.path(name).asText("").trim();
        return value.isBlank() ? fallback : value.length() > 240 ? value.substring(0, 240) : value;
    }

    private static void append(StringBuilder target, String name, String value) {
        if (value != null && !value.isBlank()) target.append(' ').append(name).append('=').append(value);
    }
}
