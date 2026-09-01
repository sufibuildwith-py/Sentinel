import type { FinancialAttribution, HistoricalValidationCaseResult, IncidentSummary } from "./types";

export function financialTruthFunnel(attribution: FinancialAttribution) {
  return [
    { name: "Failed", value: attribution.failedValueMinor },
    { name: "Addressable", value: attribution.addressableValueMinor },
    { name: "Executed", value: attribution.executedValueMinor },
    { name: "Confirmed", value: attribution.providerConfirmedRecoveryMinor },
    { name: "Attributed", value: attribution.attributedIncrementalRecoveryMinor },
  ];
}
export function policyDistribution(incidents: IncidentSummary[]) {
  const values = new Map<string, number>([["AUTO", 0], ["HUMAN", 0], ["DENY", 0], ["PENDING", 0]]);
  incidents.forEach((item) => {
    const disposition = item.policyDecision ?? "PENDING";
    values.set(disposition, (values.get(disposition) ?? 0) + 1);
  });
  return [...values.entries()].map(([name, value]) => ({ name, value }));
}

export function filterOperationalCases(incidents: IncidentSummary[], search: string, status: string) {
  const query = search.trim().toLowerCase();
  return incidents.filter((item) => (status === "ALL" || item.status === status)
    && `${item.incidentId} ${item.type} ${item.strategy ?? ""} ${item.policyDecision ?? ""}`.toLowerCase().includes(query));
}

export function filterHistoricalCases(cases: HistoricalValidationCaseResult[], search: string, status: string) {
  const query = search.trim().toLowerCase();
  return cases.filter((item) => (status === "ALL" || item.result === status)
    && `${item.caseId} ${item.sourceClass} ${item.productSurface} ${item.normalizedFailureClass} ${item.policyDisposition}`.toLowerCase().includes(query));
}
