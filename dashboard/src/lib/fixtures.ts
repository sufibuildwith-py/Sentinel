import type { Approval, AuditEntry, IncidentDetail, IncidentSummary, Metrics } from "./types";

export const demoIncidentId = "7b47c4ee-5c22-43c4-8f0c-831168683bbe";
const now = new Date().toISOString();

export const fixtureIncidents: IncidentSummary[] = [
  { incidentId: demoIncidentId, type: "UPI_DEGRADATION", status: "RECOVERED", severity: "HIGH", amountAtRiskMinor: 284000, detectedAt: now, affectedPaymentCount: 41, affectedCustomerCount: 38, strategy: "ALTERNATIVE_PAYMENT_LINK", policyDecision: "AUTO", actionStatus: "RECOVERED", latestOutcome: "RECOVERED", recoveredAmountMinor: 72300 },
  { incidentId: "5c35f128-e667-46a1-a7f3-08941e64f673", type: "PROVIDER_OUTAGE", status: "HUMAN_REVIEW", severity: "HIGH", amountAtRiskMinor: 168500, detectedAt: new Date(Date.now() - 22 * 60_000).toISOString(), affectedPaymentCount: 27, affectedCustomerCount: 25, strategy: "DEFERRED_RETRY", policyDecision: "HUMAN", actionStatus: "PENDING_APPROVAL", latestOutcome: null, recoveredAmountMinor: 0 },
  { incidentId: "110d695b-fcb9-433a-8e7b-fea6af6ce416", type: "NORMAL_FAILURE_MIX", status: "STOPPED", severity: "LOW", amountAtRiskMinor: 42000, detectedAt: new Date(Date.now() - 75 * 60_000).toISOString(), affectedPaymentCount: 9, affectedCustomerCount: 9, strategy: "NO_ACTION", policyDecision: "DENY", actionStatus: "STOPPED", latestOutcome: "STOPPED", recoveredAmountMinor: 0 },
];

export const fixtureAudit: AuditEntry[] = [
  { eventId: "evt-1", timestamp: new Date(Date.now() - 34 * 60_000).toISOString(), actor: "detector", stage: "DETECT", narrative: "UPI success rate fell 31.4 points below the rolling baseline.", confidence: .94, evidence: ["actual 51.2% vs baseline 82.6%", "41 affected payments"], ruleTrace: [], policyResult: null, externalResourceId: null },
  { eventId: "evt-2", timestamp: new Date(Date.now() - 29 * 60_000).toISOString(), actor: "root-cause-agent", stage: "INVESTIGATE", narrative: "UPI issuer degradation diagnosed with 91% confidence.", confidence: .91, evidence: ["73% of failures are UPI", "61% involve Bank X"], ruleTrace: [], policyResult: null, externalResourceId: null },
  { eventId: "evt-3", timestamp: new Date(Date.now() - 24 * 60_000).toISOString(), actor: "policy-engine", stage: "POLICY", narrative: "All mandatory stop rules passed; low-value recovery auto-approved.", confidence: .91, evidence: [], ruleTrace: ["PASS payment is not already recovered", "PASS no duplicate-charge risk", "PASS amount ₹1,000 ≤ threshold ₹1,000"], policyResult: "AUTO", externalResourceId: null },
  { eventId: "evt-4", timestamp: new Date(Date.now() - 18 * 60_000).toISOString(), actor: "razorpay-gateway", stage: "EXECUTE", narrative: "One Test Mode Payment Link created with UPI hidden.", confidence: null, evidence: [], ruleTrace: [], policyResult: "AUTO", externalResourceId: "plink_test_sentinel" },
  { eventId: "evt-5", timestamp: new Date(Date.now() - 4 * 60_000).toISOString(), actor: "webhook-processor", stage: "OBSERVE", narrative: "Signed payment_link.paid webhook verified and applied exactly once.", confidence: null, evidence: ["duplicate=false", "recovered ₹723.00"], ruleTrace: [], policyResult: null, externalResourceId: "plink_test_sentinel" },
];

export const fixtureDetail: IncidentDetail = {
  incident: fixtureIncidents[0],
  findings: [
    { source: "DETECTOR", summary: "UPI degradation exceeded all configured detection thresholds.", confidence: 0.94, evidence: ["UPI success rate: 51.2% vs 82.6% baseline", "₹2,840.00 at risk across 41 attempts", "31.4 percentage-point deviation"], createdAt: fixtureAudit[0].timestamp },
    { source: "PAYMENT_ANALYST", summary: "Failures concentrate in UPI transactions from one issuer cohort.", confidence: 0.88, evidence: ["73% of failed attempts use UPI", "61% of UPI failures involve Bank X", "Retry frequency increased 2.4×"], createdAt: fixtureAudit[1].timestamp },
    { source: "ROOT_CAUSE_AGENT", summary: "UPI issuer degradation is the most likely root cause.", confidence: 0.91, evidence: ["Detection and issuer concentration agree", "14 similar incidents recovered 72.3% of value with alternative links"], createdAt: fixtureAudit[1].timestamp },
  ],
  plan: { planId: "6e9c37d4-4e35-4222-a165-f025598395e2", strategy: "ALTERNATIVE_PAYMENT_LINK", reason: "Offer a card/netbanking link while UPI is degraded.", targetAmountMinor: 100000, confidence: 0.91, riskLevel: "LOW" },
  action: { actionId: "3a6cff21-d9a8-46e1-a071-c579f328f1c2", status: "RECOVERED", policyDecision: "AUTO", amountMinor: 100000, currency: "INR", providerId: "plink_test_sentinel", referenceId: "sentinel-ref-test", providerStatus: "paid", shortUrl: "https://rzp.io/i/sentinel-test", executionAttempts: 1, approvedAt: fixtureAudit[2].timestamp, executedAt: fixtureAudit[3].timestamp },
  governor: { decisionId: "fixture-governor", allowed: true, allowedValueMinor: 100000, violations: [], evaluatedAt: fixtureAudit[3].timestamp },
  truth: { stage: "RECOVERED_CONFIRMED", executionMode: "RAZORPAY_TEST_MODE", providerAccepted: true, awaitingReconciliation: false, providerConfirmed: true, providerConfirmedAmountMinor: 72300, basis: "Signed fixture webhook evidence applied exactly once" },
};

export const fixtureApprovals: Approval[] = [{ actionId: "7866d3a6-66cf-4af6-aa32-ee18332355d4", incidentId: fixtureIncidents[1].incidentId, incidentType: "PROVIDER_OUTAGE", amountMinor: 168500, confidence: 0.71, reason: "Amount exceeds the automatic action threshold.", failedPolicyRules: ["confidence 0.71 is below auto threshold 0.85", "₹1,685.00 exceeds auto threshold ₹1,000.00"] }];

const providerOutageAudit: AuditEntry[] = [
  { eventId: "provider-outage-policy", timestamp: fixtureIncidents[1].detectedAt, actor: "policy-engine", stage: "POLICY_DECISION", narrative: "Recovery requires explicit human review before any provider action.", confidence: .71, evidence: [], ruleTrace: fixtureApprovals[0].failedPolicyRules, policyResult: "HUMAN", externalResourceId: null },
];

const normalFailureAudit: AuditEntry[] = [
  { eventId: "normal-failure-policy", timestamp: fixtureIncidents[2].detectedAt, actor: "policy-engine", stage: "POLICY_DECISION", narrative: "Normal failure mix is not eligible for recovery intervention.", confidence: null, evidence: [], ruleTrace: ["DENY no qualifying anomaly"], policyResult: "DENY", externalResourceId: null },
];

export const fixtureIncidentDetails: Record<string, IncidentDetail> = {
  [demoIncidentId]: fixtureDetail,
  [fixtureIncidents[1].incidentId]: {
    incident: fixtureIncidents[1], findings: [],
    plan: { planId: "provider-outage-plan", strategy: "DEFERRED_RETRY", reason: fixtureApprovals[0].reason, targetAmountMinor: fixtureApprovals[0].amountMinor, confidence: fixtureApprovals[0].confidence, riskLevel: "HIGH" },
    action: { actionId: fixtureApprovals[0].actionId, status: "PENDING_APPROVAL", policyDecision: "HUMAN", amountMinor: fixtureApprovals[0].amountMinor, currency: "INR", executionAttempts: 0 },
    governor: null,
    truth: { stage: "POLICY_APPROVED", executionMode: "SYNTHETIC_BENCHMARK", providerAccepted: false, awaitingReconciliation: false, providerConfirmed: false, providerConfirmedAmountMinor: 0, basis: "Human approval is pending; no provider action has been submitted" },
  },
  [fixtureIncidents[2].incidentId]: {
    incident: fixtureIncidents[2], findings: [],
    plan: { planId: "normal-failure-plan", strategy: "NO_ACTION", reason: "No qualifying anomaly is present in the deterministic fixture.", targetAmountMinor: 0, confidence: 1, riskLevel: "LOW" },
    action: { actionId: "normal-failure-action", status: "STOPPED", policyDecision: "DENY", amountMinor: 0, currency: "INR", executionAttempts: 0 },
    governor: null,
    truth: null,
  },
};

export const fixtureAuditByIncidentId: Record<string, AuditEntry[]> = {
  [demoIncidentId]: fixtureAudit,
  [fixtureIncidents[1].incidentId]: providerOutageAudit,
  [fixtureIncidents[2].incidentId]: normalFailureAudit,
};

export const fixtureMetrics: Metrics = { revenueAtRiskMinor: 494500, attemptedRecoveryMinor: 100000, recoveredRevenueMinor: 72300, recoveryRate: 0.723, activeIncidents: 2, strategyPerformance: [
  { strategy: "Alternative link", recoveredMinor: 72300, attemptedMinor: 100000, rate: .723 },
  { strategy: "Deferred retry", recoveredMinor: 41000, attemptedMinor: 76000, rate: .539 },
  { strategy: "Reminder", recoveredMinor: 18500, attemptedMinor: 49000, rate: .378 },
] };
