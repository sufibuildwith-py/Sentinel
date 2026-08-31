package com.sentinel.evaluation;

import java.util.List;

public record HistoricalValidationCase(
        String caseId,
        String corpusVersion,
        HistoricalSourceClass sourceClass,
        String sourceTitle,
        String sourceRepository,
        String sourceId,
        String sourceUrl,
        String canonicalSourceUrl,
        String sourceDate,
        String retrievedAt,
        String sourceContentHash,
        String productSurface,
        String paymentRail,
        String providerState,
        String normalizedFailureClass,
        String normalizedFailureReason,
        List<String> expectedSafetyInvariants,
        String expectedBehaviorClass,
        boolean outcomeKnown,
        String normalizationNotes,
        String provenanceStatus,
        List<String> aliases,
        List<String> mirrorUrls,
        List<String> derivedReplayIds) {
    public HistoricalValidationCase {
        expectedSafetyInvariants = List.copyOf(expectedSafetyInvariants);
        aliases = List.copyOf(aliases);
        mirrorUrls = List.copyOf(mirrorUrls);
        derivedReplayIds = List.copyOf(derivedReplayIds);
    }
}
