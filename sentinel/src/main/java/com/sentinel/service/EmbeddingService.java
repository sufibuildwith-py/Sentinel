package com.sentinel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

// Turns a piece of text into a vector (a list of numbers representing its meaning).
// Same request/response pattern as GeminiService, just a different endpoint and model.
@Service
public class EmbeddingService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String EMBEDDING_MODEL = "gemini-embedding-001";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String buildUrl() {
        return "https://generativelanguage.googleapis.com/v1beta/models/" + EMBEDDING_MODEL + ":embedContent";
    }

    public float[] embed(String text) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode content = objectMapper.createObjectNode();
            content.putArray("parts").addObject().put("text", text);
            root.set("content", content);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl()))
                    .header("x-goog-api-key", apiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Embedding call failed (status " + response.statusCode() + "): " + response.body());
            }

            return extractVector(response.body());

        } catch (Exception e) {
            throw new RuntimeException("Failed to embed text", e);
        }
    }

    // Response shape: { "embedding": { "values": [0.01, -0.02, ...] } }
    private float[] extractVector(String responseBody) throws Exception {
        JsonNode valuesNode = objectMapper.readTree(responseBody).path("embedding").path("values");
        List<Float> values = new ArrayList<>();
        valuesNode.forEach(node -> values.add((float) node.asDouble()));

        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i);
        }
        return vector;
    }
}
