package com.sentinel.evaluation;

import java.util.List;
import java.util.Map;

public record HistoricalValidationReport(
        String title,
        String truthLabel,
        String corpusVersion,
        String manifestSha256,
        int acceptedPublicSourceCases,
        int derivedReplayCount,
        String oldestSourceDate,
        String newestSourceDate,
        Map<HistoricalSourceClass, Long> sourceComposition,
        int passed,
        int partial,
        int failed,
        int safeRefusals,
        int unsafeExecutions,
        int duplicateFinancialEffects,
        int unverifiedRecoveryClaims,
        double decisionTraceCompleteness,
        double replayDeterminismRate,
        List<CaseResult> cases,
        List<String> limitations) {
    public record CaseResult(String caseId, HistoricalSourceClass sourceClass, String sourceTitle,
                             String sourceDate, String sourceUrl, String productSurface, String paymentRail,
                             String providerState, String normalizedFailureClass, String normalizedFailureReason,
                             String expectedBehaviorClass, boolean outcomeKnown,
                             List<String> expectedInvariants, List<String> observedInvariants,
                             String result, boolean safeRefusal, boolean unexpectedExecution,
                             boolean unverifiedRecoveryClaim, boolean duplicateFinancialEffect,
                             boolean traceComplete, int logicalLatencyMillis,
                             String policyDisposition, String evidenceLabel) { }
}
