"use client";

import Link from "next/link";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, BookOpen, CircleDollarSign, Clock3, Database, ShieldCheck } from "lucide-react";
import { api, money, shortId } from "@/lib/api";
import type { HistoricalValidationCaseResult, IncidentSummary } from "@/lib/types";
import { normalizeRecoveryExecution, derivePrimaryRecoveryAction } from "@/lib/recovery-execution-state";
import { derivePipelineStages } from "@/lib/pipeline";
import { recoveryPollingInterval } from "@/lib/recovery-session";
import { GovernedRecoveryGraph } from "@/components/governed-recovery-graph";
import { LiveExecutionLedger } from "@/components/live-recovery";
import { ConsolePanel, PartialState, TruthBadge } from "@/components/console-ui";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

type Selection = { kind: "OPERATIONAL"; id: string } | { kind: "HISTORICAL"; id: string };

export function RecoveryPortfolioBoard() {
  const incidents = useQuery({ queryKey: ["incidents"], queryFn: api.incidents });
  const historical = useQuery({ queryKey: ["historical-validation"], queryFn: api.historicalValidation, staleTime: 300_000 });
  const [selection, setSelection] = useState<Selection | null>(null);
  const effectiveSelection: Selection | null = selection
    ?? (incidents.data?.[0] ? { kind: "OPERATIONAL", id: incidents.data[0].incidentId }
      : historical.data?.cases[0] ? { kind: "HISTORICAL", id: historical.data.cases[0].caseId } : null);
  const selectedIncident = effectiveSelection?.kind === "OPERATIONAL" ? incidents.data?.find((item) => item.incidentId === effectiveSelection.id) : undefined;
  const selectedHistorical = effectiveSelection?.kind === "HISTORICAL" ? historical.data?.cases.find((item) => item.caseId === effectiveSelection.id) : undefined;
  const detail = useQuery({ queryKey: ["incident", selectedIncident?.incidentId], queryFn: () => api.incident(selectedIncident!.incidentId), enabled: Boolean(selectedIncident), refetchInterval: (query) => recoveryPollingInterval(query.state.data, false) });
  const audit = useQuery({ queryKey: ["audit", selectedIncident?.incidentId], queryFn: () => api.audit(selectedIncident!.incidentId), enabled: Boolean(selectedIncident), refetchInterval: () => recoveryPollingInterval(detail.data, false) });
  const refresh = () => { void incidents.refetch(); void historical.refetch(); if (selectedIncident) { void detail.refetch(); void audit.refetch(); } };
  const operationalValue = (incidents.data ?? []).filter((item) => !["RECOVERED", "FAILED", "STOPPED"].includes(item.status)).reduce((sum, item) => sum + item.amountAtRiskMinor, 0);

  if (incidents.isLoading || historical.isLoading) return <Skeleton className="h-[720px] rounded-2xl" />;
  if (incidents.error) return <ErrorState error={incidents.error} retry={refresh} />;
  return <div>
    <PageHeader eyebrow="Recovery" title="Recovery operations board" description="Select an operational incident or provenance-linked historical case. Only operational Test Mode incidents can reach provider execution." onRefresh={refresh} refreshing={incidents.isFetching || historical.isFetching || detail.isFetching} updated={incidents.dataUpdatedAt ? new Date(incidents.dataUpdatedAt) : undefined} />
    <section className="grid gap-3 sm:grid-cols-3"><PortfolioKpi icon={CircleDollarSign} label="Open operational exposure" value={money(operationalValue)} /><PortfolioKpi icon={ShieldCheck} label="Operational incidents" value={String(incidents.data?.length ?? 0)} /><PortfolioKpi icon={Database} label="Historical public cases" value={String(historical.data?.acceptedPublicSourceCases ?? 0)} /></section>

    <section className="mt-5"><div className="flex flex-wrap items-end justify-between gap-3"><div><p className="eyebrow">Recovery portfolio strip</p><h2 className="mt-2 text-lg font-semibold">Choose the evidence universe and case</h2></div><div className="flex gap-2"><TruthBadge label="RAZORPAY TEST MODE" /><TruthBadge label="HISTORICAL PUBLIC SOURCE" /></div></div>
      <div className="mt-4 flex snap-x gap-3 overflow-x-auto pb-3">
        {(incidents.data ?? []).map((incident) => <OperationalCard key={incident.incidentId} incident={incident} selected={effectiveSelection?.kind === "OPERATIONAL" && effectiveSelection.id === incident.incidentId} onSelect={() => setSelection({ kind: "OPERATIONAL", id: incident.incidentId })} />)}
        {(historical.data?.cases ?? []).slice(0, 10).map((item) => <HistoricalCard key={item.caseId} item={item} selected={effectiveSelection?.kind === "HISTORICAL" && effectiveSelection.id === item.caseId} onSelect={() => setSelection({ kind: "HISTORICAL", id: item.caseId })} />)}
      </div>
      {(historical.data?.cases.length ?? 0) > 10 && <div className="flex justify-end"><Button nativeButton={false} variant="ghost" size="sm" render={<Link href="/evaluation/historical" />}>Browse all {historical.data!.cases.length} historical cases <ArrowRight /></Button></div>}
    </section>

    {selectedIncident && <OperationalWorkbench incident={selectedIncident} detail={detail.data} audit={audit.data ?? []} loading={detail.isLoading || audit.isLoading} />}
    {selectedHistorical && <HistoricalWorkbench item={selectedHistorical} />}
    {!effectiveSelection && <PartialState title="No recovery cases available" detail="Reset synthetic state is empty. Inject the deterministic UPI scenario or browse the historical validation corpus." />}
  </div>;
}

function OperationalWorkbench({ incident, detail, audit, loading }: { incident: IncidentSummary; detail: Awaited<ReturnType<typeof api.incident>> | undefined; audit: Awaited<ReturnType<typeof api.audit>>; loading: boolean }) {
  if (loading || !detail) return <Skeleton className="mt-5 h-[620px] rounded-2xl" />;
  const normalized = normalizeRecoveryExecution(detail, audit);
  const primary = derivePrimaryRecoveryAction(normalized);
  const stages = derivePipelineStages(normalized);
  const diagnosis = detail.findings.find((finding) => finding.source === "ROOT_CAUSE_AGENT");
  const authority = detail.governor ? detail.governor.allowed ? "ALLOW" : "DENY" : "NOT EVALUATED";
  return <section className="mt-5 space-y-4" aria-label="Selected operational recovery workbench">
    <div className="glass-panel rounded-2xl p-5 sm:p-6"><div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-start"><div><div className="flex flex-wrap gap-2"><TruthBadge label={detail.truth?.executionMode ?? "RAZORPAY TEST MODE"} /><StateBadge value={incident.status} /></div><h2 className="mt-3 text-xl font-semibold">{incident.type.replaceAll("_", " ")}</h2><p className="mt-2 text-xs text-muted-foreground">Incident {shortId(incident.incidentId)} · {incident.affectedPaymentCount} payments · {money(incident.amountAtRiskMinor)} at risk</p></div><div className="max-w-md lg:text-right"><Button nativeButton={false} render={<Link href={`/incidents/${incident.incidentId}`} />}>Open governed workbench <ArrowRight /></Button><p className="mt-2 text-xs leading-5 text-muted-foreground">Current control: {primary.label}. {primary.reason}</p></div></div><div className="mt-6"><GovernedRecoveryGraph stages={stages} /></div></div>
    <div className="grid gap-4 xl:grid-cols-2">
      <ConsolePanel eyebrow="Diagnosis & evidence" title={diagnosis?.summary ?? "Diagnosis not persisted"}><EvidenceFact label="Confidence" value={diagnosis?.confidence == null ? "Not evaluated" : `${(diagnosis.confidence * 100).toFixed(0)}%`} /><EvidenceFact label="Affected cohort" value={`${incident.affectedPaymentCount} payments · ${incident.affectedCustomerCount} customers`} /><EvidenceFact label="Evidence records" value={String(detail.findings.length)} /></ConsolePanel>
      <ConsolePanel eyebrow="Authority & guardrails" title="Deterministic execution authority"><EvidenceFact label="Policy" value={detail.action?.policyDecision ?? "NOT EVALUATED"} /><EvidenceFact label="Human" value={normalized.humanReview.resolution} /><EvidenceFact label="Governor" value={authority} /><EvidenceFact label="Provider" value={normalized.provider.accepted ? "ACCEPTED · NOT RECOVERED" : "NOT SUBMITTED"} /></ConsolePanel>
      <div className="xl:col-span-2"><LiveExecutionLedger entries={audit} loading={false} watching={recoveryPollingInterval(detail, false) !== false} /></div>
    </div>
    <div className="glass-panel rounded-2xl p-5"><p className="eyebrow">Recovery outcome</p><div className="mt-3 flex flex-wrap items-center justify-between gap-4"><div><p className="text-xl font-semibold">{outcomeLabel(normalized)}</p><p className="mt-2 text-xs text-muted-foreground">{detail.truth?.basis ?? "No provider truth has been persisted."}</p></div><TruthBadge label={normalized.reconciliation.confirmed ? "PROVIDER CONFIRMED" : normalized.provider.accepted ? "AWAITING RECONCILIATION" : "OBSERVATIONAL"} /></div></div>
  </section>;
}

function HistoricalWorkbench({ item }: { item: HistoricalValidationCaseResult }) {
  return <section className="mt-5 grid gap-4 xl:grid-cols-[1.1fr_.9fr]"><ConsolePanel eyebrow="Historical public-source replay" title={item.normalizedFailureClass.replaceAll("_", " ")}><div className="flex flex-wrap gap-2"><TruthBadge label={item.evidenceLabel || "HISTORICAL PUBLIC SOURCE"} /><TruthBadge label="REPLAY / NO EXECUTION" /><StateBadge value={item.result} /></div><p className="mt-4 text-sm leading-6 text-slate-600">{item.normalizedFailureReason}</p><div className="mt-5 grid gap-4 sm:grid-cols-2"><EvidenceFact label="Case" value={item.caseId} /><EvidenceFact label="Product surface" value={item.productSurface.replaceAll("_", " ")} /><EvidenceFact label="Payment rail" value={item.paymentRail.replaceAll("_", " ")} /><EvidenceFact label="Provider state" value={item.providerState.replaceAll("_", " ")} /><EvidenceFact label="Policy disposition" value={item.policyDisposition} /><EvidenceFact label="Expected behavior" value={item.expectedBehaviorClass.replaceAll("_", " ")} /><EvidenceFact label="Financial outcome" value={item.outcomeKnown ? "SOURCE DOCUMENTED" : "UNKNOWN · NO INR CLAIM"} /><EvidenceFact label="Safe refusal" value={item.safeRefusal ? "YES" : "NO"} /></div><Button nativeButton={false} className="mt-5" variant="outline" render={<Link href="/evaluation/historical" />}>Open replay evidence <BookOpen /></Button></ConsolePanel><ConsolePanel eyebrow="Expected invariant" title="Zero provider and customer tool path"><ul className="space-y-3">{item.expectedInvariants.map((line) => <li key={line} className="flex gap-2 text-xs leading-5 text-muted-foreground"><ShieldCheck className="mt-0.5 size-3.5 shrink-0 text-primary" />{line.replaceAll("_", " ")}</li>)}</ul><p className="mt-5 border-t border-slate-200 pt-4 text-xs text-muted-foreground">This is a provenance-linked public report transformed into a deterministic replay case. It is not a merchant transaction and cannot invoke Razorpay or customer communication.</p></ConsolePanel></section>;
}

function OperationalCard({ incident, selected, onSelect }: { incident: IncidentSummary; selected: boolean; onSelect: () => void }) {
  const state = incident.status === "RECOVERED" ? "RECOVERED" : incident.policyDecision === "DENY" ? "POLICY BLOCKED" : incident.actionStatus === "PENDING_APPROVAL" ? "REVIEW REQUIRED" : incident.status === "MONITORING" ? "AWAITING RECONCILIATION" : "READY";
  return <button type="button" onClick={onSelect} aria-pressed={selected} className={cn("min-w-[260px] snap-start rounded-xl border bg-white/75 p-4 text-left shadow-sm transition-colors", selected ? "border-primary/50 bg-primary/[.04]" : "border-slate-200 hover:border-primary/25")}><div className="flex items-center justify-between gap-2"><TruthBadge label="OPERATIONAL · TEST MODE" /><StateBadge value={incident.status} /></div><p className="mt-4 font-mono text-[10px] text-primary">{shortId(incident.incidentId)}</p><p className="mt-2 text-sm font-semibold">{incident.type.replaceAll("_", " ")}</p><div className="mt-4 flex items-end justify-between gap-4"><div><p className="font-mono text-base font-bold">{money(incident.amountAtRiskMinor)}</p><p className="mt-1 font-mono text-[8px] text-slate-400">{incident.affectedPaymentCount} PAYMENTS</p></div><span className="font-mono text-[8px] text-slate-500">{state}</span></div></button>;
}

function HistoricalCard({ item, selected, onSelect }: { item: HistoricalValidationCaseResult; selected: boolean; onSelect: () => void }) {
  return <button type="button" onClick={onSelect} aria-pressed={selected} className={cn("min-w-[260px] snap-start rounded-xl border bg-white/75 p-4 text-left shadow-sm transition-colors", selected ? "border-cyan-400/60 bg-cyan-50/60" : "border-slate-200 hover:border-cyan-300")}><div className="flex items-center justify-between gap-2"><TruthBadge label="HISTORICAL PUBLIC SOURCE" /><StateBadge value={item.result} /></div><p className="mt-4 font-mono text-[10px] text-cyan-700">{item.caseId}</p><p className="mt-2 text-sm font-semibold">{item.normalizedFailureClass.replaceAll("_", " ")}</p><p className="mt-4 font-mono text-[8px] text-slate-500">REPLAY ONLY · {item.productSurface.replaceAll("_", " ")}</p></button>;
}

function PortfolioKpi({ icon: Icon, label, value }: { icon: typeof Clock3; label: string; value: string }) { return <div className="glass-panel rounded-xl p-5"><Icon className="size-4 text-primary" /><p className="mt-5 text-2xl font-semibold tracking-tight">{value}</p><p className="mt-1 font-mono text-[9px] tracking-[.14em] text-slate-400 uppercase">{label}</p></div>; }
function EvidenceFact({ label, value }: { label: string; value: string }) { return <div className="border-b border-slate-200 py-3 last:border-0"><p className="font-mono text-[9px] tracking-[.14em] text-slate-400 uppercase">{label}</p><p className="mt-1 text-sm text-slate-700">{value}</p></div>; }
function outcomeLabel(state: ReturnType<typeof normalizeRecoveryExecution>) { if (state.reconciliation.confirmed) return "Provider confirmed recovered"; if (state.provider.awaitingReconciliation) return "Provider accepted · not recovered yet"; if (state.governor.resolution === "DENIED") return "Held by governor"; if (state.policy.resolution === "DENIED") return "Blocked by policy"; if (state.humanReview.resolution === "PENDING") return "Awaiting human review"; return "Not executed"; }
