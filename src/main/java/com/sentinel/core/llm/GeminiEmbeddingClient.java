package com.sentinel.core.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sentinel.core.config.GeminiProperties;
import com.sentinel.core.error.InvalidModelResponseException;
import com.sentinel.core.error.UpstreamServiceException;
import com.sentinel.core.error.UpstreamTimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

@Component
public class GeminiEmbeddingClient implements EmbeddingClient {

    private final GeminiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiEmbeddingClient(GeminiProperties properties,
                                @Qualifier("geminiHttpClient") HttpClient httpClient,
                                ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", "models/" + properties.embeddingModel());
        root.putObject("content").putArray("parts").addObject().put("text", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(embeddingEndpoint())
                .timeout(properties.requestTimeout())
                .header("x-goog-api-key", properties.apiKey())
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(serialize(root)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new UpstreamServiceException("Gemini embedding", response.statusCode());
            }
            return extractVector(response.body());
        } catch (HttpTimeoutException exception) {
            throw new UpstreamTimeoutException("Gemini embedding", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException("Gemini embedding", exception);
        } catch (IOException exception) {
            throw new UpstreamServiceException("Gemini embedding", exception);
        }
    }

    private java.net.URI embeddingEndpoint() {
        return properties.modelEndpoint(properties.embeddingModel(), "embedContent");
    }

    private String serialize(JsonNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize the embedding request", exception);
        }
    }

    private float[] extractVector(String responseBody) {
        try {
            JsonNode values = objectMapper.readTree(responseBody).path("embedding").path("values");
            if (!values.isArray() || values.isEmpty()) {
                throw new InvalidModelResponseException("Gemini returned an empty embedding");
            }
            float[] vector = new float[values.size()];
            for (int index = 0; index < values.size(); index++) {
                vector[index] = (float) values.get(index).asDouble();
            }
            return vector;
        } catch (JsonProcessingException exception) {
            throw new InvalidModelResponseException("Gemini returned a malformed embedding response", exception);
        }
    }
}
