import type { IncidentStatus } from "./types";

export const pipeline = ["Detect", "Triage", "Evidence", "Diagnose", "Counterfactual", "Policy", "Governor", "Human", "Execute", "Accept", "Reconcile", "Learn"] as const;
const progress: Record<IncidentStatus, number> = { DETECTED: 0, INVESTIGATING: 2, DIAGNOSED: 3, PLANNING: 4, POLICY_REVIEW: 5, HUMAN_REVIEW: 7, APPROVED: 7, EXECUTING: 8, MONITORING: 10, RECOVERED: 11, FAILED: 10, STOPPED: 6 };
export const progressFor = (status: IncidentStatus) => progress[status];
export const nextActionFor = (status: IncidentStatus) => status === "DETECTED" ? "investigate" : status === "DIAGNOSED" ? "plan" : status === "APPROVED" ? "execute" : null;
