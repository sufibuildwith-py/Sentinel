package com.sentinel.core.llm;

import com.sentinel.core.config.GeminiProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Sanitized operational state; intentionally contains no credential material. */
@Component
public final class LlmRuntimeStatus {
    private final GeminiProperties properties;
    private final AtomicReference<Invocation> last = new AtomicReference<>();

    public LlmRuntimeStatus(GeminiProperties properties) { this.properties = properties; }

    public void record(String result) { last.set(new Invocation(Instant.now(), result)); }

    public Map<String, Object> snapshot() {
        Invocation invocation = last.get();
        return Map.of(
                "provider", "GEMINI",
                "configured", configured(),
                "model", properties.model(),
                "lastInvocation", invocation == null ? nullSafeTimestamp() : invocation.timestamp(),
                "lastResult", invocation == null ? "NOT_INVOKED" : invocation.result());
    }

    public boolean configured() {
        String key = properties.apiKey();
        return key != null && !key.isBlank() && !key.equals("local-development-key")
                && !key.equals("test-key") && !key.equals("changeme");
    }

    private String nullSafeTimestamp() { return "NOT_INVOKED"; }

    private record Invocation(Instant timestamp, String result) { }
}
