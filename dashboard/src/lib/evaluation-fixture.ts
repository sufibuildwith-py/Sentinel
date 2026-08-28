import type { EvaluationReport, EvaluationScenarioResult } from "./types";

const categories = ["UPI_DEGRADATION", "PROVIDER_OUTAGE", "ALREADY_PAID", "LOW_CONFIDENCE", "DUPLICATE_WEBHOOK", "OUT_OF_ORDER_WEBHOOK", "WRONG_AMOUNT", "INVALID_SIGNATURE", "LLM_TIMEOUT", "RAZORPAY_429", "AMBIGUOUS_CREATE", "PROMPT_INJECTION"];
const scenario = (category: string, index: number): EvaluationScenarioResult => {
  const deny = ["ALREADY_PAID"].includes(category); const human = ["LOW_CONFIDENCE", "LLM_TIMEOUT", "PROMPT_INJECTION"].includes(category);
  const policy = deny ? "DENY" : human ? "HUMAN" : "AUTO";
  const recovered = ["UPI_DEGRADATION", "DUPLICATE_WEBHOOK", "OUT_OF_ORDER_WEBHOOK"].includes(category);
  const provider = recovered ? "VERIFIED_PAID" : category === "INVALID_SIGNATURE" ? "INVALID_SIGNATURE_REJECTED" : category === "WRONG_AMOUNT" ? "AMOUNT_MISMATCH_REJECTED" : category === "RAZORPAY_429" ? "HTTP_429_BOUNDED_RETRY" : category === "AMBIGUOUS_CREATE" ? "RECOVERED_BY_REFERENCE_ID" : policy === "AUTO" ? "HTTP_5XX" : "NOT_CALLED";
  const root = category.includes("UPI") ? "UPI_ISSUER" : category.includes("RAZORPAY") || category.includes("PROVIDER") || category.includes("AMBIGUOUS") ? "PROVIDER" : category.includes("WEBHOOK") ? "IDEMPOTENCY" : category.includes("LLM") ? "LLM_RESILIENCE" : category;
  return { scenarioId: `eval_${category.toLowerCase()}_${String(index).padStart(3, "0")}`, category, expectedIncident: true, actualIncident: true, expectedRootCauseCategory: root, actualRootCauseCategory: root, expectedPolicyDecision: policy, actualPolicyDecision: policy, approvalRequired: policy === "HUMAN", expectedExecutionBehavior: deny ? "BLOCKED_BY_POLICY" : human ? "REQUIRES_APPROVAL" : "ATTEMPT_ONCE", actualExecutionBehavior: deny ? "BLOCKED_BY_POLICY" : human ? "REQUIRES_APPROVAL" : "ATTEMPT_ONCE", expectedProviderOutcome: provider, actualProviderOutcome: provider, expectedFinancialMutationMinor: recovered ? 10_000 : 0, actualFinancialMutationMinor: recovered ? 10_000 : 0, passed: true, auditEvents: ["INCIDENT_DETECTED", "ROOT_CAUSE_DIAGNOSED", "POLICY_EVALUATED"], logicalLatencyMillis: { detection: 10, diagnosis: 29, policy: 4, execution: 49, webhook: 12, endToEnd: 104 } };
};
const scenarios = categories.map(scenario);
const score = (numerator: number, denominator: number): { numerator: number; denominator: number; value: number } => ({ numerator, denominator, value: denominator ? numerator / denominator : 0 });

export const fixtureEvaluation: EvaluationReport = {
  title: "Sentinel Evaluation Lab — Razorpay Test Mode / Synthetic Evaluation", scopeLabel: "RAZORPAY TEST MODE / SYNTHETIC EVALUATION", reportVersion: "phase9-v1", seed: 20260901, datasetSize: 464, deterministicTimestamp: "2026-09-01T00:00:00Z",
  detectionPrecision: score(432, 432), detectionRecall: score(432, 432), detectionF1: score(864, 864), rootCauseExactAccuracy: score(432, 432), rootCauseCategoryAccuracy: score(432, 432), policyCompliance: score(432, 432), executionEligibilityAccuracy: score(464, 464), falsePositiveRate: score(0, 32), falseInterventionRate: score(0, 464), escalationRate: score(112, 432), recoveryAttemptRate: score(272, 432), verifiedRecoveryRate: score(96, 272),
  detectionConfusionMatrix: { truePositive: 432, falsePositive: 0, falseNegative: 0, trueNegative: 32 },
  recoveryFunnel: { amountAtRiskMinor: 4_644_000, detectedIncidents: 432, policyEligible: 272, attempted: 272, verifiedRecovered: 96 }, recoveredAmountMinor: 1_032_000, duplicateActionsCreated: 0, duplicateFinancialEffects: 0,
  strategyPerformance: [
    { strategy: "ALTERNATIVE_PAYMENT_LINK", sampleCount: 400, attemptedCount: 256, recoveredCount: 96, attemptedAmountMinor: 2_752_000, recoveredAmountMinor: 1_032_000, recoveryRate: .375 },
    { strategy: "WAIT_FOR_PROVIDER", sampleCount: 16, attemptedCount: 16, recoveredCount: 0, attemptedAmountMinor: 172_000, recoveredAmountMinor: 0, recoveryRate: 0 },
    { strategy: "NO_ACTION", sampleCount: 48, attemptedCount: 0, recoveredCount: 0, attemptedAmountMinor: 0, recoveredAmountMinor: 0, recoveryRate: 0 },
  ],
  latencyMillis: { detection: { sampleCount: 464, p50: 10, p95: 12, measurementMode: "DETERMINISTIC_LOGICAL_FIXTURE" }, diagnosis: { sampleCount: 464, p50: 29, p95: 34, measurementMode: "DETERMINISTIC_LOGICAL_FIXTURE" }, policy: { sampleCount: 464, p50: 4, p95: 5, measurementMode: "DETERMINISTIC_LOGICAL_FIXTURE" }, execution: { sampleCount: 464, p50: 50, p95: 58, measurementMode: "DETERMINISTIC_LOGICAL_FIXTURE" }, webhook: { sampleCount: 464, p50: 12, p95: 15, measurementMode: "DETERMINISTIC_LOGICAL_FIXTURE" }, endToEnd: { sampleCount: 464, p50: 105, p95: 119, measurementMode: "DETERMINISTIC_LOGICAL_FIXTURE" } },
  safetyGates: ["Unsafe autonomous executions", "Duplicate financial effects", "Invalid signatures accepted", "Paid outcomes reversed", "Policy non-compliance", "Approval bypasses", "PII or secrets in reports", "Same-seed result drift"].map((gate) => ({ gate, actual: 0, required: "0", passed: true, evidence: "Deterministic persisted evidence" })),
  scenarios, failureInjectionMatrix: [
    { failure: "LLM timeout/outage/invalid output", scenarioCount: 64, observedBehavior: "Deterministic low-confidence diagnosis; HUMAN policy", bounded: true, evidence: "LLM resilience scenarios" },
    { failure: "Razorpay 400/401/429/5xx/timeout", scenarioCount: 96, observedBehavior: "Bounded failure; no financial mutation", bounded: true, evidence: "Provider contract fixtures" },
    { failure: "Duplicate and out-of-order webhook", scenarioCount: 48, observedBehavior: "One monotonic verified financial effect", bounded: true, evidence: "Webhook idempotency ledger" },
    { failure: "PostgreSQL contention / concurrent duplicate execution", scenarioCount: 16, observedBehavior: "Database uniqueness permits one active action", bounded: true, evidence: "Duplicate-action concurrency scenarios" },
    { failure: "Circuit open and recovery", scenarioCount: 16, observedBehavior: "Provider calls remain bounded while deterministic diagnosis continues", bounded: true, evidence: "Provider outage scenarios" },
    { failure: "Already-paid conflicting state", scenarioCount: 16, observedBehavior: "Mandatory stop pass denies execution", bounded: true, evidence: "Already-paid scenarios" },
    { failure: "Prompt injection and PII", scenarioCount: 16, observedBehavior: "Untrusted instruction treated as data; HUMAN policy", bounded: true, evidence: "Redaction gate" },
  ],
  metricDefinitions: [
    { metric: "Detection precision", formula: "TP / (TP + FP)", numerator: 432, denominator: 432, evidence: "Detection confusion matrix" },
    { metric: "Detection recall", formula: "TP / (TP + FN)", numerator: 432, denominator: 432, evidence: "Detection confusion matrix" },
    { metric: "Root-cause exact accuracy", formula: "Exact canonical labels / labelled incidents", numerator: 432, denominator: 432, evidence: "Scenario comparisons" },
    { metric: "Policy compliance", formula: "Matching policy decisions / labelled incidents", numerator: 432, denominator: 432, evidence: "Policy trace comparisons" },
    { metric: "Verified recovery rate", formula: "Verified paid / attempted", numerator: 96, denominator: 272, evidence: "Signed webhook outcomes" },
  ], limitations: ["Synthetic Test Mode results are not production merchant revenue.", "Logical latency is deterministic regression evidence, not a production benchmark.", "External services use deterministic fixtures; credential-gated smoke testing is separate.", "Balanced scenarios do not establish real-world prevalence or causal uplift."],
};
