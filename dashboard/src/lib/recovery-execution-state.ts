import type { AuditEntry, IncidentDetail } from "./types";

export type Resolution = "NOT_STARTED" | "ACTIVE" | "ALLOWED" | "DENIED" | "PENDING" | "APPROVED" | "REJECTED" | "NOT_REQUIRED";

export interface RecoveryExecutionState {
  detail: IncidentDetail;
  audit: AuditEntry[];
  investigation: { triage: boolean; evidence: boolean; diagnosis: boolean };
  plan: { exists: boolean; counterfactual: boolean };
  policy: { resolution: Resolution; decision: string | null; event?: AuditEntry };
  governor: { resolution: Resolution; event?: AuditEntry; reason: string };
  humanReview: { resolution: Resolution; approvalPersisted: boolean; event?: AuditEntry; reason: string };
  action: { status: string | null; executionPermission: boolean; resumable: boolean; claimed: boolean; executed: boolean };
  provider: { accepted: boolean; awaitingReconciliation: boolean };
  reconciliation: { confirmed: boolean; event?: AuditEntry };
}

export type PrimaryRecoveryKind =
  | "INVESTIGATE" | "PLAN" | "EXECUTE" | "SUBMITTED" | "AWAITING_RECONCILIATION"
  | "PROVIDER_CONFIRMED" | "POLICY_BLOCKED" | "GOVERNOR_BLOCKED" | "HUMAN_REVIEW"
  | "NO_ACTION" | "UNSUPPORTED" | "NOT_READY";

export interface PrimaryRecoveryAction {
  kind: PrimaryRecoveryKind;
  label: string;
  reason: string;
  executable: boolean;
  operation: "investigate" | "plan" | "execute" | null;
}

const submittedStatuses = new Set(["EXECUTING", "EXECUTED", "PARTIALLY_RECOVERED"]);
const resumableStatuses = new Set(["RETRY_PENDING", "EXECUTION_UNCERTAIN"]);

export function normalizeRecoveryExecution(detail: IncidentDetail, audit: AuditEntry[] = []): RecoveryExecutionState {
  const last = (pattern: RegExp) => audit.findLast((entry) => pattern.test(searchable(entry)));
  const action = detail.action;
  const policyEvent = last(/\bPOLICY_DECISION\b/i);
  const governorEvent = last(/\bBLAST_RADIUS_EVALUATED\b/i);
  const humanApprovedEvent = last(/\bHUMAN_APPROVED\b/i);
  const humanRejectedEvent = last(/\bHUMAN_REJECTED\b/i);
  const policyDecision = action?.policyDecision ?? policyEvent?.policyResult ?? detail.incident.policyDecision ?? null;
  const policyResolution: Resolution = policyDecision === "DENY" ? "DENIED"
    : policyDecision === "AUTO" || policyDecision === "HUMAN" ? "ALLOWED"
    : detail.incident.status === "POLICY_REVIEW" ? "ACTIVE" : "NOT_STARTED";
  const governorDenied = detail.governor?.allowed === false || denied(governorEvent);
  const governorAllowed = detail.governor?.allowed === true || allowed(governorEvent);
  let governorResolution: Resolution = policyResolution === "DENIED" ? "NOT_REQUIRED"
    : policyResolution !== "ALLOWED" ? "NOT_STARTED"
    : governorDenied ? "DENIED" : governorAllowed ? "ALLOWED"
    : detail.incident.status === "EXECUTING" ? "ACTIVE" : "NOT_STARTED";

  let humanResolution: Resolution = "NOT_STARTED";
  let humanReason = "No persisted human decision";
  let humanApprovalPersisted = false;
  if (policyDecision === "AUTO" || policyDecision === "DENY") {
    humanResolution = "NOT_REQUIRED";
    humanReason = policyDecision === "AUTO" ? "Deterministic AUTO policy requires no human authority" : "Policy denied before human review";
  } else if (policyDecision === "HUMAN") {
    if (humanRejectedEvent && ["REJECTED", "STOPPED"].includes(action?.status ?? "")) {
      humanResolution = "REJECTED";
      humanReason = humanRejectedEvent.narrative;
      if (governorResolution === "NOT_STARTED") governorResolution = "NOT_REQUIRED";
    } else if (humanApprovedEvent && ["APPROVED", "EXECUTING", "RETRY_PENDING", "EXECUTION_UNCERTAIN", "EXECUTED", "PARTIALLY_RECOVERED", "RECOVERED"].includes(action?.status ?? "") && Boolean(action?.approvedAt)) {
      humanApprovalPersisted = true;
      humanResolution = "APPROVED";
      humanReason = humanApprovedEvent.narrative;
    } else {
      humanResolution = "PENDING";
      humanReason = "Explicit persisted human approval is required";
    }
  }

  const providerAccepted = Boolean(detail.truth?.providerAccepted)
    || (Boolean(action?.providerId) && Boolean(action?.executedAt));
  const reconciliationEvent = last(/WEBHOOK_(?:ACCEPTED|APPLIED|IGNORED_STALE)|RECOVERY_(?:PARTIAL|METRIC_UPDATED|CANCELLED)|\bOBSERVE\b.*(?:signed|verified|paid|recovered)/i);

  return {
    detail,
    audit,
    investigation: {
      triage: Boolean(last(/TRIAGE|INVESTIGATE/i)),
      evidence: detail.findings.some((finding) => finding.source === "PAYMENT_ANALYST"),
      diagnosis: detail.findings.some((finding) => finding.source === "ROOT_CAUSE_AGENT"),
    },
    plan: { exists: Boolean(detail.plan), counterfactual: Boolean(last(/COUNTERFACTUAL|SHADOW_OPPORTUNITY_EVALUATED/i)) },
    policy: { resolution: policyResolution, decision: policyDecision, event: policyEvent },
    governor: {
      resolution: governorResolution,
      event: governorEvent,
      reason: detail.governor
        ? detail.governor.allowed ? `Governor allowed ${detail.governor.allowedValueMinor} minor units` : detail.governor.violations.join(" · ") || "Governor denied execution"
        : governorEvent?.narrative ?? "No persisted governor evaluation",
    },
    humanReview: { resolution: humanResolution, approvalPersisted: humanApprovalPersisted, event: humanRejectedEvent ?? humanApprovedEvent, reason: humanReason },
    action: {
      status: action?.status ?? null,
      executionPermission: Boolean(action) && (
        (action?.status === "AUTO_APPROVED" && policyDecision === "AUTO")
        || (action?.status === "APPROVED" && policyDecision === "HUMAN" && Boolean(action.approvedAt) && Boolean(humanApprovedEvent))
        || resumableStatuses.has(action?.status ?? "")
      ),
      resumable: resumableStatuses.has(action?.status ?? ""),
      claimed: Boolean(last(/EXECUTION_CLAIMED|PROVIDER_REQUEST/i)) || ["EXECUTING", "EXECUTED", "PARTIALLY_RECOVERED"].includes(action?.status ?? ""),
      executed: Boolean(action?.executedAt) || action?.status === "EXECUTED" || action?.status === "PARTIALLY_RECOVERED" || action?.status === "RECOVERED",
    },
    provider: { accepted: providerAccepted, awaitingReconciliation: Boolean(detail.truth?.awaitingReconciliation) || (providerAccepted && !detail.truth?.providerConfirmed) },
    reconciliation: { confirmed: Boolean(detail.truth?.providerConfirmed), event: reconciliationEvent },
  };
}

export function derivePrimaryRecoveryAction(state: RecoveryExecutionState): PrimaryRecoveryAction {
  const { detail } = state;
  const action = detail.action;
  if (state.reconciliation.confirmed || detail.incident.status === "RECOVERED" || action?.status === "RECOVERED") {
    return result("PROVIDER_CONFIRMED", "Provider confirmed", detail.truth?.basis ?? "Provider-confirmed recovery is persisted.");
  }
  if (state.provider.awaitingReconciliation) return result("AWAITING_RECONCILIATION", "Awaiting provider reconciliation", detail.truth?.basis ?? "Provider accepted; signed reconciliation is pending.");
  if (action && submittedStatuses.has(action.status)) return result("SUBMITTED", "Action submitted", "A provider action has already been claimed or submitted. Duplicate execution is disabled.");
  if (state.governor.resolution === "DENIED") return result("GOVERNOR_BLOCKED", "Governor blocked", state.governor.reason);
  if (state.policy.resolution === "DENIED" || ["REJECTED", "STOPPED", "FAILED", "CANCELLED"].includes(action?.status ?? "")) return result("POLICY_BLOCKED", "Action blocked", "Persisted policy or action state does not permit provider execution.");
  if (detail.plan?.strategy === "NO_ACTION") return result("NO_ACTION", "No action", "Sentinel intentionally selected no intervention for this incident.");
  if (state.humanReview.resolution === "PENDING" && !state.humanReview.approvalPersisted) return result("HUMAN_REVIEW", "Awaiting human approval", state.humanReview.reason);
  if (!state.investigation.diagnosis && detail.incident.status === "DETECTED") return operation("INVESTIGATE", "Run recovery", "Investigation is the first incomplete persisted stage.", "investigate");
  if (!state.plan.exists && detail.incident.status === "DIAGNOSED") return operation("PLAN", "Run recovery", "Recovery planning is the first incomplete persisted stage.", "plan");
  if (!detail.plan || !action) return result("NOT_READY", "Awaiting recovery decision", "A persisted plan and policy-gated action are required.");
  if (detail.executionAvailability && !detail.executionAvailability.eligible) {
    if (detail.executionAvailability.reasonCode === "GOVERNOR_DENIED") {
      return result("GOVERNOR_BLOCKED", "Governor blocked", detail.executionAvailability.reason);
    }
    if (detail.executionAvailability.reasonCode === "RAZORPAY_EXECUTION_DISABLED") {
      return result("UNSUPPORTED", "Test Mode execution disabled", detail.executionAvailability.reason);
    }
    if (detail.executionAvailability.reasonCode === "ACTION_ALREADY_SUBMITTED") {
      return result("SUBMITTED", "Action submitted", detail.executionAvailability.reason);
    }
    if (detail.executionAvailability.reasonCode === "MAX_EXECUTION_ATTEMPTS") {
      return result("NOT_READY", "Execution attempts exhausted", detail.executionAvailability.reason);
    }
  }
  if (detail.plan.strategy !== "ALTERNATIVE_PAYMENT_LINK") return result("UNSUPPORTED", "Provider action unavailable", `${detail.plan.strategy.replaceAll("_", " ")} is not executable as a Payment Link.`);
  if (state.action.executionPermission && !state.action.claimed && !state.provider.accepted) return operation("EXECUTE", "Run recovery", "Persisted policy and approval state permit the execution endpoint to evaluate the governor and submit at most one provider action.", "execute");
  if (state.action.resumable && !state.provider.accepted) return operation("EXECUTE", "Resume recovery", "The backend persisted a retry-safe state and will reconcile by idempotency key before any provider create.", "execute");
  return result("NOT_READY", "Action unavailable", `Incident ${detail.incident.status} and action ${action.status} are not currently executable.`);
}

function searchable(entry: AuditEntry) { return `${entry.stage} ${entry.actor} ${entry.narrative} ${entry.policyResult ?? ""} ${entry.evidence.join(" ")}`; }
function denied(entry?: AuditEntry) { return Boolean(entry && (entry.policyResult === "DENY" || /denied|blocked/i.test(searchable(entry)))); }
function allowed(entry?: AuditEntry) { return Boolean(entry && (entry.policyResult === "ALLOW" || /granted|allowed/i.test(searchable(entry)))); }
function result(kind: PrimaryRecoveryKind, label: string, reason: string): PrimaryRecoveryAction { return { kind, label, reason, executable: false, operation: null }; }
function operation(kind: PrimaryRecoveryKind, label: string, reason: string, value: NonNullable<PrimaryRecoveryAction["operation"]>): PrimaryRecoveryAction { return { kind, label, reason, executable: true, operation: value }; }
