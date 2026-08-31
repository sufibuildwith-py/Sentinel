import { describe, expect, it } from "vitest";
import { failureLabTrace } from "./failure-lab";

describe("Failure Lab persisted processing trace", () => {
  it("treats an evidenced policy refusal as a successful safety demonstration", () => {
    const trace = failureLabTrace({ scenario: { id: "policy-deny", title: "Policy DENY", description: "", mode: "SYNTHETIC_BENCHMARK", expectedSafetyOutcome: "DENY", evidenceSelector: "paid", runnable: true }, status: "EVIDENCED", safetyDemonstrationPassed: true, observedBehavior: "Policy rejected an already-paid action", evidence: ["scenario passed"], evaluatedAt: "2026-09-01T00:00:00Z" });
    expect(trace.at(-1)?.state).toBe("SAFE_REFUSAL");
  });

  it("does not synthesize success when signed provider evidence is required", () => {
    const trace = failureLabTrace({ scenario: { id: "accepted", title: "Accepted", description: "", mode: "REAL_RAZORPAY_TEST_MODE", expectedSafetyOutcome: "AWAITING_RECONCILIATION", evidenceSelector: "provider", runnable: false }, status: "REQUIRES_REAL_PROVIDER_EVENT", safetyDemonstrationPassed: false, observedBehavior: "No outcome synthesized", evidence: ["AWAITING RECONCILIATION"], evaluatedAt: "2026-09-01T00:00:00Z" });
    expect(trace.at(-1)?.state).toBe("REQUIRES_PROVIDER_EVENT");
  });
});
