import { fixtureApprovals, fixtureAudit, fixtureDetail, fixtureIncidents, fixtureMetrics } from "./fixtures";
import { fixtureEvaluation } from "./evaluation-fixture";
import type { Approval, AuditEntry, ControlTower, DecisionCertificate, EvaluationReport, EvidenceCapsule, ExecutionResult, FailureLabResult, FailureLabScenario, FinancialAttribution, HistoricalValidationReport, IncidentDetail, IncidentSummary, LostRevenueExplorer, Metrics, PlanningResult, RecoveryOlympicsReport } from "./types";

const API_URL = process.env.NEXT_PUBLIC_SENTINEL_API_URL ?? "http://localhost:8080";
const USE_FIXTURES = process.env.NEXT_PUBLIC_USE_FIXTURES === "true";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, { ...init, headers: { "Content-Type": "application/json", ...init?.headers } });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error((body as { message?: string }).message ?? `Sentinel API returned ${response.status}`);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

const delay = <T>(value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), 180));

const fixtureFailureLab: FailureLabScenario[] = [
  ["duplicate-webhook", "Duplicate webhook", "At-least-once delivery produces one financial effect.", "FAULT_INJECTION", "IDEMPOTENT_ACK"],
  ["out-of-order-webhook", "Out-of-order webhook", "Monotonic reconciliation rejects state regression.", "FAULT_INJECTION", "MONOTONIC_STATE"],
  ["invalid-signature", "Invalid signature", "Unverified provider input cannot mutate revenue truth.", "FAULT_INJECTION", "HTTP_400_NO_MUTATION"],
  ["provider-timeout", "Provider timeout", "Timeout remains uncertain and bounded.", "FAULT_INJECTION", "EXECUTION_UNCERTAIN"],
  ["provider-5xx", "Provider 5xx", "Retry remains bounded with no financial mutation.", "FAULT_INJECTION", "BOUNDED_RETRY"],
  ["payment-downtime", "Payment downtime", "Degraded instruments route to wait or alternate recovery.", "SYNTHETIC_BENCHMARK", "WAIT_OR_ALTERNATE"],
  ["systemic-upi-degradation", "Systemic UPI / bank degradation", "Correlates merchant-wide failure evidence.", "SYNTHETIC_BENCHMARK", "SYSTEMIC_INCIDENT"],
  ["policy-deny", "Policy DENY", "Correct refusal is a successful safety result.", "SYNTHETIC_BENCHMARK", "DENY"],
  ["blast-radius-denial", "Blast-radius denial", "Execution envelope stops an otherwise eligible action.", "SYNTHETIC_BENCHMARK", "GOVERNOR_DENY"],
  ["kill-switch", "Kill switch", "Autonomous execution can be stopped independently.", "SYNTHETIC_BENCHMARK", "STOPPED"],
  ["canary-held", "Canary held", "No expansion before confirmed reconciliation.", "SYNTHETIC_BENCHMARK", "CANARY_NOT_EXPANDED"],
  ["communication-blocked", "Communication blocked", "Consent, DNC, and quiet hours remain authoritative.", "SYNTHETIC_BENCHMARK", "CONTACT_DENIED"],
  ["unsupported-action", "Unsupported recovery action", "Provider capability registry refuses the tool path.", "SYNTHETIC_BENCHMARK", "NO_ACTION"],
  ["ambiguous-acceptance", "Ambiguous provider acceptance", "Unique reference lookup without a recovery claim.", "FAULT_INJECTION", "AWAITING_RECONCILIATION"],
  ["accepted-not-recovered", "Provider accepted, not recovered", "Acceptance remains separate from confirmed money.", "REAL_RAZORPAY_TEST_MODE", "AWAITING_RECONCILIATION"],
  ["challenger-loses", "Challenger loses to champion", "Regression evidence blocks promotion.", "SHADOW_ONLY", "PROMOTION_BLOCKED"],
  ["evidence-refusal", "Evidence Capsule refusal", "Shows why Sentinel safely refused.", "SYNTHETIC_BENCHMARK", "REFUSAL_EXPLAINED"],
].map(([id, title, description, mode, expectedSafetyOutcome]) => ({ id, title, description, mode: mode as FailureLabScenario["mode"], expectedSafetyOutcome, evidenceSelector: "fixture", runnable: id !== "accepted-not-recovered" }));

export const api = {
  failureLabScenarios: () => USE_FIXTURES ? delay(fixtureFailureLab) : request<FailureLabScenario[]>("/api/v1/failure-lab/scenarios"),
  runFailureLab: (id: string) => USE_FIXTURES
    ? delay<FailureLabResult>({ scenario: fixtureFailureLab.find((item) => item.id === id)!, status: id === "accepted-not-recovered" ? "REQUIRES_REAL_PROVIDER_EVENT" : "EVIDENCED", safetyDemonstrationPassed: id !== "accepted-not-recovered", observedBehavior: id === "accepted-not-recovered" ? "No outcome was synthesized. A signed provider event is required." : "The deterministic fixture harness contains the expected bounded or refusal behavior.", evidence: ["SYNTHETIC BENCHMARK", "No real provider outcome claimed"], evaluatedAt: new Date().toISOString() })
    : request<FailureLabResult>(`/api/v1/failure-lab/scenarios/${id}/run`, { method: "POST" }),
  controlTower: async (): Promise<ControlTower> => {
    if (!USE_FIXTURES) return request<ControlTower>("/api/v1/revenue/control-tower");
    const now = new Date().toISOString();
    const financialAttribution = await api.financialAttribution();
    return delay({
      scopeLabel: "RAZORPAY TEST MODE / SYNTHETIC EVALUATION", generatedAt: now,
      paymentHealth: { merchantId: "FIXTURE", evaluatedAt: now,
        current: { "15m": { volume: 46, failures: 18, amountAtRiskMinor: fixtureMetrics.revenueAtRiskMinor, successRate: 0.609, failureVelocityPerMinute: 1.2, methodSuccessRates: { UPI: 0.31, CARD: 0.92 } } },
        baseline: { "24h": { volume: 300, failures: 34, amountAtRiskMinor: fixtureMetrics.revenueAtRiskMinor, successRate: 0.887, failureVelocityPerMinute: 0.024, methodSuccessRates: { UPI: 0.89, CARD: 0.91 } } },
        signals: [{ type: "PAYMENT_METHOD_DEGRADATION", active: true, actual: 0.31, baseline: 0.89, threshold: 0.15, scope: "UPI", evidence: ["UPI success rate dropped from 89% to 31%"] }] },
      financialAttribution, systemicIncidents: [],
      opportunities: fixtureIncidents.slice(0, 3).map((incident, index) => ({ decisionId: `fixture-opportunity-${index}`, incidentId: incident.incidentId, maturity: "M0", mode: "SHADOW_ONLY", selectedAction: incident.strategy ?? "NO_ACTION", fallbackStrategy: incident.strategy, priorityScore: 0.82 - index * 0.1, policyState: incident.policyDecision ?? "PENDING", governorState: "EVALUATION_ONLY", netIncrementalValueMinor: incident.amountAtRiskMinor, createdAt: now })),
      governor: { killSwitches: { ALL_AUTONOMOUS_EXECUTION: false, PAYMENT_LINK_CREATION: false, NEW_ORDER_CREATION: false, CUSTOMER_OUTREACH: false, MODEL_DRIVEN_RANKING: false }, maxTotalValueMinor: 10000000, maxValuePerIncidentMinor: 1000000, maxIncidents: 25, maxProviderCallsPerMinute: 30, maxConcurrentJobs: 4, maxUnreconciledValueMinor: 2500000, canarySize: 3, requiredReconciledCount: 2, batches: [] },
      models: [{ id: "fixture-model", name: "opportunity-ranking", version: "baseline-v1", featureSchemaVersion: "opportunity-v1", lifecycle: "SHADOW", createdAt: now }],
      replayAndShadow: { snapshotCount: 2, comparisonCount: 2, criticalRegressionCount: 0, latestDifferences: [] },
      promises: { total: 0, byStatus: {}, promisedAmountMinor: 0, fulfilledAmountMinor: 0 },
      truthLabels: ["RAZORPAY TEST MODE", "SIMULATION", "FAULT INJECTION", "SYNTHETIC BENCHMARK", "SHADOW ONLY", "PROVIDER CONFIRMED", "AWAITING RECONCILIATION"],
    });
  },
  incidents: () => USE_FIXTURES ? delay(fixtureIncidents) : request<IncidentSummary[]>("/api/v1/revenue/incidents"),
  incident: (id: string) => USE_FIXTURES ? delay({ ...fixtureDetail, incident: fixtureIncidents.find((item) => item.incidentId === id) ?? fixtureDetail.incident }) : request<IncidentDetail>(`/api/v1/revenue/incidents/${id}`),
  approvals: () => USE_FIXTURES ? delay(fixtureApprovals) : request<Approval[]>("/api/v1/revenue/approvals"),
  metrics: async () => {
    if (USE_FIXTURES) return delay(fixtureMetrics);
    const [raw, incidents] = await Promise.all([request<Omit<Metrics, "activeIncidents" | "strategyPerformance"> & { strategyPerformance: { strategy: string; attemptedRecoveryMinor: number; recoveredRevenueMinor: number; recoveryRate: number; }[] }>("/api/v1/revenue/metrics"), request<IncidentSummary[]>("/api/v1/revenue/incidents")]);
    return { ...raw, activeIncidents: incidents.filter((item) => !["RECOVERED", "FAILED", "STOPPED"].includes(item.status)).length, strategyPerformance: raw.strategyPerformance.map((item) => ({ strategy: item.strategy.replaceAll("_", " "), attemptedMinor: item.attemptedRecoveryMinor, recoveredMinor: item.recoveredRevenueMinor, rate: item.recoveryRate })) } as Metrics;
  },
  audit: (id: string) => USE_FIXTURES ? delay(fixtureAudit) : request<AuditEntry[]>(`/api/v1/revenue/incidents/${id}/audit-trail`),
  capsule: async (id: string): Promise<EvidenceCapsule> => {
    if (!USE_FIXTURES) return request<EvidenceCapsule>(`/api/v1/revenue/incidents/${id}/evidence-capsule`);
    return delay({
      incidentId: id, assembledAt: new Date().toISOString(), webhooks: [],
      providerTruth: fixtureDetail.truth ?? { stage: "PROPOSED", executionMode: "SYNTHETIC_BENCHMARK", providerAccepted: false, awaitingReconciliation: false, providerConfirmed: false, providerConfirmedAmountMinor: 0, basis: "Fixture evidence only" },
      systemicEvidence: fixtureDetail.findings.map((finding, index) => ({ evidenceId: `fixture-${index}`, source: finding.source, summary: finding.summary, confidence: finding.confidence, capturedAt: finding.createdAt, fresh: true })),
      agentClaims: [], prediction: fixtureDetail.findings.find((finding) => finding.source === "ROOT_CAUSE_AGENT")?.summary,
      policy: [], execution: null, reconciliation: null, finalOutcome: fixtureDetail.incident.latestOutcome ?? "NOT_PROVIDER_CONFIRMED",
      completeness: { presentStages: 3, totalStages: 9, missingStages: ["WEBHOOK", "AGENT_CLAIMS", "POLICY", "EXECUTION", "RECONCILIATION", "FINAL_OUTCOME"] },
    });
  },
  financialAttribution: async (): Promise<FinancialAttribution> => {
    if (!USE_FIXTURES) return request<FinancialAttribution>("/api/v1/revenue/financial-attribution");
    const failed = fixtureMetrics.revenueAtRiskMinor;
    const executed = fixtureMetrics.attemptedRecoveryMinor;
    const confirmed = fixtureMetrics.recoveredRevenueMinor;
    return delay({ label: "Financial Attribution — Test Mode / Synthetic Evaluation", failedValueMinor: failed,
      policyOrProviderIneligibleMinor: 0, addressableValueMinor: failed, expectedNaturalRecoveryMinor: 0,
      naturalRecoveryEstimationStatus: "NOT_ESTIMATED_NO_CAUSAL_BASELINE", expectedIncrementalOpportunityMinor: failed,
      executedValueMinor: executed, providerConfirmedRecoveryMinor: confirmed,
      unreconciledExecutedValueMinor: Math.max(0, executed - confirmed), attributedIncrementalRecoveryMinor: confirmed,
      recoveryCostMinor: 0, recoveryCostStatus: "NOT_CONFIGURED", netIncrementalValueMinor: confirmed,
      timings: { ttd: { averageMillis: null, samples: 0, definition: "first failed payment event → incident detected" },
        tgd: { averageMillis: null, samples: 0, definition: "incident detected → grounded diagnosis" },
        tte: { averageMillis: null, samples: 0, definition: "incident detected → provider execution accepted" },
        ttr: { averageMillis: null, samples: 0, definition: "incident detected → provider-confirmed reconciliation" } } });
  },
  lostRevenue: (): Promise<LostRevenueExplorer> => USE_FIXTURES ? delay({ label: "Lost Revenue Explorer — fixture evidence", revenueAtRiskMinor: fixtureMetrics.revenueAtRiskMinor, providerConfirmedRecoveryMinor: fixtureMetrics.recoveredRevenueMinor, unrecoveredMinor: Math.max(0, fixtureMetrics.revenueAtRiskMinor - fixtureMetrics.recoveredRevenueMinor), reasons: [{ category: "UNRESOLVED_OR_NO_ACTION", amountMinor: Math.max(0, fixtureMetrics.revenueAtRiskMinor - fixtureMetrics.recoveredRevenueMinor), incidentCount: fixtureIncidents.filter((item) => item.status !== "RECOVERED").length, evidenceClass: "SYNTHETIC_FIXTURE_STATE", explanation: "Fixture incidents remain unresolved; no provider-confirmed claim is inferred." }], evidenceQuality: "SYNTHETIC_FIXTURE_STATE", limitations: ["Fixture values are synthetic and do not estimate causal uplift."] }) : request<LostRevenueExplorer>("/api/v1/revenue/lost-revenue"),
  investigate: (id: string) => request(`/api/v1/revenue/incidents/${id}/investigate`, { method: "POST" }),
  plan: (id: string) => request<PlanningResult>(`/api/v1/revenue/incidents/${id}/plan`, { method: "POST" }),
  execute: (incidentId: string) => request<ExecutionResult>(`/api/v1/revenue/incidents/${incidentId}/execute`, { method: "POST" }),
  decide: (actionId: string, decision: "approve" | "reject", actor: string, reason: string) => request(`/api/v1/revenue/actions/${actionId}/${decision}`, { method: "POST", body: JSON.stringify({ actor, reason }) }),
  reset: () => USE_FIXTURES ? delay({ reset: true }) : request("/api/v1/demo/reset", { method: "POST" }),
  inject: () => USE_FIXTURES ? delay({ incidentIds: [fixtureIncidents[0].incidentId] }) : request<{ incidentIds?: string[] }>("/api/v1/demo/inject/upi-outage", { method: "POST" }),
  evaluation: () => USE_FIXTURES ? delay(fixtureEvaluation) : request<EvaluationReport>("/api/v1/evaluation/report"),
  runEvaluation: () => USE_FIXTURES ? delay(fixtureEvaluation) : request<EvaluationReport>("/api/v1/evaluation/run", { method: "POST" }),
  recoveryOlympics: () => USE_FIXTURES ? delay<RecoveryOlympicsReport>({ title: "Recovery Olympics", truthLabel: "SYNTHETIC / CONTROLLED BENCHMARK", datasetVersion: "requires-live-evaluation-api", seed: 20260901, datasetSize: 0, frozenSplit: { DEVELOPMENT: 0, HELD_OUT: 0, ADVERSARIAL: 0 }, arms: [], integrityRules: [], simulatorAssumptions: [], limitations: ["Connect the evaluation API to generate benchmark results; no numbers are fabricated in fixture mode."] }) : request<RecoveryOlympicsReport>("/api/v1/evaluation/recovery-olympics"),
  historicalValidation: () => USE_FIXTURES ? delay<HistoricalValidationReport>({ title: "Razorpay Historical Validation", truthLabel: "PUBLIC-SOURCE HISTORICAL VALIDATION", corpusVersion: "requires-live-evaluation-api", manifestSha256: "UNAVAILABLE_IN_FIXTURE_MODE", acceptedPublicSourceCases: 0, derivedReplayCount: 0, oldestSourceDate: "UNKNOWN", newestSourceDate: "UNKNOWN", sourceComposition: {}, passed: 0, partial: 0, failed: 0, safeRefusals: 0, unsafeExecutions: 0, duplicateFinancialEffects: 0, unverifiedRecoveryClaims: 0, decisionTraceCompleteness: 0, replayDeterminismRate: 0, cases: [], limitations: ["Connect the live evaluation API to load the frozen public-source corpus; fixture mode does not invent historical cases."] }) : request<HistoricalValidationReport>("/api/v1/evaluation/historical"),
  decisionCertificates: (incidentId: string) => USE_FIXTURES ? delay<DecisionCertificate[]>([]) : request<DecisionCertificate[]>(`/api/v1/revenue/incidents/${incidentId}/decision-certificates`),
  evaluationDownloadUrl: (format: "json" | "md") => `${API_URL}/api/v1/evaluation/report.${format}`,
};

export const money = (minor: number) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", minimumFractionDigits: 2 }).format(minor / 100);
export const updatedAt = (date = new Date()) => date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
export const shortId = (id: string) => id.slice(0, 8).toUpperCase();
