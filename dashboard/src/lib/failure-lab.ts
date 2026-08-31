import type { FailureLabResult } from "./types";

export interface FailureLabTraceStep {
  label: "SCENARIO" | "EVIDENCE" | "DECISION" | "SAFETY OUTCOME";
  state: "COMPLETE" | "SAFE_REFUSAL" | "REQUIRES_PROVIDER_EVENT" | "NOT_EVIDENCED";
  detail: string;
}

export function failureLabTrace(result: FailureLabResult): FailureLabTraceStep[] {
  const requiresProvider = result.status === "REQUIRES_REAL_PROVIDER_EVENT";
  const refusal = /DENY|STOP|BLOCK|REFUS|NO_ACTION|HELD|PROMOTION_BLOCKED/.test(result.scenario.expectedSafetyOutcome);
  return [
    { label: "SCENARIO", state: "COMPLETE", detail: `${result.scenario.mode.replaceAll("_", " ")} · ${result.scenario.title}` },
    { label: "EVIDENCE", state: result.evidence.length ? "COMPLETE" : "NOT_EVIDENCED", detail: result.evidence.length ? `${result.evidence.length} persisted evaluation evidence item(s)` : "No matching persisted evidence was returned" },
    { label: "DECISION", state: requiresProvider ? "REQUIRES_PROVIDER_EVENT" : result.safetyDemonstrationPassed ? "COMPLETE" : "NOT_EVIDENCED", detail: result.observedBehavior },
    { label: "SAFETY OUTCOME", state: requiresProvider ? "REQUIRES_PROVIDER_EVENT" : result.safetyDemonstrationPassed && refusal ? "SAFE_REFUSAL" : result.safetyDemonstrationPassed ? "COMPLETE" : "NOT_EVIDENCED", detail: result.scenario.expectedSafetyOutcome.replaceAll("_", " ") },
  ];
}
