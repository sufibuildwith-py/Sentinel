export type IncidentStatus = "DETECTED" | "INVESTIGATING" | "DIAGNOSED" | "PLANNING" | "POLICY_REVIEW" | "APPROVED" | "HUMAN_REVIEW" | "EXECUTING" | "MONITORING" | "RECOVERED" | "FAILED" | "STOPPED";

export interface IncidentSummary { incidentId: string; type: string; status: IncidentStatus; severity: string; amountAtRiskMinor: number; detectedAt: string; affectedPaymentCount: number; affectedCustomerCount: number; strategy?: string | null; policyDecision?: "AUTO" | "HUMAN" | "DENY" | null; actionStatus?: string | null; latestOutcome?: string | null; recoveredAmountMinor: number; }
export interface Finding { source: string; summary: string; confidence?: number | null; evidence: string[]; createdAt: string; }
export interface PlanView { planId: string; strategy: string; reason: string; targetAmountMinor: number; confidence: number; riskLevel: string; }
export interface ActionView { actionId: string; status: string; policyDecision?: string | null; amountMinor: number; currency: string; providerId?: string | null; referenceId?: string | null; shortUrl?: string | null; providerStatus?: string | null; executionAttempts: number; approvedAt?: string | null; executedAt?: string | null; }
export interface IncidentDetail { incident: IncidentSummary; findings: Finding[]; plan?: PlanView | null; action?: ActionView | null; }
export interface AuditEntry { eventId: string; timestamp: string; actor: string; stage: string; narrative: string; confidence?: number | null; evidence: string[]; ruleTrace: string[]; policyResult?: string | null; externalResourceId?: string | null; }
export interface Approval { actionId: string; incidentId: string; incidentType: string; amountMinor: number; confidence: number; reason: string; failedPolicyRules: string[]; }
export interface Metrics { revenueAtRiskMinor: number; attemptedRecoveryMinor: number; recoveredRevenueMinor: number; recoveryRate: number; activeIncidents: number; strategyPerformance: { strategy: string; recoveredMinor: number; attemptedMinor: number; rate: number; }[]; }
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
