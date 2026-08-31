import type { AuditEntry, IncidentDetail, IncidentStatus } from "./types";

export const pipeline = ["Detect", "Triage", "Evidence", "Diagnose", "Counterfactual", "Policy", "Governor", "Human", "Execute", "Accept", "Reconcile", "Learn"] as const;
const progress: Record<IncidentStatus, number> = { DETECTED: 0, INVESTIGATING: 2, DIAGNOSED: 3, PLANNING: 4, POLICY_REVIEW: 5, HUMAN_REVIEW: 7, APPROVED: 7, EXECUTING: 8, MONITORING: 10, RECOVERED: 11, FAILED: 10, STOPPED: 6 };
export const progressFor = (status: IncidentStatus) => progress[status];
export const nextActionFor = (status: IncidentStatus) => status === "DETECTED" ? "investigate" : status === "DIAGNOSED" ? "plan" : status === "APPROVED" ? "execute" : null;

export type PipelineStageState = "COMPLETE" | "CURRENT" | "PENDING" | "BLOCKED" | "SKIPPED" | "NOT_APPLICABLE" | "FAILED";
export interface PipelineStageView { label: typeof pipeline[number]; state: PipelineStageState; evidence: string; }

export function pipelineStates(detail: IncidentDetail, audit: AuditEntry[]): PipelineStageView[] {
  const has = (pattern: RegExp) => audit.find((entry) => pattern.test(`${entry.stage} ${entry.actor} ${entry.narrative}`));
  const finding = (source: string) => detail.findings.find((item) => item.source === source);
  const action = detail.action;
  const truth = detail.truth;
  const policyBlocked = action?.policyDecision === "DENY" || ["REJECTED", "STOPPED"].includes(action?.status ?? "");
  const governorBlock = has(/BLAST_RADIUS.*(?:DENY|BLOCK)|GOVERNOR.*(?:denied|blocked)/i);
  const executeEvidence = has(/EXECUTION_(?:CLAIMED|REQUESTED|ACCEPTED)|PROVIDER_REQUEST/i);
  const reconcileEvidence = has(/RECONCILIATION|OBSERVE|OUTCOME/i);
  const stages: PipelineStageView[] = [
    view("Detect", "COMPLETE", has(/DETECT/i)?.narrative ?? "Persisted incident detection"),
    view("Triage", has(/TRIAGE/i) ? "COMPLETE" : detail.incident.status === "INVESTIGATING" ? "CURRENT" : "PENDING", has(/TRIAGE/i)?.narrative ?? "No persisted triage event"),
    view("Evidence", finding("PAYMENT_ANALYST") ? "COMPLETE" : "PENDING", finding("PAYMENT_ANALYST")?.summary ?? "No persisted analyst evidence"),
    view("Diagnose", finding("ROOT_CAUSE_AGENT") ? "COMPLETE" : detail.incident.status === "INVESTIGATING" ? "CURRENT" : "PENDING", finding("ROOT_CAUSE_AGENT")?.summary ?? "No persisted diagnosis"),
    view("Counterfactual", has(/COUNTERFACTUAL|OPPORTUNITY/i) ? "COMPLETE" : "PENDING", has(/COUNTERFACTUAL|OPPORTUNITY/i)?.narrative ?? "No persisted counterfactual event"),
    view("Policy", policyBlocked ? "BLOCKED" : action?.policyDecision ? "COMPLETE" : detail.incident.status === "POLICY_REVIEW" ? "CURRENT" : "PENDING", has(/POLICY/i)?.narrative ?? "No persisted policy decision"),
    view("Governor", governorBlock ? "BLOCKED" : has(/BLAST_RADIUS|GOVERNOR/i) ? "COMPLETE" : "PENDING", governorBlock?.narrative ?? has(/BLAST_RADIUS|GOVERNOR/i)?.narrative ?? "No persisted governor evaluation"),
    view("Human", action?.policyDecision === "AUTO" ? "NOT_APPLICABLE" : action?.policyDecision === "DENY" ? "SKIPPED" : action?.approvedAt ? "COMPLETE" : action?.status === "PENDING_APPROVAL" ? "CURRENT" : "PENDING", action?.approvedAt ? `Approval persisted ${action.approvedAt}` : "No persisted human decision"),
    view("Execute", ["FAILED", "CANCELLED"].includes(action?.status ?? "") ? "FAILED" : executeEvidence || action?.executedAt ? "COMPLETE" : action?.status === "EXECUTING" ? "CURRENT" : policyBlocked || governorBlock ? "BLOCKED" : "PENDING", executeEvidence?.narrative ?? (action?.executedAt ? `Executed ${action.executedAt}` : "No persisted execution event")),
    view("Accept", truth?.providerAccepted ? "COMPLETE" : action?.status === "EXECUTING" ? "CURRENT" : "PENDING", truth?.providerAccepted ? truth.basis : "No provider acceptance persisted"),
    view("Reconcile", truth?.providerConfirmed ? "COMPLETE" : truth?.awaitingReconciliation ? "CURRENT" : reconcileEvidence ? "CURRENT" : "PENDING", reconcileEvidence?.narrative ?? truth?.basis ?? "No reconciliation event persisted"),
    view("Learn", truth?.providerConfirmed ? "COMPLETE" : "PENDING", truth?.providerConfirmed ? "Provider-confirmed outcome is available for learning" : "Requires reconciled outcome"),
  ];
  return stages;
}

function view(label: typeof pipeline[number], state: PipelineStageState, evidence: string): PipelineStageView {
  return { label, state, evidence };
}
