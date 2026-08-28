import type { IncidentStatus } from "./types";

export const pipeline = ["Detect", "Investigate", "Plan", "Policy", "Execute", "Observe"] as const;
const progress: Record<IncidentStatus, number> = { DETECTED: 0, INVESTIGATING: 1, DIAGNOSED: 1, PLANNING: 2, POLICY_REVIEW: 3, HUMAN_REVIEW: 3, APPROVED: 3, EXECUTING: 4, MONITORING: 5, RECOVERED: 5, FAILED: 5, STOPPED: 5 };
export const progressFor = (status: IncidentStatus) => progress[status];
export const nextActionFor = (status: IncidentStatus) => status === "DETECTED" ? "investigate" : status === "DIAGNOSED" ? "plan" : status === "APPROVED" ? "execute" : null;
