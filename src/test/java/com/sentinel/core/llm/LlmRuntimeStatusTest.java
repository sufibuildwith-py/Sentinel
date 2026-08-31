package com.sentinel.core.llm;

import com.sentinel.core.config.GeminiProperties;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LlmRuntimeStatusTest {
    @Test
    void exposesOnlySanitizedProviderState() {
        GeminiProperties properties = new GeminiProperties("super-secret-api-key", "gemini-test-model",
                "embedding-model", URI.create("https://example.test"), Duration.ofSeconds(1), Duration.ofSeconds(2));
        LlmRuntimeStatus status = new LlmRuntimeStatus(properties);

        status.record("SUCCESS");
        var snapshot = status.snapshot();

        assertThat(snapshot).containsEntry("provider", "GEMINI")
                .containsEntry("configured", true)
                .containsEntry("model", "gemini-test-model")
                .containsEntry("lastResult", "SUCCESS");
        assertThat(snapshot.toString()).doesNotContain("super-secret-api-key");
    }

    @Test
    void placeholderKeyIsNotReportedAsConfigured() {
        GeminiProperties properties = new GeminiProperties("local-development-key", "gemini-test-model",
                "embedding-model", URI.create("https://example.test"), Duration.ofSeconds(1), Duration.ofSeconds(2));
        assertThat(new LlmRuntimeStatus(properties).snapshot())
                .containsEntry("configured", false)
                .containsEntry("lastResult", "NOT_INVOKED");
    }
}
