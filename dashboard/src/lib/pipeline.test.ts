import { describe, expect, it } from "vitest";
import { nextActionFor, pipelineStates, progressFor } from "./pipeline";
import type { IncidentDetail } from "./types";
import { money, shortId } from "./api";

describe("incident command-center state", () => {
  it("maps every workflow state to a bounded pipeline step", () => {
    expect(progressFor("DETECTED")).toBe(0);
    expect(progressFor("DIAGNOSED")).toBe(3);
    expect(progressFor("HUMAN_REVIEW")).toBe(7);
    expect(progressFor("MONITORING")).toBe(11);
    expect(progressFor("RECOVERED")).toBe(12);
  });

  it("only offers valid next actions", () => {
    expect(nextActionFor("DETECTED")).toBe("investigate");
    expect(nextActionFor("DIAGNOSED")).toBe("plan");
    expect(nextActionFor("APPROVED")).toBe("execute");
    expect(nextActionFor("HUMAN_REVIEW")).toBeNull();
    expect(nextActionFor("MONITORING")).toBeNull();
  });
});

describe("audit-backed pipeline", () => {
  it("does not infer completed evidence stages from a later incident status", () => {
    const detail: IncidentDetail = {
      incident: { incidentId: "1", type: "TEST", status: "APPROVED", severity: "HIGH", amountAtRiskMinor: 100, detectedAt: "2026-08-31T00:00:00Z", affectedPaymentCount: 1, affectedCustomerCount: 1, recoveredAmountMinor: 0 },
      findings: [],
      action: { actionId: "a", status: "AUTO_APPROVED", policyDecision: "AUTO", amountMinor: 100, currency: "INR", executionAttempts: 0 },
    };
    const stages = pipelineStates(detail, []);
    expect(stages.find((stage) => stage.label === "Evidence")?.state).toBe("QUEUED");
    expect(stages.find((stage) => stage.label === "Human")?.state).toBe("NOT_APPLICABLE");
    expect(stages.find((stage) => stage.label === "Execute")?.state).toBe("QUEUED");
  });

  it("derives blocked, held, provider-accepted, and learned stages only from persisted facts", () => {
    const detail: IncidentDetail = {
      incident: { incidentId: "1", type: "TEST", status: "MONITORING", severity: "HIGH", amountAtRiskMinor: 100, detectedAt: "2026-08-31T00:00:00Z", affectedPaymentCount: 1, affectedCustomerCount: 1, recoveredAmountMinor: 0 },
      findings: [],
      action: { actionId: "a", status: "EXECUTED", policyDecision: "HUMAN", amountMinor: 100, currency: "INR", executionAttempts: 1, approvedAt: "2026-08-31T00:00:03Z", executedAt: "2026-08-31T00:00:05Z" },
      governor: { decisionId: "g", allowed: true, allowedValueMinor: 100, violations: [], evaluatedAt: "2026-08-31T00:00:04Z" },
      truth: { stage: "PROVIDER_ACCEPTED", executionMode: "RAZORPAY_TEST_MODE", providerAccepted: true, awaitingReconciliation: true, providerConfirmed: false, providerConfirmedAmountMinor: 0, basis: "Provider accepted; awaiting signed event" },
    };
    const audit = [
      { eventId: "1", timestamp: "2026-08-31T00:00:03Z", actor: "reviewer", stage: "HUMAN_APPROVED", narrative: "Approved", evidence: [], ruleTrace: [] },
      { eventId: "g", timestamp: "2026-08-31T00:00:04Z", actor: "GOVERNOR", stage: "BLAST_RADIUS_EVALUATED", narrative: "Execution envelope granted", evidence: [], ruleTrace: [], policyResult: "ALLOW" },
      { eventId: "2", timestamp: "2026-08-31T00:00:05Z", actor: "RAZORPAY_TEST", stage: "EXECUTION_SUCCESS", narrative: "Provider resource accepted", evidence: [], ruleTrace: [] },
    ];
    const stages = pipelineStates(detail, audit);
    expect(stages.find((stage) => stage.label === "Human")?.state).toBe("COMPLETE");
    expect(stages.find((stage) => stage.label === "Accept")?.state).toBe("COMPLETE");
    expect(stages.find((stage) => stage.label === "Reconcile")?.state).toBe("ACTIVE");
    expect(stages.find((stage) => stage.label === "Learn")?.state).toBe("QUEUED");
  });
});

describe("safe financial presentation", () => {
  it("formats integer minor units without floating-point storage", () => {
    expect(money(72300)).toMatch(/₹723\.00/);
    expect(shortId("7b47c4ee-5c22-43c4")).toBe("7B47C4EE");
  });
});
