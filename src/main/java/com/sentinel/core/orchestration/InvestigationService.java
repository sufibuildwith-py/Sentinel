package com.sentinel.core.orchestration;

import com.sentinel.core.config.RunbookProperties;
import com.sentinel.core.llm.EmbeddingClient;
import com.sentinel.core.llm.LlmClient;
import com.sentinel.core.llm.Prompt;
import com.sentinel.core.memory.MemoryMatch;
import com.sentinel.core.memory.RunbookMemory;
import com.sentinel.dto.InvestigationResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InvestigationService {

    private static final String SYSTEM_INSTRUCTION = """
            You are an SRE assistant helping an on-call engineer triage a production incident.
            Use the supplied historical incidents as evidence where relevant, but do not assume
            the current incident has the same root cause. Return a concise first-pass hypothesis
            and the next checks needed to confirm it. Cite a historical incident when it helped,
            and state uncertainty directly when the evidence is weak.
            """;

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "diagnosis", Map.of(
                            "type", "string",
                            "description", "A concise evidence-grounded hypothesis and the next checks"
                    )
            ),
            "required", List.of("diagnosis"),
            "additionalProperties", false
    );

    private final EmbeddingClient embeddingClient;
    private final RunbookMemory runbookMemory;
    private final LlmClient llmClient;
    private final RunbookProperties runbookProperties;

    public InvestigationService(
            EmbeddingClient embeddingClient,
            RunbookMemory runbookMemory,
            LlmClient llmClient,
            RunbookProperties runbookProperties
    ) {
        this.embeddingClient = embeddingClient;
        this.runbookMemory = runbookMemory;
        this.llmClient = llmClient;
        this.runbookProperties = runbookProperties;
    }

    public InvestigationResponse investigate(String incident) {
        if (incident == null || incident.isBlank()) {
            throw new IllegalArgumentException("incident must not be blank");
        }

        float[] incidentEmbedding = embeddingClient.embed(incident);
        List<MemoryMatch> matches = runbookMemory.findSimilar(
                incidentEmbedding,
                runbookProperties.topK(),
                runbookProperties.minimumSimilarity()
        );

        Prompt prompt = new Prompt(
                SYSTEM_INSTRUCTION,
                buildUserMessage(incident, matches),
                RESPONSE_SCHEMA
        );
        InvestigationDraft draft = llmClient.generateStructured(prompt, InvestigationDraft.class);
        return new InvestigationResponse(draft.diagnosis());
    }

    private String buildUserMessage(String incident, List<MemoryMatch> matches) {
        String context = matches.stream()
                .map(match -> "Source: %s\nSimilarity: %.4f\n%s".formatted(
                        match.document().source(),
                        match.similarity(),
                        match.document().content()
                ))
                .collect(Collectors.joining("\n\n---\n\n"));

        if (context.isBlank()) {
            return "Current incident: " + incident
                    + "\n\nNo sufficiently similar historical incident was found.";
        }
        return "Current incident: " + incident
                + "\n\nSimilar historical incident evidence:\n" + context;
    }
}
