package com.sentinel.core.llm;

import java.util.Map;
import java.util.Objects;

public record Prompt(
        String systemInstruction,
        String userMessage,
        Map<String, Object> responseSchema
) {
    public Prompt {
        if (systemInstruction == null || systemInstruction.isBlank()) {
            throw new IllegalArgumentException("systemInstruction must not be blank");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be blank");
        }
        responseSchema = Map.copyOf(Objects.requireNonNull(responseSchema, "responseSchema"));
        if (responseSchema.isEmpty()) {
            throw new IllegalArgumentException("responseSchema must not be empty");
        }
    }
}
