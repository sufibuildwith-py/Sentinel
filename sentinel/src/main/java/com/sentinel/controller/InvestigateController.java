package com.sentinel.controller;

import com.sentinel.dto.IncidentRequest;
import com.sentinel.dto.InvestigationResponse;
import com.sentinel.dto.RunbookEntry;
import com.sentinel.service.EmbeddingService;
import com.sentinel.service.GeminiService;
import com.sentinel.service.RunbookStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class InvestigateController {

    private final GeminiService geminiService;
    private final EmbeddingService embeddingService;
    private final RunbookStore runbookStore;

    public InvestigateController(GeminiService geminiService, EmbeddingService embeddingService, RunbookStore runbookStore) {
        this.geminiService = geminiService;
        this.embeddingService = embeddingService;
        this.runbookStore = runbookStore;
    }

    @PostMapping("/investigate")
    public InvestigationResponse investigate(@RequestBody IncidentRequest request) {
        // Step 1: turn the incoming incident into a vector
        float[] incidentVector = embeddingService.embed(request.getIncident());

        // Step 2: find the most similar past incident(s) in our runbook store
        List<RunbookEntry> matches = runbookStore.findMostSimilar(incidentVector, 1);

        String retrievedContext = matches.stream()
                .map(RunbookEntry::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        // Step 3: ask Gemini to diagnose, now armed with that retrieved context
        String diagnosis = geminiService.investigate(request.getIncident(), retrievedContext);

        return new InvestigationResponse(diagnosis);
    }
}
