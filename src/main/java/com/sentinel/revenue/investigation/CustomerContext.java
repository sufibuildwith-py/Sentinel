package com.sentinel.revenue.investigation;

import java.util.List;
import java.util.Map;

public record CustomerContext(int customerCount, int retryCount,
                              Map<String, Long> priorSuccessfulMethods,
                              int historicalIncidentCount,
                              Double historicalRecoveryRate,
                              List<String> evidence) {
    public CustomerContext {
        priorSuccessfulMethods = Map.copyOf(priorSuccessfulMethods);
        evidence = List.copyOf(evidence);
    }
}
