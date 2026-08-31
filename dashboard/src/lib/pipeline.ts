import type { AuditEntry, IncidentDetail, IncidentStatus } from "./types";

export const pipeline = ["Detect", "Triage", "Evidence", "Diagnose", "Counterfactual", "Plan", "Policy", "Governor", "Human", "Execute", "Accept", "Reconcile", "Learn"] as const;
const progress: Record<IncidentStatus, number> = { DETECTED: 0, INVESTIGATING: 1, DIAGNOSED: 3, PLANNING: 5, POLICY_REVIEW: 6, HUMAN_REVIEW: 8, APPROVED: 8, EXECUTING: 9, MONITORING: 11, RECOVERED: 12, FAILED: 11, STOPPED: 6 };
export const progressFor = (status: IncidentStatus) => progress[status];
export const nextActionFor = (status: IncidentStatus) => status === "DETECTED" ? "investigate" : status === "DIAGNOSED" ? "plan" : status === "APPROVED" ? "execute" : null;

export type PipelineStageState = "COMPLETE" | "ACTIVE" | "QUEUED" | "HELD" | "BLOCKED" | "SKIPPED" | "NOT_APPLICABLE" | "FAILED";
export interface PipelineStageView {
  label: typeof pipeline[number];
  state: PipelineStageState;
  evidence: string;
  timestamp?: string;
  actor?: string;
  eventId?: string;
}

export function pipelineStates(detail: IncidentDetail, audit: AuditEntry[]): PipelineStageView[] {
  const first = (pattern: RegExp) => audit.find((entry) => pattern.test(searchable(entry)));
  const last = (pattern: RegExp) => audit.findLast((entry) => pattern.test(searchable(entry)));
  const finding = (source: string) => detail.findings.find((item) => item.source === source);
  const action = detail.action;
  const truth = detail.truth;
  const stopped = detail.incident.status === "STOPPED";
  const policyDecision = last(/POLICY_DECISION/i);
  const policyBlock = policyDecision && (policyDecision.policyResult === "DENY" || /\bDENY|denied|blocked/i.test(searchable(policyDecision))) ? policyDecision : undefined;
  const governorDecision = last(/BLAST_RADIUS_EVALUATED|GOVERNOR/i);
  const governorBlock = governorDecision && (governorDecision.policyResult === "DENY" || /denied|blocked/i.test(searchable(governorDecision))) ? governorDecision : undefined;
  const humanApproved = last(/HUMAN_APPROVED/i);
  const humanRejected = last(/HUMAN_REJECTED/i);
  const executionClaimed = last(/EXECUTION_CLAIMED|PROVIDER_REQUEST/i);
  const executionFailed = last(/EXECUTION_FAILED|EXECUTION_STOPPED|EXECUTION_CANCELLED/i);
  const providerAccepted = last(/EXECUTION_SUCCESS/i);
  const reconciliation = last(/WEBHOOK_(?:ACCEPTED|APPLIED|IGNORED_STALE)|RECOVERY_(?:PARTIAL|METRIC_UPDATED|CANCELLED)/i);
  const learned = last(/HISTORICAL_MEMORY_RECORDED|RECOVERY_METRIC_UPDATED/i);
  const diagnosis = finding("ROOT_CAUSE_AGENT");
  const analyst = finding("PAYMENT_ANALYST");
  const triage = first(/TRIAGE|INVESTIGATE/i);
  const opportunity = first(/COUNTERFACTUAL|SHADOW_OPPORTUNITY_EVALUATED/i);
  const proposal = first(/RECOVERY_PROPOSED|RecoveryPlanner/i);
  const persistedProviderAcceptance = truth?.providerAccepted || (Boolean(action?.providerId) && Boolean(action?.executedAt));
  const fixtureReconciliation = last(/\bOBSERVE\b.*(?:signed|verified|paid|recovered)/i);

  return [
    fromAudit("Detect", first(/INCIDENT_DETECTED/i), "COMPLETE", "Persisted incident detection"),
    fromAudit("Triage", triage, triage ? "COMPLETE" : detail.incident.status === "INVESTIGATING" ? "ACTIVE" : "QUEUED", "No persisted triage event"),
    analyst ? fromFinding("Evidence", analyst) : view("Evidence", detail.incident.status === "INVESTIGATING" && Boolean(triage) ? "ACTIVE" : "QUEUED", "No persisted analyst evidence"),
    diagnosis ? fromFinding("Diagnose", diagnosis) : view("Diagnose", detail.incident.status === "INVESTIGATING" && Boolean(analyst) ? "ACTIVE" : "QUEUED", "No persisted diagnosis"),
    fromAudit("Counterfactual", opportunity, opportunity ? "COMPLETE" : detail.incident.status === "PLANNING" ? "ACTIVE" : "QUEUED", "No persisted counterfactual or opportunity evaluation"),
    itemPlan(detail) ? fromAudit("Plan", proposal, "COMPLETE", "Persisted recovery proposal") : fromAudit("Plan", proposal, proposal ? "COMPLETE" : detail.incident.status === "PLANNING" ? "ACTIVE" : "QUEUED", "No persisted recovery proposal"),
    fromAudit("Policy", policyBlock ?? policyDecision, policyBlock ? "BLOCKED" : policyDecision || action?.policyDecision ? "COMPLETE" : detail.incident.status === "POLICY_REVIEW" ? "ACTIVE" : "QUEUED", action?.policyDecision ? `Persisted policy disposition ${action.policyDecision}` : "No persisted policy decision"),
    fromAudit("Governor", governorBlock ?? governorDecision, governorBlock ? "BLOCKED" : governorDecision ? "COMPLETE" : policyBlock ? "SKIPPED" : "QUEUED", policyBlock ? "Provider execution stopped at deterministic policy" : "No persisted governor evaluation"),
    humanRejected ? fromAudit("Human", humanRejected, "BLOCKED", "Human review denied execution")
      : humanApproved ? fromAudit("Human", humanApproved, "COMPLETE", "Human approval persisted")
      : action?.policyDecision === "AUTO" ? view("Human", "NOT_APPLICABLE", "Deterministic policy authorized this action without human review")
      : action?.status === "PENDING_APPROVAL" || detail.incident.status === "HUMAN_REVIEW" ? view("Human", "HELD", "Persisted human review is required")
      : action?.policyDecision === "DENY" ? view("Human", "SKIPPED", "Policy denied the proposal before human review")
      : view("Human", "QUEUED", "No persisted human decision"),
    executionFailed ? fromAudit("Execute", executionFailed, "FAILED", "Provider execution ended safely")
      : executionClaimed || action?.executedAt ? fromAudit("Execute", executionClaimed, action?.status === "EXECUTING" ? "ACTIVE" : "COMPLETE", action?.executedAt ? `Execution persisted ${action.executedAt}` : "Execution claimed")
      : view("Execute", policyBlock || governorBlock || humanRejected ? "SKIPPED" : detail.incident.status === "EXECUTING" ? "ACTIVE" : "QUEUED", "No persisted execution event"),
    providerAccepted || persistedProviderAcceptance ? fromAudit("Accept", providerAccepted, "COMPLETE", truth?.basis ?? "Provider resource and execution timestamp persisted")
      : view("Accept", truth?.stage === "EXECUTION_REQUESTED" ? "ACTIVE" : executionFailed ? "SKIPPED" : "QUEUED", truth?.stage === "EXECUTION_REQUESTED" ? truth.basis : "No provider acceptance persisted"),
    truth?.providerConfirmed || fixtureReconciliation ? fromAudit("Reconcile", reconciliation ?? fixtureReconciliation, "COMPLETE", truth?.basis ?? "Signed provider outcome persisted")
      : truth?.awaitingReconciliation ? fromAudit("Reconcile", reconciliation, "ACTIVE", truth.basis)
      : view("Reconcile", executionFailed || stopped ? "SKIPPED" : "QUEUED", "No provider-confirmed reconciliation persisted"),
    learned ? fromAudit("Learn", learned, "COMPLETE", "Reconciled outcome retained for attribution and memory")
      : view("Learn", truth?.providerConfirmed ? "ACTIVE" : executionFailed || stopped ? "SKIPPED" : "QUEUED", truth?.providerConfirmed ? "Provider truth is available; no persisted learning event yet" : "Requires provider-confirmed reconciliation"),
  ];
}

function searchable(entry: AuditEntry) {
  return `${entry.stage} ${entry.actor} ${entry.narrative} ${entry.policyResult ?? ""} ${entry.evidence.join(" ")}`;
}

function itemPlan(detail: IncidentDetail) { return detail.plan != null; }

function fromAudit(label: typeof pipeline[number], event: AuditEntry | undefined, state: PipelineStageState, fallback: string): PipelineStageView {
  return { label, state, evidence: event?.narrative ?? fallback, timestamp: event?.timestamp, actor: event?.actor, eventId: event?.eventId };
}

function fromFinding(label: typeof pipeline[number], finding: IncidentDetail["findings"][number]): PipelineStageView {
  return { label, state: "COMPLETE", evidence: finding.summary, timestamp: finding.createdAt, actor: finding.source };
}

function view(label: typeof pipeline[number], state: PipelineStageState, evidence: string): PipelineStageView {
  return { label, state, evidence };
}
