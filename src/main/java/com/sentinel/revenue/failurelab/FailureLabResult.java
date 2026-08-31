package com.sentinel.revenue.failurelab;

import java.time.Instant;
import java.util.List;

public record FailureLabResult(FailureLabScenario scenario, String status,
                               boolean safetyDemonstrationPassed, String observedBehavior,
                               List<String> evidence, Instant evaluatedAt) {
    public FailureLabResult { evidence = List.copyOf(evidence); }
}
