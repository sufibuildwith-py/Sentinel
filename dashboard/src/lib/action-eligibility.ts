import { derivePrimaryRecoveryAction, normalizeRecoveryExecution } from "./recovery-execution-state";
import type { PrimaryRecoveryAction, PrimaryRecoveryKind } from "./recovery-execution-state";
import type { AuditEntry, IncidentDetail } from "./types";

export type ActionEligibilityKind = PrimaryRecoveryKind;
export type ActionEligibility = PrimaryRecoveryAction;

export function actionEligibility(detail: IncidentDetail, audit: AuditEntry[] = []): ActionEligibility {
  return derivePrimaryRecoveryAction(normalizeRecoveryExecution(detail, audit));
}

export function shouldPollIncident(detail?: IncidentDetail): boolean {
  if (!detail || detail.truth?.providerConfirmed || ["RECOVERED", "FAILED", "STOPPED"].includes(detail.incident.status)) return false;
  return ["EXECUTING", "MONITORING"].includes(detail.incident.status)
    || ["EXECUTING", "EXECUTED", "RETRY_PENDING", "EXECUTION_UNCERTAIN", "PARTIALLY_RECOVERED"].includes(detail.action?.status ?? "");
}
