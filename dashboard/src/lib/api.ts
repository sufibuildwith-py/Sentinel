import { fixtureApprovals, fixtureAudit, fixtureDetail, fixtureIncidents, fixtureMetrics } from "./fixtures";
import { fixtureEvaluation } from "./evaluation-fixture";
import type { Approval, AuditEntry, EvaluationReport, ExecutionResult, IncidentDetail, IncidentSummary, Metrics, PlanningResult } from "./types";

const API_URL = process.env.NEXT_PUBLIC_SENTINEL_API_URL ?? "http://localhost:8080";
const USE_FIXTURES = process.env.NEXT_PUBLIC_USE_FIXTURES === "true";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, { ...init, headers: { "Content-Type": "application/json", ...init?.headers } });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error((body as { message?: string }).message ?? `Sentinel API returned ${response.status}`);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

const delay = <T>(value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), 180));

export const api = {
  incidents: () => USE_FIXTURES ? delay(fixtureIncidents) : request<IncidentSummary[]>("/api/v1/revenue/incidents"),
  incident: (id: string) => USE_FIXTURES ? delay({ ...fixtureDetail, incident: fixtureIncidents.find((item) => item.incidentId === id) ?? fixtureDetail.incident }) : request<IncidentDetail>(`/api/v1/revenue/incidents/${id}`),
  approvals: () => USE_FIXTURES ? delay(fixtureApprovals) : request<Approval[]>("/api/v1/revenue/approvals"),
  metrics: async () => {
    if (USE_FIXTURES) return delay(fixtureMetrics);
    const [raw, incidents] = await Promise.all([request<Omit<Metrics, "activeIncidents" | "strategyPerformance"> & { strategyPerformance: { strategy: string; attemptedRecoveryMinor: number; recoveredRevenueMinor: number; recoveryRate: number; }[] }>("/api/v1/revenue/metrics"), request<IncidentSummary[]>("/api/v1/revenue/incidents")]);
    return { ...raw, activeIncidents: incidents.filter((item) => !["RECOVERED", "FAILED", "STOPPED"].includes(item.status)).length, strategyPerformance: raw.strategyPerformance.map((item) => ({ strategy: item.strategy.replaceAll("_", " "), attemptedMinor: item.attemptedRecoveryMinor, recoveredMinor: item.recoveredRevenueMinor, rate: item.recoveryRate })) } as Metrics;
  },
  audit: (id: string) => USE_FIXTURES ? delay(fixtureAudit) : request<AuditEntry[]>(`/api/v1/revenue/incidents/${id}/audit-trail`),
  investigate: (id: string) => request(`/api/v1/revenue/incidents/${id}/investigate`, { method: "POST" }),
  plan: (id: string) => request<PlanningResult>(`/api/v1/revenue/incidents/${id}/plan`, { method: "POST" }),
  execute: (incidentId: string) => request<ExecutionResult>(`/api/v1/revenue/incidents/${incidentId}/execute`, { method: "POST" }),
  decide: (actionId: string, decision: "approve" | "reject", actor: string, reason: string) => request(`/api/v1/revenue/actions/${actionId}/${decision}`, { method: "POST", body: JSON.stringify({ actor, reason }) }),
  reset: () => USE_FIXTURES ? delay({ reset: true }) : request("/api/v1/demo/reset", { method: "POST" }),
  inject: () => USE_FIXTURES ? delay({ incidentIds: [fixtureIncidents[0].incidentId] }) : request<{ incidentIds?: string[] }>("/api/v1/demo/inject/upi-outage", { method: "POST" }),
  evaluation: () => USE_FIXTURES ? delay(fixtureEvaluation) : request<EvaluationReport>("/api/v1/evaluation/report"),
  runEvaluation: () => USE_FIXTURES ? delay(fixtureEvaluation) : request<EvaluationReport>("/api/v1/evaluation/run", { method: "POST" }),
  evaluationDownloadUrl: (format: "json" | "md") => `${API_URL}/api/v1/evaluation/report.${format}`,
};

export const money = (minor: number) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", minimumFractionDigits: 2 }).format(minor / 100);
export const updatedAt = (date = new Date()) => date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
export const shortId = (id: string) => id.slice(0, 8).toUpperCase();
