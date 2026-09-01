import { derivePrimaryRecoveryAction, normalizeRecoveryExecution } from "./recovery-execution-state";
import type { AuditEntry, IncidentDetail } from "./types";

export type RecoveryOperation = "investigate" | "plan" | "execute";
export type LedgerActor = "SYSTEM" | "AGENT" | "EVIDENCE" | "ECONOMICS" | "POLICY" | "GOVERNOR" | "HUMAN" | "TOOL" | "PROVIDER" | "RECONCILIATION" | "AUDIT";

export interface ExecutionLedgerRow {
  eventId: string;
  timestamp: string;
  actor: LedgerActor;
  sourceActor: string;
  stage: string;
  message: string;
  truthClass: "RAZORPAY_TEST_MODE" | "PROVIDER_CONFIRMED" | "AWAITING_RECONCILIATION" | "OBSERVATIONAL";
  evidence: string[];
}

export function nextRecoveryOperation(detail: IncidentDetail, audit: AuditEntry[]): RecoveryOperation | null {
  return derivePrimaryRecoveryAction(normalizeRecoveryExecution(detail, audit)).operation;
}

export function recoveryPollingInterval(detail: IncidentDetail | undefined, activeSession: boolean): number | false {
  if (!detail || isTerminalOrPaused(detail)) return false;
  if (detail.truth?.awaitingReconciliation || detail.truth?.stage === "PROVIDER_ACCEPTED") return 6_000;
  if (activeSession || ["INVESTIGATING", "PLANNING", "POLICY_REVIEW", "EXECUTING"].includes(detail.incident.status)) return 1_500;
  return false;
}

export function isTerminalOrPaused(detail: IncidentDetail): boolean {
  if (["RECOVERED", "FAILED", "STOPPED", "HUMAN_REVIEW"].includes(detail.incident.status)) return true;
  if (["REJECTED", "FAILED", "STOPPED", "CANCELLED", "PENDING_APPROVAL"].includes(detail.action?.status ?? "")) return true;
  return Boolean(detail.truth?.providerConfirmed);
}

export function sessionButtonLabel(detail: IncidentDetail, audit: AuditEntry[], pending: boolean): string {
  if (pending) return "RUNNING RECOVERY…";
  const eligibility = derivePrimaryRecoveryAction(normalizeRecoveryExecution(detail, audit));
  if (eligibility.operation) return eligibility.operation === "execute" && eligibility.label === "Resume recovery" ? "RESUME RECOVERY" : "RUN RECOVERY";
  if (eligibility.kind === "HUMAN_REVIEW") return "AWAITING HUMAN REVIEW";
  if (eligibility.kind === "POLICY_BLOCKED") return "BLOCKED BY POLICY";
  if (eligibility.kind === "GOVERNOR_BLOCKED") return "HELD BY GOVERNOR";
  if (["AWAITING_RECONCILIATION", "SUBMITTED"].includes(eligibility.kind)) return "AWAITING PROVIDER TRUTH";
  if (eligibility.kind === "PROVIDER_CONFIRMED") return "RECOVERY COMPLETE";
  return eligibility.label.toUpperCase();
}

export function mergeAuditEntries(previous: AuditEntry[], incoming: AuditEntry[]): AuditEntry[] {
  const byId = new Map(previous.map((entry) => [entry.eventId, entry]));
  incoming.forEach((entry) => byId.set(entry.eventId, entry));
  return [...byId.values()].sort((a, b) => a.timestamp.localeCompare(b.timestamp) || a.eventId.localeCompare(b.eventId));
}

export function executionLedger(entries: AuditEntry[]): ExecutionLedgerRow[] {
  return mergeAuditEntries([], entries).map((entry) => ({
    eventId: entry.eventId,
    timestamp: entry.timestamp,
    actor: ledgerActor(entry),
    sourceActor: entry.actor,
    stage: entry.stage,
    message: entry.narrative,
    truthClass: truthClass(entry),
    evidence: [...entry.evidence, ...entry.ruleTrace],
  }));
}

function ledgerActor(entry: AuditEntry): LedgerActor {
  const value = `${entry.actor} ${entry.stage}`.toUpperCase();
  if (/POLICY/.test(value)) return "POLICY";
  if (/GOVERNOR|BLAST_RADIUS/.test(value)) return "GOVERNOR";
  if (/HUMAN/.test(value)) return "HUMAN";
  if (/WEBHOOK|RECONCILIATION|\bOBSERVE\b|RECOVERY_(?:PARTIAL|METRIC|CANCELLED)/.test(value)) return "RECONCILIATION";
  if (/RAZORPAY/.test(value)) return "PROVIDER";
  if (/EXECUTOR|PROVIDER_REQUEST|EXECUTION_/.test(value)) return "TOOL";
  if (/OPPORTUNITY|COUNTERFACTUAL/.test(value)) return "ECONOMICS";
  if (/AGENT|TRIAGE|ANALYST|ROOT_CAUSE|PLANNER/.test(value)) return "AGENT";
  if (/EVIDENCE|INCIDENT_DETECTED|\bDETECT\b/.test(value)) return "EVIDENCE";
  if (/STATE_TRANSITION|SENTINEL/.test(value)) return "SYSTEM";
  return "AUDIT";
}

function truthClass(entry: AuditEntry): ExecutionLedgerRow["truthClass"] {
  const value = `${entry.actor} ${entry.stage} ${entry.narrative}`.toUpperCase();
  if (/RECOVERY_METRIC_UPDATED|HISTORICAL_MEMORY_RECORDED|SIGNED.*(?:PAID|RECOVER)/.test(value)) return "PROVIDER_CONFIRMED";
  if (/WEBHOOK|RECONCILIATION/.test(value)) return "AWAITING_RECONCILIATION";
  if (/RAZORPAY|PROVIDER_REQUEST|EXECUTION_/.test(value)) return "RAZORPAY_TEST_MODE";
  return "OBSERVATIONAL";
}
