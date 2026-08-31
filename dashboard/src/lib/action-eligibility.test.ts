import { describe, expect, it } from "vitest";
import { actionEligibility, shouldPollIncident } from "./action-eligibility";
import type { AuditEntry, IncidentDetail } from "./types";

const base: IncidentDetail = {
  incident: { incidentId: "incident-1", type: "UPI_DEGRADATION", status: "APPROVED", severity: "HIGH", amountAtRiskMinor: 10000, detectedAt: "2026-08-31T12:00:00Z", affectedPaymentCount: 1, affectedCustomerCount: 1, recoveredAmountMinor: 0 },
  findings: [],
  plan: { planId: "plan-1", strategy: "ALTERNATIVE_PAYMENT_LINK", reason: "provider degradation", targetAmountMinor: 10000, confidence: 0.9, riskLevel: "LOW" },
  action: { actionId: "action-1", status: "AUTO_APPROVED", policyDecision: "AUTO", amountMinor: 10000, currency: "INR", executionAttempts: 0 },
  truth: { stage: "POLICY_APPROVED", executionMode: "RAZORPAY_TEST_MODE", providerAccepted: false, awaitingReconciliation: false, providerConfirmed: false, providerConfirmedAmountMinor: 0, basis: "Persisted policy approval" },
};

const withDetail = (patch: Partial<IncidentDetail>): IncidentDetail => ({ ...base, ...patch });
const withAction = (patch: Partial<NonNullable<IncidentDetail["action"]>>): IncidentDetail => ({ ...base, action: { ...base.action!, ...patch } });

describe("persisted recovery action eligibility", () => {
  it("allows one approved unexecuted Payment Link", () => expect(actionEligibility(base).kind).toBe("EXECUTE"));
  it("stops offering execution after submission", () => expect(actionEligibility(withAction({ status: "EXECUTED", providerId: "plink_1" })).kind).toBe("SUBMITTED"));
  it("shows provider acceptance as awaiting reconciliation", () => expect(actionEligibility(withDetail({ truth: { ...base.truth!, stage: "AWAITING_RECONCILIATION", providerAccepted: true, awaitingReconciliation: true } })).kind).toBe("AWAITING_RECONCILIATION"));
  it("shows provider-confirmed truth", () => expect(actionEligibility(withDetail({ truth: { ...base.truth!, stage: "RECOVERED_CONFIRMED", providerAccepted: true, awaitingReconciliation: false, providerConfirmed: true, providerConfirmedAmountMinor: 10000 } })).kind).toBe("PROVIDER_CONFIRMED"));
  it("blocks a policy denial", () => expect(actionEligibility(withAction({ status: "STOPPED", policyDecision: "DENY" })).kind).toBe("POLICY_BLOCKED"));
  it("blocks a governor denial from persisted audit", () => {
    const audit: AuditEntry[] = [{ eventId: "1", timestamp: "2026-08-31T12:00:00Z", actor: "GOVERNOR", stage: "BLAST_RADIUS_EVALUATED", narrative: "Execution envelope denied", evidence: [], ruleTrace: [], policyResult: "DENY" }];
    expect(actionEligibility(base, audit).kind).toBe("GOVERNOR_BLOCKED");
  });
  it("requires persisted human approval", () => expect(actionEligibility(withAction({ status: "PENDING_APPROVAL", policyDecision: "HUMAN" })).kind).toBe("HUMAN_REVIEW"));
  it("represents NO_ACTION as intentional", () => expect(actionEligibility(withDetail({ plan: { ...base.plan!, strategy: "NO_ACTION" } })).kind).toBe("NO_ACTION"));
  it("does not map unsupported strategies to Payment Links", () => expect(actionEligibility(withDetail({ plan: { ...base.plan!, strategy: "DEFERRED_RETRY" } })).kind).toBe("UNSUPPORTED"));
  it("requires both incident and action execution state", () => expect(actionEligibility(withAction({ status: "PROPOSED" })).executable).toBe(false));

  it("polls active reconciliation and stops at provider-confirmed truth", () => {
    expect(shouldPollIncident(withAction({ status: "EXECUTED" }))).toBe(true);
    expect(shouldPollIncident(withDetail({ truth: { ...base.truth!, stage: "RECOVERED_CONFIRMED", providerAccepted: true, providerConfirmed: true, providerConfirmedAmountMinor: 10000 } }))).toBe(false);
  });
});
