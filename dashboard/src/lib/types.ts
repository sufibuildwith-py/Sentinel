export type IncidentStatus = "DETECTED" | "INVESTIGATING" | "DIAGNOSED" | "PLANNING" | "POLICY_REVIEW" | "APPROVED" | "HUMAN_REVIEW" | "EXECUTING" | "MONITORING" | "RECOVERED" | "FAILED" | "STOPPED";

export interface IncidentSummary { incidentId: string; type: string; status: IncidentStatus; severity: string; amountAtRiskMinor: number; detectedAt: string; affectedPaymentCount: number; affectedCustomerCount: number; strategy?: string | null; policyDecision?: "AUTO" | "HUMAN" | "DENY" | null; actionStatus?: string | null; latestOutcome?: string | null; recoveredAmountMinor: number; }
export interface Finding { source: string; summary: string; confidence?: number | null; evidence: string[]; createdAt: string; }
export interface PlanView { planId: string; strategy: string; reason: string; targetAmountMinor: number; confidence: number; riskLevel: string; }
export interface ActionView { actionId: string; status: string; policyDecision?: string | null; amountMinor: number; currency: string; providerId?: string | null; referenceId?: string | null; shortUrl?: string | null; providerStatus?: string | null; lastErrorCode?: string | null; executionAttempts: number; approvedAt?: string | null; executedAt?: string | null; }
export interface GovernorView { decisionId: string; allowed: boolean; allowedValueMinor: number; violations: string[]; evaluatedAt: string; }
export interface RecoveryTruth { stage: "PROPOSED" | "POLICY_APPROVED" | "EXECUTION_REQUESTED" | "PROVIDER_ACCEPTED" | "AWAITING_RECONCILIATION" | "RECOVERED_CONFIRMED" | "FAILED_CONFIRMED" | "EXPIRED"; executionMode: "RAZORPAY_TEST_MODE" | "SIMULATION" | "FAULT_INJECTION" | "SYNTHETIC_BENCHMARK" | "SHADOW_ONLY" | "LEGACY_UNSPECIFIED"; providerAccepted: boolean; awaitingReconciliation: boolean; providerConfirmed: boolean; providerConfirmedAmountMinor: number; basis: string; }
export interface ExecutionAvailability { enabled: boolean; eligible: boolean; reasonCode: string; reason: string; }
export interface BackendInfo { application?: { name?: string; version?: string; commit?: string; buildTime?: string; }; providerExecution?: { provider: string; mode: string; enabled: boolean; credentialsConfigured: boolean; }; }
export interface IncidentDetail { incident: IncidentSummary; findings: Finding[]; plan?: PlanView | null; action?: ActionView | null; governor?: GovernorView | null; truth?: RecoveryTruth | null; executionAvailability?: ExecutionAvailability | null; }
export interface EvidenceCapsule {
  incidentId: string; assembledAt: string;
  webhooks: { eventId: string; eventType: string; verified: boolean; processed: boolean; receivedAt: string; processedAt?: string | null; }[];
  providerTruth: RecoveryTruth;
  systemicEvidence: { evidenceId: string; source: string; summary: string; confidence?: number | null; capturedAt: string; validUntil?: string | null; fresh: boolean; }[];
  agentClaims: { claimId: string; claimType: string; claim: string; confidence: number; evidenceRefs: string[]; contradictingEvidenceRefs: string[]; proposedAction?: string | null; validationStatus: "VALID" | "DOWNGRADED" | "REJECTED"; validationErrors: string[]; createdAt: string; }[];
  prediction?: string | null;
  policy: { timestamp: string; narrative: string; ruleTrace: string[]; result?: string | null; }[];
  execution?: { actionId: string; status: string; policyDecision?: string | null; executionMode: string; providerResourceId?: string | null; executedAt?: string | null; } | null;
  reconciliation?: { outcomeId: string; status: string; recoveredAmountMinor: number; providerConfirmed: boolean; confirmationSource?: string | null; sourceEventId?: string | null; occurredAt: string; } | null;
  finalOutcome: string; completeness: { presentStages: number; totalStages: number; missingStages: string[]; };
}
export interface AuditEntry { eventId: string; timestamp: string; actor: string; stage: string; narrative: string; confidence?: number | null; evidence: string[]; ruleTrace: string[]; policyResult?: string | null; externalResourceId?: string | null; }
export interface Approval { actionId: string; incidentId: string; incidentType: string; amountMinor: number; confidence: number; reason: string; failedPolicyRules: string[]; }
export interface Metrics { revenueAtRiskMinor: number; attemptedRecoveryMinor: number; recoveredRevenueMinor: number; recoveryRate: number; activeIncidents: number; strategyPerformance: { strategy: string; recoveredMinor: number; attemptedMinor: number; rate: number; }[]; }
export interface FinancialAttribution { label: string; failedValueMinor: number; policyOrProviderIneligibleMinor: number; addressableValueMinor: number; expectedNaturalRecoveryMinor: number; naturalRecoveryEstimationStatus: string; expectedIncrementalOpportunityMinor: number; executedValueMinor: number; providerConfirmedRecoveryMinor: number; unreconciledExecutedValueMinor: number; attributedIncrementalRecoveryMinor: number; recoveryCostMinor: number; recoveryCostStatus: string; netIncrementalValueMinor: number; timings: { ttd: TimingMetric; tgd: TimingMetric; tte: TimingMetric; ttr: TimingMetric; }; }
export interface LostRevenueExplorer { label: string; revenueAtRiskMinor: number; providerConfirmedRecoveryMinor: number; unrecoveredMinor: number; reasons: { category: string; amountMinor: number; incidentCount: number; evidenceClass: string; explanation: string; }[]; evidenceQuality: string; limitations: string[]; }
export interface TimingMetric { averageMillis?: number | null; samples: number; definition: string; }
export interface ControlTower {
  scopeLabel: string; generatedAt: string;
  paymentHealth: {
    merchantId: string; evaluatedAt: string;
    current: Record<string, { volume: number; failures: number; amountAtRiskMinor: number; successRate: number; failureVelocityPerMinute: number; methodSuccessRates: Record<string, number>; }>;
    baseline: Record<string, { volume: number; failures: number; amountAtRiskMinor: number; successRate: number; failureVelocityPerMinute: number; methodSuccessRates: Record<string, number>; }>;
    signals: { type: string; active: boolean; actual: number; baseline: number; threshold: number; scope: string; evidence: string[]; }[];
  };
  financialAttribution: FinancialAttribution;
  systemicIncidents: { id: string; status: string; scope: string; rootCauses: { cause: string; confidence: number; scope: string; support: string[]; contradiction: string[]; }[]; createdAt: string; }[];
  opportunities: { decisionId: string; incidentId: string; maturity: string; mode: string; selectedAction: string; fallbackStrategy?: string | null; priorityScore?: number | null; policyState?: string | null; governorState?: string | null; netIncrementalValueMinor?: number | null; createdAt: string; }[];
  governor: { killSwitches: Record<string, boolean>; maxTotalValueMinor: number; maxValuePerIncidentMinor: number; maxIncidents: number; maxProviderCallsPerMinute: number; maxConcurrentJobs: number; maxUnreconciledValueMinor: number; canarySize: number; requiredReconciledCount: number; batches: { id: string; strategy: string; status: string; incidentCount: number; releasedCount: number; requiredReconciledCount: number; }[]; };
  models: { id: string; name: string; version: string; featureSchemaVersion: string; lifecycle: string; createdAt: string; }[];
  replayAndShadow: { snapshotCount: number; comparisonCount: number; criticalRegressionCount: number; latestDifferences: { id: string; productionAction: string; shadowAction: string; productionPolicy: string; shadowPolicy: string; productionGovernor: string; shadowGovernor: string; rankingChanged: boolean; approvalRequirementChanged: boolean; criticalRegression: boolean; explanation: string; createdAt: string; }[]; };
  promises: { total: number; byStatus: Record<string, number>; promisedAmountMinor: number; fulfilledAmountMinor: number; };
  truthLabels: string[];
}
export type FailureLabMode = "REAL_RAZORPAY_TEST_MODE" | "FAULT_INJECTION" | "SYNTHETIC_BENCHMARK" | "SHADOW_ONLY";
export interface FailureLabScenario { id: string; title: string; description: string; mode: FailureLabMode; expectedSafetyOutcome: string; evidenceSelector: string; runnable: boolean; }
export interface FailureLabResult { scenario: FailureLabScenario; status: string; safetyDemonstrationPassed: boolean; observedBehavior: string; evidence: string[]; evaluatedAt: string; }
export interface PlanningResult { planId: string; actionId: string; incidentId: string; strategy: string; policyDecision: string; actionStatus: string; incidentStatus: string; ruleTrace: { rule: string; passed: boolean; actual: string; threshold: string }[]; reason: string; }
export interface ExecutionResult { incidentId: string; actionId: string; actionStatus: string; providerId: string; referenceId: string; providerStatus: string; shortUrl?: string; existing: boolean; mode: string; message: string; }

export interface EvaluationScore { numerator: number; denominator: number; value: number; }
export interface EvaluationScenarioResult { scenarioId: string; category: string; expectedIncident: boolean; actualIncident: boolean; expectedRootCauseCategory: string; actualRootCauseCategory: string; expectedPolicyDecision: "AUTO" | "HUMAN" | "DENY" | null; actualPolicyDecision: "AUTO" | "HUMAN" | "DENY" | null; approvalRequired: boolean; expectedExecutionBehavior: string; actualExecutionBehavior: string; expectedProviderOutcome: string; actualProviderOutcome: string; expectedFinancialMutationMinor: number; actualFinancialMutationMinor: number; passed: boolean; auditEvents: string[]; logicalLatencyMillis: Record<string, number>; }
export interface EvaluationReport {
  title: string; scopeLabel: string; reportVersion: string; seed: number; datasetSize: number; deterministicTimestamp: string;
  detectionPrecision: EvaluationScore; detectionRecall: EvaluationScore; detectionF1: EvaluationScore; rootCauseExactAccuracy: EvaluationScore; rootCauseCategoryAccuracy: EvaluationScore; policyCompliance: EvaluationScore; executionEligibilityAccuracy: EvaluationScore; falsePositiveRate: EvaluationScore; falseInterventionRate: EvaluationScore; escalationRate: EvaluationScore; recoveryAttemptRate: EvaluationScore; verifiedRecoveryRate: EvaluationScore;
  detectionConfusionMatrix: { truePositive: number; falsePositive: number; falseNegative: number; trueNegative: number; };
  recoveryFunnel: { amountAtRiskMinor: number; detectedIncidents: number; policyEligible: number; attempted: number; verifiedRecovered: number; };
  recoveredAmountMinor: number; duplicateActionsCreated: number; duplicateFinancialEffects: number;
  strategyPerformance: { strategy: string; sampleCount: number; attemptedCount: number; recoveredCount: number; attemptedAmountMinor: number; recoveredAmountMinor: number; recoveryRate: number; }[];
  latencyMillis: Record<string, { sampleCount: number; p50: number; p95: number; measurementMode: string; }>;
  safetyGates: { gate: string; actual: number; required: string; passed: boolean; evidence: string; }[];
  scenarios: EvaluationScenarioResult[];
  failureInjectionMatrix: { failure: string; scenarioCount: number; observedBehavior: string; bounded: boolean; evidence: string; }[];
  metricDefinitions: { metric: string; formula: string; numerator: number; denominator: number; evidence: string; }[];
  limitations: string[];
}

export interface RecoveryOlympicsReport {
  title: string; truthLabel: string; datasetVersion: string; seed: number; datasetSize: number;
  frozenSplit: Record<"DEVELOPMENT" | "HELD_OUT" | "ADVERSARIAL", number>;
  arms: { arm: string; label: string; methodologyLabel: string; sampleCount: number; interventions: number; refusals: number; noActions: number; grossRecoveryMinor: number; naturalRecoveryMinor: number; incrementalRecoveryMinor: number; incrementalRecoveryRate: { value: number; lower95: number; upper95: number; numerator: number; denominator: number; method: string; }; recoveryCostMinor: number; netIncrementalValueMinor: number; meanTimeToRecoveryMinutes: number; customerContactRate: number; humanEscalationRate: number; falseInterventionRate: number; unnecessaryInterventionRate: number; duplicateFinancialEffects: number; unsafeExecutions: number; policyViolations: number; auditCompleteness: number; decisionLatencyMillis: { p50: number; p95: number; p99: number; measurementMode: string; }; }[];
  integrityRules: string[]; simulatorAssumptions: string[]; limitations: string[];
}

export interface HistoricalValidationCaseResult {
  caseId: string; sourceClass: string; sourceTitle: string; sourceDate: string; sourceUrl: string; productSurface: string; paymentRail: string; providerState: string; normalizedFailureClass: string; normalizedFailureReason: string; expectedBehaviorClass: string; outcomeKnown: boolean; expectedInvariants: string[]; observedInvariants: string[]; result: "PASS" | "PARTIAL" | "FAIL"; safeRefusal: boolean; unexpectedExecution: boolean; unverifiedRecoveryClaim: boolean; duplicateFinancialEffect: boolean; traceComplete: boolean; logicalLatencyMillis: number; policyDisposition: string; evidenceLabel: string;
}

export interface HistoricalValidationReport {
  title: string; truthLabel: string; corpusVersion: string; manifestSha256: string; acceptedPublicSourceCases: number; derivedReplayCount: number; oldestSourceDate: string; newestSourceDate: string; sourceComposition: Record<string, number>; passed: number; partial: number; failed: number; safeRefusals: number; unsafeExecutions: number; duplicateFinancialEffects: number; unverifiedRecoveryClaims: number; decisionTraceCompleteness: number; replayDeterminismRate: number; cases: HistoricalValidationCaseResult[]; limitations: string[];
}

export interface DecisionCertificate {
  id: string; decisionId: string; incidentId: string; recoveryActionId?: string | null; decisionType: string; policyVersion: string; modelVersion: string; featureSchemaVersion: string; strategyVersion: string; inputSnapshotHash: string; evidenceCapsuleHash?: string | null; candidateActions: string[]; rejectedAlternatives: string[]; selectedAction: string; counterfactualMethod: string; evidenceQuality: string; expectedIncrementalValueMinor?: number | null; authorizationResult: string; exposureDecision: string; finalTruthState: string; certificateVersion: string; certificateSha256: string; createdAt: string;
}

export interface ActionMarketplace { version: string; actions: RecoveryActionDefinition[]; }
export interface RecoveryActionDefinition { action: string; version: string; providerCapability: string; eligibleFailureClasses: string[]; eligiblePaymentRails: string[]; minimumConfidence: number; maximumAmountMinor?: number | null; riskClass: string; executionAdapter: string; verificationMethod: string; compensationStrategy: string; materiallyExecutable: boolean; }
export interface ProbabilityInterval { lower: number; upper: number; method: string; }
export interface CounterfactualEstimate { action: string; maturity: string; naturalRecoveryProbability?: number | null; actionRecoveryProbability?: number | null; estimatedIncrementalRecoveryMinor?: number | null; estimatedDirectCostMinor?: number | null; estimatedCustomerCostMinor?: number | null; estimatedRiskCostMinor?: number | null; estimatedNetIncrementalValueMinor?: number | null; naturalRecoveryInterval?: ProbabilityInterval | null; actionRecoveryInterval?: ProbabilityInterval | null; method: string; evidenceQuality: string; modelVersion: string; naturalSampleSize: number; actionSampleSize: number; dataWindowStart?: string | null; dataWindowEnd?: string | null; availableAtDecisionTime: boolean; }
export interface TimingRecommendation { action: string; recommendedAt: string; channel: string; providerWindow: string; customerEligibility: string; authorityState: string; evidenceQuality: string; method: string; explanation: string[]; }
export interface RecoveryCostEntry { id: string; incidentId: string; recoveryActionId?: string | null; decisionId?: string | null; costCategory: string; amountMinor: number; currency: string; source: string; calculationMethod: string; evidenceQuality: string; costVersion: string; occurredAt: string; createdAt: string; }
export interface CustomerRecoveryProfile { incidentId: string; customerCount: number; paymentSamples: number; successfulPayments: number; preferredPaymentRail: string; failureClasses: Record<string, number>; successfulUtcHours: Record<string, number>; priorInteractions: number; simulatedInteractions: number; promisesObserved: number; promisesKept: number; promiseReliability: number; evidenceQuality: string; featureSchemaVersion: string; featureDefinitions: { name: string; source: string; purpose: string; retention: string; allowedUse: string; }[]; }
export interface LlmDiagnostic { provider: "GEMINI"; configured: boolean; model: string; lastInvocation: string; lastResult: string; }
export interface RegisteredModel { id: string; modelName: string; modelVersion: string; featureSchemaVersion: string; lifecycle: string; createdAt: string; }
export interface PolicyConstitutionPreview { merchantId: string; policyVersion: string; compilerVersion: string; constitution: unknown; constitutionSha256: string; status: string; }
