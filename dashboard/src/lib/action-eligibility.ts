import type { AuditEntry, IncidentDetail } from "./types";

export type ActionEligibilityKind =
  | "EXECUTE"
  | "SUBMITTED"
  | "AWAITING_RECONCILIATION"
  | "PROVIDER_CONFIRMED"
  | "POLICY_BLOCKED"
  | "GOVERNOR_BLOCKED"
  | "HUMAN_REVIEW"
  | "NO_ACTION"
  | "UNSUPPORTED"
  | "NOT_READY";

export interface ActionEligibility {
  kind: ActionEligibilityKind;
  label: string;
  reason: string;
  executable: boolean;
}

const submittedStatuses = new Set(["EXECUTING", "EXECUTED", "RETRY_PENDING", "EXECUTION_UNCERTAIN", "PARTIALLY_RECOVERED"]);
const eligibleStatuses = new Set(["AUTO_APPROVED", "APPROVED"]);

export function actionEligibility(detail: IncidentDetail, audit: AuditEntry[] = []): ActionEligibility {
  const { incident, action, plan, truth } = detail;
  if (truth?.providerConfirmed || incident.status === "RECOVERED" || action?.status === "RECOVERED") {
    return result("PROVIDER_CONFIRMED", "Provider confirmed", truth?.basis ?? "Provider-confirmed recovery is persisted.");
  }
  if (truth?.awaitingReconciliation || (truth?.providerAccepted && !truth.providerConfirmed)) {
    return result("AWAITING_RECONCILIATION", "Awaiting provider reconciliation", truth.basis);
  }
  if (action && submittedStatuses.has(action.status)) {
    return result("SUBMITTED", "Action submitted", "A provider action has already been claimed or submitted. Duplicate execution is disabled.");
  }
  if (governorDenied(audit)) {
    const evidence = audit.find((entry) => entry.stage.includes("BLAST_RADIUS") && denied(entry));
    return result("GOVERNOR_BLOCKED", "Governor blocked", evidence?.narrative ?? "The recovery safety governor denied this execution envelope.");
  }
  if (action?.policyDecision === "DENY" || ["REJECTED", "STOPPED", "FAILED", "CANCELLED"].includes(action?.status ?? "")) {
    return result("POLICY_BLOCKED", "Action blocked", "Persisted policy or action state does not permit provider execution.");
  }
  if (plan?.strategy === "NO_ACTION") {
    return result("NO_ACTION", "No action", "Sentinel intentionally selected no intervention for this incident.");
  }
  if (action?.status === "PENDING_APPROVAL" || incident.status === "HUMAN_REVIEW"
      || (action?.policyDecision === "HUMAN" && !action.approvedAt)) {
    return result("HUMAN_REVIEW", "Awaiting human approval", "A persisted human approval is required before execution.");
  }
  if (!plan || !action) return result("NOT_READY", "Awaiting recovery decision", "A persisted plan and policy-gated action are required.");
  if (plan.strategy !== "ALTERNATIVE_PAYMENT_LINK") {
    return result("UNSUPPORTED", "Provider action unavailable", `${plan.strategy.replaceAll("_", " ")} is not executable as a Payment Link.`);
  }
  if (incident.status === "APPROVED" && eligibleStatuses.has(action.status)
      && (action.policyDecision === "AUTO" || (action.policyDecision === "HUMAN" && Boolean(action.approvedAt)))) {
    return { kind: "EXECUTE", label: "Create Payment Link", reason: "Current persisted policy and approval state permit one provider action.", executable: true };
  }
  return result("NOT_READY", "Action unavailable", `Incident ${incident.status} and action ${action.status} are not currently executable.`);
}

export function shouldPollIncident(detail?: IncidentDetail): boolean {
  if (!detail || detail.truth?.providerConfirmed || ["RECOVERED", "FAILED", "STOPPED"].includes(detail.incident.status)) return false;
  return ["EXECUTING", "MONITORING"].includes(detail.incident.status)
    || ["EXECUTING", "EXECUTED", "RETRY_PENDING", "EXECUTION_UNCERTAIN", "PARTIALLY_RECOVERED"].includes(detail.action?.status ?? "");
}

function governorDenied(audit: AuditEntry[]) {
  return audit.some((entry) => entry.stage.includes("BLAST_RADIUS") && denied(entry));
}

function denied(entry: AuditEntry) {
  return entry.policyResult === "DENY" || /denied|blocked/i.test(entry.narrative);
}

function result(kind: ActionEligibilityKind, label: string, reason: string): ActionEligibility {
  return { kind, label, reason, executable: false };
}
