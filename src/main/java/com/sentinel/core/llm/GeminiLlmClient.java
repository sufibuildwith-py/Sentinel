package com.sentinel.core.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sentinel.core.config.GeminiProperties;
import com.sentinel.core.error.InvalidModelResponseException;
import com.sentinel.core.error.UpstreamServiceException;
import com.sentinel.core.error.UpstreamTimeoutException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GeminiLlmClient implements LlmClient {

    private final GeminiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final GeminiCallGuard callGuard;
    private final LlmRuntimeStatus runtimeStatus;

    @Autowired
    public GeminiLlmClient(
            GeminiProperties properties,
            @Qualifier("geminiHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper,
            Validator validator,
            GeminiCallGuard callGuard,
            LlmRuntimeStatus runtimeStatus
    ) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.callGuard = callGuard;
        this.runtimeStatus = runtimeStatus;
    }

    public GeminiLlmClient(GeminiProperties properties, HttpClient httpClient,
                           ObjectMapper objectMapper, Validator validator) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.callGuard = null;
        this.runtimeStatus = new LlmRuntimeStatus(properties);
    }

    @Override
    public <T> T generateStructured(Prompt prompt, Class<T> outputType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(properties.modelEndpoint(properties.model(), "generateContent"))
                .timeout(properties.requestTimeout())
                .header("x-goog-api-key", properties.apiKey())
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(prompt)))
                .build();

            String responseBody = send(request, "Gemini generation");
            String generatedJson = extractGeneratedText(responseBody);
            T result = parseAndValidate(generatedJson, outputType);
            runtimeStatus.record("SUCCESS");
            return result;
        } catch (RuntimeException exception) {
            runtimeStatus.record("ERROR");
            throw exception;
        }
    }

    private String buildRequestBody(Prompt prompt) {
        ObjectNode root = objectMapper.createObjectNode();
        root.putObject("systemInstruction")
                .putArray("parts")
                .addObject()
                .put("text", prompt.systemInstruction());

        ObjectNode userContent = root.putArray("contents").addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", prompt.userMessage());

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseJsonSchema", objectMapper.valueToTree(prompt.responseSchema()));

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize the Gemini request", exception);
        }
    }

    private String send(HttpRequest request, String serviceName) {
        try {
            HttpResponse<String> response = callGuard == null
                    ? httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                    : callGuard.send(httpClient, request, serviceName);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new UpstreamServiceException(serviceName, response.statusCode());
            }
            return response.body();
        } catch (HttpTimeoutException exception) {
            throw new UpstreamTimeoutException(serviceName, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException(serviceName, exception);
        } catch (IOException exception) {
            throw new UpstreamServiceException(serviceName, exception);
        }
    }

    private String extractGeneratedText(String responseBody) {
        try {
            JsonNode candidates = objectMapper.readTree(responseBody).path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new InvalidModelResponseException("Gemini returned no candidates");
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new InvalidModelResponseException("Gemini returned no content");
            }
            String text = parts.get(0).path("text").asText();
            if (text.isBlank()) {
                throw new InvalidModelResponseException("Gemini returned blank content");
            }
            return text;
        } catch (JsonProcessingException exception) {
            throw new InvalidModelResponseException("Gemini returned malformed JSON", exception);
        }
    }

    private <T> T parseAndValidate(String generatedJson, Class<T> outputType) {
        final T result;
        try {
            result = objectMapper.readerFor(outputType)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(generatedJson);
        } catch (JsonProcessingException exception) {
            throw new InvalidModelResponseException("Gemini output did not match the requested JSON structure", exception);
        }

        Set<ConstraintViolation<T>> violations = validator.validate(result);
        if (!violations.isEmpty()) {
            String fields = violations.stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new InvalidModelResponseException("Gemini output failed validation for: " + fields);
        }
        return result;
    }
}
