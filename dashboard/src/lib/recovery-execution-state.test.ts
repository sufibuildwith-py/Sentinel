import { describe, expect, it } from "vitest";
import { derivePipelineStages } from "./pipeline";
import { derivePrimaryRecoveryAction, normalizeRecoveryExecution } from "./recovery-execution-state";
import { nextRecoveryOperation } from "./recovery-session";
import type { AuditEntry, IncidentDetail } from "./types";

const at = "2026-09-01T00:00:00Z";
const event = (eventId: string, stage: string, actor: string, narrative: string, policyResult?: string): AuditEntry => ({
  eventId, timestamp: at, actor, stage, narrative, policyResult, evidence: [], ruleTrace: [],
});

const approvedHuman: IncidentDetail = {
  incident: { incidentId: "incident", type: "PAYMENT_FAILURE_CLUSTER", status: "APPROVED", severity: "HIGH", amountAtRiskMinor: 3600000, detectedAt: at, affectedPaymentCount: 30, affectedCustomerCount: 30, recoveredAmountMinor: 0 },
  findings: [
    { source: "PAYMENT_ANALYST", summary: "Evidence persisted", confidence: 1, evidence: [], createdAt: at },
    { source: "ROOT_CAUSE_AGENT", summary: "Diagnosis persisted", confidence: .6, evidence: [], createdAt: at },
  ],
  plan: { planId: "plan", strategy: "ALTERNATIVE_PAYMENT_LINK", reason: "Degraded UPI", targetAmountMinor: 3600000, confidence: .6, riskLevel: "HIGH" },
  action: { actionId: "action", status: "APPROVED", policyDecision: "HUMAN", amountMinor: 3600000, currency: "INR", executionAttempts: 0, approvedAt: at },
  truth: { stage: "POLICY_APPROVED", executionMode: "RAZORPAY_TEST_MODE", providerAccepted: false, awaitingReconciliation: false, providerConfirmed: false, providerConfirmedAmountMinor: 0, basis: "Policy and human approval persisted" },
};

const approvedAudit = [
  event("policy", "POLICY_DECISION", "POLICY_ENGINE", "Human review required", "HUMAN"),
  event("human", "HUMAN_APPROVED", "ops-reviewer", "Explicit human approval persisted", "HUMAN"),
];

describe("normalized persisted recovery execution state", () => {
  it("places explicit human approval before the governor gate", () => {
    const state = normalizeRecoveryExecution(approvedHuman, approvedAudit);
    const stages = derivePipelineStages(state);
    expect(stages.find((stage) => stage.label === "Governor")?.state).toBe("QUEUED");
    expect(stages.find((stage) => stage.label === "Human")?.state).toBe("COMPLETE");
    expect(state.humanReview.approvalPersisted).toBe(true);
    expect(state.humanReview.reason).toContain("Explicit human approval");
    expect(stages.findIndex((stage) => stage.label === "Human")).toBeLessThan(stages.findIndex((stage) => stage.label === "Governor"));
  });

  it("does not complete a downstream gate without its persisted prerequisite", () => {
    const policyMissing = normalizeRecoveryExecution({ ...approvedHuman, action: null }, [event("g", "BLAST_RADIUS_EVALUATED", "GOVERNOR", "Execution envelope granted", "ALLOW")]);
    const stages = derivePipelineStages(policyMissing);
    expect(stages.find((stage) => stage.label === "Policy")?.state).toBe("QUEUED");
    expect(stages.find((stage) => stage.label === "Governor")?.state).not.toBe("COMPLETE");

    const auto = normalizeRecoveryExecution({ ...approvedHuman, action: { ...approvedHuman.action!, status: "AUTO_APPROVED", policyDecision: "AUTO", approvedAt: null } }, [event("p", "POLICY_DECISION", "POLICY_ENGINE", "Auto", "AUTO")]);
    expect(derivePipelineStages(auto).find((stage) => stage.label === "Human")?.state).toBe("NOT_APPLICABLE");
  });

  it("requires exact provider evidence for execute, accept, and reconciliation completion", () => {
    const state = normalizeRecoveryExecution(approvedHuman, approvedAudit);
    const stages = derivePipelineStages(state);
    expect(stages.find((stage) => stage.label === "Execute")?.state).toBe("QUEUED");
    expect(stages.find((stage) => stage.label === "Accept")?.state).toBe("QUEUED");
    expect(stages.find((stage) => stage.label === "Reconcile")?.state).toBe("QUEUED");
  });

  it("uses refreshed durable governor denial to remove Run Recovery and prevent a second call", () => {
    expect(derivePrimaryRecoveryAction(normalizeRecoveryExecution(approvedHuman, approvedAudit)).operation).toBe("execute");
    const denied: IncidentDetail = {
      ...approvedHuman,
      incident: { ...approvedHuman.incident, status: "STOPPED" },
      action: { ...approvedHuman.action!, status: "STOPPED" },
      governor: { decisionId: "governor", allowed: false, allowedValueMinor: 0, violations: ["MAX_UNRECONCILED_VALUE"], evaluatedAt: at },
    };
    const deniedAudit = [...approvedAudit, event("governor", "BLAST_RADIUS_EVALUATED", "GOVERNOR", "Execution envelope denied", "DENY")];
    const refreshed = normalizeRecoveryExecution(denied, deniedAudit);
    expect(derivePrimaryRecoveryAction(refreshed).kind).toBe("GOVERNOR_BLOCKED");
    expect(nextRecoveryOperation(denied, deniedAudit)).toBeNull();
    expect(derivePipelineStages(refreshed).find((stage) => stage.label === "Governor")?.state).toBe("BLOCKED");
  });

  it("resumes from the first incomplete persisted stage without replaying completed work", () => {
    expect(nextRecoveryOperation(approvedHuman, approvedAudit)).toBe("execute");
    const accepted: IncidentDetail = {
      ...approvedHuman,
      incident: { ...approvedHuman.incident, status: "MONITORING" },
      action: { ...approvedHuman.action!, status: "EXECUTED", providerId: "plink", executedAt: at },
      governor: { decisionId: "governor", allowed: true, allowedValueMinor: 3600000, violations: [], evaluatedAt: at },
      truth: { ...approvedHuman.truth!, stage: "AWAITING_RECONCILIATION", providerAccepted: true, awaitingReconciliation: true },
    };
    expect(nextRecoveryOperation(accepted, approvedAudit)).toBeNull();
    expect(derivePrimaryRecoveryAction(normalizeRecoveryExecution(accepted, approvedAudit)).kind).toBe("AWAITING_RECONCILIATION");
  });

  it("keeps shadow NO_ACTION advisory when authoritative gates permit execution", () => {
    const shadowAudit = [
      ...approvedAudit,
      event("shadow", "SHADOW_OPPORTUNITY_EVALUATED", "OPPORTUNITY_ENGINE", "Shadow choice NO_ACTION; SHADOW_ONLY_NOT_AUTHORIZED"),
    ];
    const normalized = normalizeRecoveryExecution(approvedHuman, shadowAudit);
    expect(normalized.plan.counterfactual).toBe(true);
    expect(derivePrimaryRecoveryAction(normalized)).toMatchObject({ kind: "EXECUTE", operation: "execute", executable: true });

    const denied: IncidentDetail = {
      ...approvedHuman,
      incident: { ...approvedHuman.incident, status: "STOPPED" },
      action: { ...approvedHuman.action!, status: "STOPPED" },
      governor: { decisionId: "deny", allowed: false, allowedValueMinor: 0, violations: ["KILL_SWITCH"], evaluatedAt: at },
    };
    const favorableShadow = [...approvedAudit, event("shadow-allow", "SHADOW_OPPORTUNITY_EVALUATED", "OPPORTUNITY_ENGINE", "Shadow recommends intervention")];
    expect(derivePrimaryRecoveryAction(normalizeRecoveryExecution(denied, favorableShadow))).toMatchObject({ kind: "GOVERNOR_BLOCKED", operation: null });
  });

  it("uses the incident-scoped summary disposition without inventing provider evidence", () => {
    const pending = normalizeRecoveryExecution({
      ...approvedHuman,
      incident: { ...approvedHuman.incident, status: "HUMAN_REVIEW", policyDecision: "HUMAN" },
      findings: [], plan: null, action: null, governor: null, truth: null,
    }, []);

    expect(pending.policy.resolution).toBe("ALLOWED");
    expect(pending.humanReview.resolution).toBe("PENDING");
    expect(pending.provider.accepted).toBe(false);
    expect(pending.reconciliation.confirmed).toBe(false);
  });

  it("renders terminal policy and human refusals without queued downstream execution", () => {
    const policyDenied: IncidentDetail = {
      ...approvedHuman,
      incident: { ...approvedHuman.incident, status: "STOPPED" },
      action: { ...approvedHuman.action!, status: "STOPPED", policyDecision: "DENY", approvedAt: null },
    };
    const stages = derivePipelineStages(normalizeRecoveryExecution(policyDenied, [event("deny", "POLICY_DECISION", "POLICY_ENGINE", "Denied", "DENY")]));
    expect(stages.find((stage) => stage.label === "Governor")?.state).toBe("SKIPPED");
    expect(stages.find((stage) => stage.label === "Human")?.state).toBe("NOT_APPLICABLE");
    expect(stages.find((stage) => stage.label === "Execute")?.state).toBe("SKIPPED");
    expect(stages.find((stage) => stage.label === "Accept")?.state).toBe("SKIPPED");
  });
});
