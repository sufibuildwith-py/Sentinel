package com.sentinel.core.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        @NotBlank String apiKey,
        @NotBlank String model,
        @NotBlank String embeddingModel,
        @NotNull URI baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration requestTimeout
) {
    public GeminiProperties {
        if (connectTimeout != null && (connectTimeout.isZero() || connectTimeout.isNegative())) {
            throw new IllegalArgumentException("gemini.connect-timeout must be positive");
        }
        if (requestTimeout != null && (requestTimeout.isZero() || requestTimeout.isNegative())) {
            throw new IllegalArgumentException("gemini.request-timeout must be positive");
        }
    }

    public URI modelEndpoint(String modelName, String operation) {
        String root = baseUrl.toString().replaceAll("/+$", "");
        return URI.create(root + "/models/" + modelName + ":" + operation);
    }
}
