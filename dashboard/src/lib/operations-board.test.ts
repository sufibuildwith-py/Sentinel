import { describe, expect, it } from "vitest";
import { filterHistoricalCases, filterOperationalCases, financialTruthFunnel, policyDistribution } from "./operations-board";
import type { FinancialAttribution, HistoricalValidationCaseResult, IncidentSummary } from "./types";

const incident = (id: string, type: string, status: IncidentSummary["status"], policyDecision: IncidentSummary["policyDecision"]): IncidentSummary => ({
  incidentId: id, type, status, policyDecision, severity: "HIGH", amountAtRiskMinor: 1_000,
  detectedAt: "2026-09-01T00:00:00Z", affectedPaymentCount: 1, affectedCustomerCount: 1, recoveredAmountMinor: 0,
});
describe("recovery operations board truth", () => {
  it("derives financial funnel values only from backend attribution fields", () => {
    const attribution = { failedValueMinor: 900, addressableValueMinor: 700, executedValueMinor: 400, providerConfirmedRecoveryMinor: 250, attributedIncrementalRecoveryMinor: 200 } as FinancialAttribution;
    expect(financialTruthFunnel(attribution).map((item) => item.value)).toEqual([900, 700, 400, 250, 200]);
  });

  it("derives policy counts from operational incidents without fallback values", () => {
    expect(policyDistribution([incident("a", "UPI", "APPROVED", "AUTO"), incident("b", "CARD", "STOPPED", "DENY")])).toEqual([
      { name: "AUTO", value: 1 }, { name: "HUMAN", value: 0 }, { name: "DENY", value: 1 }, { name: "PENDING", value: 0 },
    ]);
    expect(policyDistribution([]).every((item) => item.value === 0)).toBe(true);
  });

  it("filters operational cases by persisted status and domain fields", () => {
    const rows = [incident("upi-1", "UPI_DEGRADATION", "APPROVED", "AUTO"), incident("card-1", "CARD_FAILURE", "STOPPED", "DENY")];
    expect(filterOperationalCases(rows, "upi", "APPROVED").map((item) => item.incidentId)).toEqual(["upi-1"]);
  });

  it("keeps historical source cases separate and filterable", () => {
    const historical = [{ caseId: "RZP-001", sourceClass: "GITHUB_ISSUE", productSurface: "PAYMENT_LINK", normalizedFailureClass: "SIGNATURE_FAILURE", policyDisposition: "DENY", result: "PASS" }] as HistoricalValidationCaseResult[];
    expect(filterHistoricalCases(historical, "signature", "PASS")).toHaveLength(1);
    expect(filterHistoricalCases(historical, "signature", "FAIL")).toHaveLength(0);
  });
});
