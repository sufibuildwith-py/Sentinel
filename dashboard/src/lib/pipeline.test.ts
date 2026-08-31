import { describe, expect, it } from "vitest";
import { nextActionFor, pipelineStates, progressFor } from "./pipeline";
import type { IncidentDetail } from "./types";
import { money, shortId } from "./api";

describe("incident command-center state", () => {
  it("maps every workflow state to a bounded pipeline step", () => {
    expect(progressFor("DETECTED")).toBe(0);
    expect(progressFor("DIAGNOSED")).toBe(3);
    expect(progressFor("HUMAN_REVIEW")).toBe(7);
    expect(progressFor("MONITORING")).toBe(10);
    expect(progressFor("RECOVERED")).toBe(11);
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
    expect(stages.find((stage) => stage.label === "Evidence")?.state).toBe("PENDING");
    expect(stages.find((stage) => stage.label === "Human")?.state).toBe("NOT_APPLICABLE");
    expect(stages.find((stage) => stage.label === "Execute")?.state).toBe("PENDING");
  });
});

describe("safe financial presentation", () => {
  it("formats integer minor units without floating-point storage", () => {
    expect(money(72300)).toMatch(/₹723\.00/);
    expect(shortId("7b47c4ee-5c22-43c4")).toBe("7B47C4EE");
  });
});
