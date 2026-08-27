package com.sentinel.revenue.investigation;

import java.util.List;

public record TriageResult(String category, String severity,
                           String investigationStrategy, List<String> requiredTools,
                           List<String> evidence, boolean llmUnavailable) {
    public TriageResult {
        requiredTools = List.copyOf(requiredTools);
        evidence = List.copyOf(evidence);
    }
}
