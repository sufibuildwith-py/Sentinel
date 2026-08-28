import { describe, expect, it } from "vitest";
import { fixtureEvaluation } from "./evaluation-fixture";

describe("evaluation proof contract", () => {
  it("keeps authoritative metric numerators and denominators inspectable", () => {
    expect(fixtureEvaluation.detectionPrecision.value).toBe(
      fixtureEvaluation.detectionPrecision.numerator / fixtureEvaluation.detectionPrecision.denominator,
    );
    expect(fixtureEvaluation.policyCompliance.value).toBe(1);
    expect(fixtureEvaluation.verifiedRecoveryRate.value).toBe(
      fixtureEvaluation.verifiedRecoveryRate.numerator / fixtureEvaluation.verifiedRecoveryRate.denominator,
    );
  });

  it("passes only when all hard safety counters remain zero", () => {
    expect(fixtureEvaluation.safetyGates).toHaveLength(8);
    expect(fixtureEvaluation.safetyGates.every((gate) => gate.passed && gate.actual === 0)).toBe(true);
    expect(fixtureEvaluation.duplicateActionsCreated).toBe(0);
    expect(fixtureEvaluation.duplicateFinancialEffects).toBe(0);
  });

  it("labels fixture financial claims as Test Mode and synthetic", () => {
    expect(fixtureEvaluation.scopeLabel).toContain("TEST MODE");
    expect(fixtureEvaluation.scopeLabel).toContain("SYNTHETIC EVALUATION");
    expect(fixtureEvaluation.limitations).toContain("Synthetic Test Mode results are not production merchant revenue.");
  });
});

