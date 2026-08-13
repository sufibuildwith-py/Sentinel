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

// Same shape as ClaudeService - build JSON, send HTTP request, parse JSON back.
// Gemini's request/response format is shaped a bit differently, but the pattern is identical.
@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Gemini puts the model name directly in the URL, unlike Claude where it goes in the JSON body
    private String buildUrl() {
        return "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
    }

    private static final String SYSTEM_PROMPT = """
            You are an SRE assistant helping an on-call engineer triage a production incident.
            You will be given the current incident, plus one or more similar past incidents
            retrieved from a runbook archive. Use the past incidents as evidence where relevant,
            but do not assume the current incident is identical - it may be a different root cause.
            Respond with:
            1. A short first-pass hypothesis of what might be wrong, citing the past incident if it helped
            2. What you'd check next to confirm it
            Keep it concise - a few sentences, not an essay. Be direct about your uncertainty
            if the past incidents don't clearly match.
            """;

    // context can be null/empty if no relevant runbook was found - the prompt still works either way
    public String investigate(String incidentDescription, String retrievedContext) {
        try {
            String requestBody = buildRequestBody(incidentDescription, retrievedContext);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl()))
                    .header("x-goog-api-key", apiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Gemini API call failed (status " + response.statusCode() + "): " + response.body());
            }

            return extractText(response.body());

        } catch (Exception e) {
            throw new RuntimeException("Failed to get investigation from Gemini", e);
        }
    }

    // Gemini's body shape: { "system_instruction": {...}, "contents": [ { "role": "user", "parts": [{"text": "..."}] } ] }
    private String buildRequestBody(String incidentDescription, String retrievedContext) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode systemInstruction = objectMapper.createObjectNode();
        systemInstruction.putArray("parts").addObject().put("text", SYSTEM_PROMPT);
        root.set("system_instruction", systemInstruction);

        String userText = "Current incident: " + incidentDescription;
        if (retrievedContext != null && !retrievedContext.isBlank()) {
            userText += "\n\nSimilar past incident(s) retrieved from the runbook archive:\n" + retrievedContext;
        } else {
            userText += "\n\nNo similar past incident was found in the runbook archive.";
        }

        ObjectNode userContent = objectMapper.createObjectNode();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", userText);

        root.putArray("contents").add(userContent);

        return objectMapper.writeValueAsString(root);
    }

    // Gemini's response shape: { "candidates": [ { "content": { "parts": [ { "text": "..." } ] } } ] }
    private String extractText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        return root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();
    }
}
