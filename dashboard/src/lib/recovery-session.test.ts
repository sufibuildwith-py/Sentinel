import { describe, expect, it } from "vitest";
import type { AuditEntry, IncidentDetail } from "./types";
import { executionLedger, mergeAuditEntries, nextRecoveryOperation, recoveryPollingInterval, sessionButtonLabel } from "./recovery-session";

const detail: IncidentDetail = {
  incident: { incidentId: "incident-1", type: "UPI_DEGRADATION", status: "APPROVED", severity: "HIGH", amountAtRiskMinor: 10000, detectedAt: "2026-09-01T00:00:00Z", affectedPaymentCount: 1, affectedCustomerCount: 1, recoveredAmountMinor: 0 },
  findings: [],
  plan: { planId: "plan-1", strategy: "ALTERNATIVE_PAYMENT_LINK", reason: "degradation", targetAmountMinor: 10000, confidence: .9, riskLevel: "LOW" },
  action: { actionId: "action-1", status: "AUTO_APPROVED", policyDecision: "AUTO", amountMinor: 10000, currency: "INR", executionAttempts: 0 },
  truth: { stage: "POLICY_APPROVED", executionMode: "RAZORPAY_TEST_MODE", providerAccepted: false, awaitingReconciliation: false, providerConfirmed: false, providerConfirmedAmountMinor: 0, basis: "Persisted approval" },
};

const event = (eventId: string, stage: string, actor = "SENTINEL", narrative = stage): AuditEntry => ({ eventId, timestamp: `2026-09-01T00:00:0${eventId}Z`, actor, stage, narrative, evidence: [], ruleTrace: [] });

describe("live recovery session", () => {
  it("starts with the real next persisted operation", () => {
    expect(nextRecoveryOperation({ ...detail, incident: { ...detail.incident, status: "DETECTED" }, plan: null, action: null }, [])).toBe("investigate");
    expect(nextRecoveryOperation({ ...detail, incident: { ...detail.incident, status: "DIAGNOSED" }, plan: null, action: null }, [])).toBe("plan");
    expect(nextRecoveryOperation(detail, [])).toBe("execute");
  });

  it("disables a run after provider execution and never equates acceptance with recovery", () => {
    const accepted = { ...detail, incident: { ...detail.incident, status: "MONITORING" as const }, action: { ...detail.action!, status: "EXECUTED" }, truth: { ...detail.truth!, stage: "PROVIDER_ACCEPTED" as const, providerAccepted: true, awaitingReconciliation: true } };
    expect(nextRecoveryOperation(accepted, [])).toBeNull();
    expect(sessionButtonLabel(accepted, [], false)).toBe("AWAITING PROVIDER TRUTH");
    expect(recoveryPollingInterval(accepted, false)).toBe(6000);
  });

  it("stops polling for terminal, blocked, denied, and human-review states", () => {
    expect(recoveryPollingInterval({ ...detail, incident: { ...detail.incident, status: "RECOVERED" } }, true)).toBe(false);
    expect(recoveryPollingInterval({ ...detail, incident: { ...detail.incident, status: "HUMAN_REVIEW" }, action: { ...detail.action!, status: "PENDING_APPROVAL", policyDecision: "HUMAN" } }, true)).toBe(false);
    expect(recoveryPollingInterval(undefined, true)).toBe(false);
  });

  it("pauses for human review, resumes only after persisted approval, and terminates after denial", () => {
    const held = { ...detail, incident: { ...detail.incident, status: "HUMAN_REVIEW" as const }, action: { ...detail.action!, status: "PENDING_APPROVAL", policyDecision: "HUMAN" } };
    expect(nextRecoveryOperation(held, [])).toBeNull();
    expect(sessionButtonLabel(held, [], false)).toBe("AWAITING HUMAN REVIEW");
    const approved = { ...held, incident: { ...held.incident, status: "APPROVED" as const }, action: { ...held.action, status: "APPROVED", approvedAt: "2026-09-01T00:00:04Z" } };
    expect(nextRecoveryOperation(approved, [])).toBe("execute");
    const denied = { ...held, incident: { ...held.incident, status: "STOPPED" as const }, action: { ...held.action, status: "REJECTED" } };
    expect(nextRecoveryOperation(denied, [])).toBeNull();
  });

  it("stops at persisted policy and governor refusals", () => {
    const policyDenied = { ...detail, incident: { ...detail.incident, status: "STOPPED" as const }, action: { ...detail.action!, status: "STOPPED", policyDecision: "DENY" } };
    expect(sessionButtonLabel(policyDenied, [], false)).toBe("BLOCKED BY POLICY");
    const governorAudit = [{ ...event("1", "BLAST_RADIUS_EVALUATED", "GOVERNOR", "Execution envelope denied"), policyResult: "DENY" }];
    expect(sessionButtonLabel(detail, governorAudit, false)).toBe("HELD BY GOVERNOR");
    expect(nextRecoveryOperation(detail, governorAudit)).toBeNull();
  });

  it("completes only on persisted provider-confirmed truth", () => {
    const confirmed = { ...detail, incident: { ...detail.incident, status: "RECOVERED" as const }, action: { ...detail.action!, status: "RECOVERED" }, truth: { ...detail.truth!, stage: "RECOVERED_CONFIRMED" as const, providerAccepted: true, providerConfirmed: true, providerConfirmedAmountMinor: 10000 } };
    expect(sessionButtonLabel(confirmed, [], false)).toBe("RECOVERY COMPLETE");
    expect(recoveryPollingInterval(confirmed, true)).toBe(false);
  });

  it("appends new persisted audit events once and preserves order", () => {
    const merged = mergeAuditEntries([event("1", "INCIDENT_DETECTED")], [event("1", "INCIDENT_DETECTED"), event("2", "POLICY_DECISION", "POLICY_ENGINE")]);
    expect(merged.map((item) => item.eventId)).toEqual(["1", "2"]);
    expect(executionLedger(merged).map((item) => item.actor)).toEqual(["EVIDENCE", "POLICY"]);
  });

  it("labels provider acceptance as Test Mode and confirmed attribution separately", () => {
    const ledger = executionLedger([
      event("1", "EXECUTION_SUCCESS", "RAZORPAY_TEST", "Provider accepted resource"),
      event("2", "RECOVERY_METRIC_UPDATED", "RAZORPAY_WEBHOOK", "Verified amount applied"),
    ]);
    expect(ledger[0].truthClass).toBe("RAZORPAY_TEST_MODE");
    expect(ledger[1].truthClass).toBe("PROVIDER_CONFIRMED");
  });
});
