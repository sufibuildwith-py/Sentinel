"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Check, Circle, ExternalLink, Play, RefreshCw } from "lucide-react";
import { motion } from "motion/react";
import { toast } from "sonner";
import { api, money, shortId } from "@/lib/api";
import { mutationErrorMessage } from "@/lib/api-errors";
import { actionEligibility, shouldPollIncident } from "@/lib/action-eligibility";
import { pipeline, pipelineStates } from "@/lib/pipeline";
import { useStatusIsland } from "@/components/providers";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { PartialState, TruthBadge } from "@/components/console-ui";

export default function IncidentDetailPage() {
  const id = useParams<{ id: string }>().id; const client = useQueryClient(); const { emit } = useStatusIsland();
  const [selectedStage, setSelectedStage] = useState<number | null>(null);
  const detail = useQuery({ queryKey: ["incident", id], queryFn: () => api.incident(id), refetchInterval: (query) => shouldPollIncident(query.state.data) ? 8_000 : false });
  const audit = useQuery({ queryKey: ["audit", id], queryFn: () => api.audit(id), refetchInterval: shouldPollIncident(detail.data) ? 8_000 : false });
  const capsule = useQuery({ queryKey: ["evidence-capsule", id], queryFn: () => api.capsule(id), refetchInterval: shouldPollIncident(detail.data) ? 8_000 : false });
  const certificates = useQuery({ queryKey: ["decision-certificates", id], queryFn: () => api.decisionCertificates(id) });
  const counterfactuals = useQuery({ queryKey: ["counterfactuals", id], queryFn: () => api.counterfactuals(id) });
  const timing = useQuery({ queryKey: ["timing", id], queryFn: () => api.timingRecommendation(id) });
  const costs = useQuery({ queryKey: ["recovery-costs", id], queryFn: () => api.recoveryCosts(id) });
  const invalidate = async () => {
    const keys = [["incident", id], ["audit", id], ["evidence-capsule", id], ["decision-certificates", id], ["recovery-costs", id], ["incidents"], ["metrics"], ["financial-attribution"], ["lost-revenue"], ["approvals"], ["control-tower"]];
    await Promise.all(keys.map((queryKey) => client.invalidateQueries({ queryKey })));
    await Promise.all(keys.map((queryKey) => client.refetchQueries({ queryKey, type: "active" })));
  };
  const run = useMutation({
    mutationFn: async (kind: "investigate" | "plan" | "execute") => {
      if (kind !== "execute") return kind === "investigate" ? api.investigate(id) : api.plan(id);
      const [currentDetail, currentAudit] = await Promise.all([detail.refetch(), audit.refetch()]);
      if (!currentDetail.data) throw new Error("The current incident state could not be loaded. No provider action was sent.");
      const eligibility = actionEligibility(currentDetail.data, currentAudit.data ?? []);
      if (!eligibility.executable) throw new Error(`${eligibility.label}. ${eligibility.reason} No provider action was sent.`);
      return api.execute(id);
    },
    onSuccess: async (result, kind) => {
      await invalidate();
      const existing = kind === "execute" && typeof result === "object" && result !== null && "existing" in result && result.existing === true;
      const titles = { investigate: "Incident diagnosed", plan: "Recovery plan evaluated", execute: existing ? "Persisted provider action loaded" : "Payment Link created" };
      emit({ title: titles[kind], detail: `Incident ${shortId(id)} advanced from persisted state` });
      toast.success(existing ? "Action already submitted. No duplicate provider action was sent." : titles[kind]);
    },
    onError: async (error: unknown, kind) => {
      if (kind === "execute") await invalidate();
      toast.error(mutationErrorMessage(error, kind === "execute" ? "execute" : "generic"));
    },
  });
  const refresh = async () => { await Promise.all([detail.refetch(), audit.refetch(), capsule.refetch(), certificates.refetch(), counterfactuals.refetch(), timing.refetch(), costs.refetch()]); };
  if (detail.isLoading) return <div className="space-y-4"><Skeleton className="h-24 rounded-xl" /><Skeleton className="h-64 rounded-xl" /></div>;
  if (detail.error || !detail.data) return <ErrorState error={detail.error ?? new Error("Incident was not found")} retry={() => void detail.refetch()} />;
  const item = detail.data;
  const eligibility = actionEligibility(item, audit.data ?? []);
  const next = item.incident.status === "DETECTED" ? "investigate" : item.incident.status === "DIAGNOSED" ? "plan" : eligibility.executable ? "execute" : null;
  const stages = pipelineStates(item, audit.data ?? []);
  const ruleTrace = [...new Set((audit.data ?? []).flatMap((entry) => entry.ruleTrace ?? []))];
  const diagnosis = item.findings.find((finding) => finding.source === "ROOT_CAUSE_AGENT");
  const policyEvent = (audit.data ?? []).find((entry) => entry.stage === "POLICY_DECISION" || entry.stage === "POLICY");
  const outcomeEvent = (audit.data ?? []).find((entry) => entry.stage === "OBSERVE");
  const recovered = item.incident.status === "RECOVERED";
  const policyVerdict = item.action?.policyDecision === "AUTO" ? "APPROVED" : item.action?.policyDecision === "HUMAN" ? "HUMAN_REQUIRED" : item.action?.policyDecision === "DENY" ? "DENIED" : null;
  const verdictTone = policyVerdict === "APPROVED" ? "text-[#22c55e]" : policyVerdict === "DENIED" ? "text-[#ef4444]" : policyVerdict === "HUMAN_REQUIRED" ? "text-[#f59e0b]" : "text-[#444444]";
  const actionExecuted = item.action && ["EXECUTED", "MONITORING", "RECOVERED"].includes(item.action.status);
  return <div><Link href="/incidents" className="mb-4 inline-flex items-center gap-2 text-xs text-muted-foreground hover:text-foreground"><ArrowLeft className="size-3" /> Back to incidents</Link><PageHeader eyebrow={`Incident ${shortId(id)}`} title={item.incident.type.replaceAll("_", " ")} description={`${item.incident.severity} priority · ${item.incident.affectedPaymentCount} affected payments · detected ${new Date(item.incident.detectedAt).toLocaleString()}`} onRefresh={() => void refresh()} refreshing={detail.isFetching || audit.isFetching} updated={detail.dataUpdatedAt ? new Date(detail.dataUpdatedAt) : undefined} />
    <motion.div layoutId={`incident-${id}`} className="glass-panel rounded-2xl p-4 sm:p-6"><div className="flex flex-wrap items-start justify-between gap-4"><div><div className="flex flex-wrap items-center gap-2"><span className="font-mono text-[10px] tracking-[0.2em] text-[#444444] uppercase">{shortId(id)}</span><StateBadge value={item.incident.status} /></div><div className="mt-4 flex flex-wrap items-baseline gap-x-3 gap-y-1"><p className={`font-mono text-2xl font-bold ${recovered ? "text-[#22c55e]" : "text-[#ef4444]"}`}>{money(item.incident.amountAtRiskMinor)}</p><p className="text-xs text-muted-foreground">at risk · {item.action ? "Razorpay" : "Gateway not specified"} · {new Date(item.incident.detectedAt).toLocaleString()}</p></div><span className="mt-3 inline-flex test-label">Test mode / Synthetic evaluation</span></div><div className="max-w-sm text-right">{next ? <Button onClick={() => run.mutate(next)} disabled={run.isPending}><Play />{run.isPending ? "Checking persisted state…" : next === "investigate" ? "Run investigation" : next === "plan" ? "Build recovery plan" : eligibility.label}</Button> : <><StateBadge value={eligibility.label} /><p className="mt-2 text-xs leading-5 text-muted-foreground">{eligibility.reason}</p></>}</div></div>
      <ol className="mt-8 grid grid-cols-3 gap-y-5 sm:grid-cols-6 xl:grid-cols-12" aria-label="Incident pipeline">{pipeline.map((label, index) => { const state = stages[index]; const active = state.state === "COMPLETE" || state.state === "CURRENT"; return <li key={label} className="relative flex flex-col items-center text-center"><div className="absolute left-0 right-0 top-3 hidden h-px bg-slate-200 sm:block" /><button type="button" aria-label={`${label}: ${state.state.replaceAll("_", " ")}`} aria-pressed={selectedStage === index} onClick={() => setSelectedStage(index)} className="relative z-10 flex flex-col items-center"><motion.span initial={false} animate={{ scale: state.state === "CURRENT" ? 1.08 : 1 }} className={`grid size-7 place-items-center rounded-full border ${state.state === "BLOCKED" || state.state === "FAILED" ? "border-red-300 bg-red-50 text-red-600" : active ? "border-primary/50 bg-primary text-white" : "border-slate-200 bg-card text-muted-foreground"}`}>{state.state === "COMPLETE" ? <Check className="size-3.5" /> : <Circle className="size-2.5" />}</motion.span><span className={`mt-2 text-[10px] font-medium ${active ? "text-foreground" : "text-muted-foreground"}`}>{label}</span><span className="mt-1 font-mono text-[7px] text-slate-400">{state.state.replaceAll("_", " ")}</span></button></li>; })}</ol>
      {selectedStage != null && <div className="mt-5 border-t border-slate-200 pt-4" aria-live="polite"><p className="font-mono text-[9px] tracking-[0.2em] text-primary uppercase">{stages[selectedStage].label} · {stages[selectedStage].state.replaceAll("_", " ")}</p><p className="mt-2 text-xs leading-5 text-muted-foreground">{stages[selectedStage].evidence}</p></div>}
    </motion.div>

    <section className="mt-8"><SectionLabel>Evidence</SectionLabel>{item.findings.length ? <div className="grid gap-3 lg:grid-cols-2">{item.findings.map((finding, index) => <div key={`${finding.source}-${index}`} className="glass-panel rounded-xl p-5"><div className="flex items-center justify-between gap-3"><span className="font-mono text-[10px] tracking-[0.16em] text-[#888888] uppercase">{finding.source.replaceAll("_", " ")}</span>{finding.confidence != null && <span className="font-mono text-[10px] text-primary">{(finding.confidence * 100).toFixed(0)}%</span>}</div><p className="mt-3 text-sm font-medium leading-6">{finding.summary}</p><ul className="mt-4 space-y-2">{finding.evidence.map((line) => <li key={line} className="flex gap-2 text-xs leading-5 text-muted-foreground"><Check className="mt-1 size-3 shrink-0 text-primary" />{line}</li>)}</ul></div>)}</div> : <PendingText>Awaiting evidence</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Decision evidence capsule</SectionLabel>{capsule.isLoading ? <Skeleton className="h-36 rounded-xl" /> : capsule.data ? <div className="glass-panel rounded-xl p-5"><div className="grid gap-5 sm:grid-cols-3"><DetailValue label="Evidence completeness" value={`${capsule.data.completeness.presentStages} / ${capsule.data.completeness.totalStages} stages`} /><DetailValue label="Provider truth" value={capsule.data.providerTruth.stage.replaceAll("_", " ")} /><DetailValue label="Final outcome" value={capsule.data.finalOutcome.replaceAll("_", " ")} /></div><div className="mt-5 flex flex-wrap gap-2">{capsule.data.agentClaims.map((claim) => <span key={claim.claimId} className={`border px-2 py-1 font-mono text-[9px] tracking-[0.12em] uppercase ${claim.validationStatus === "VALID" ? "border-[#22c55e]/30 text-[#22c55e]" : claim.validationStatus === "DOWNGRADED" ? "border-[#f59e0b]/30 text-[#f59e0b]" : "border-[#ef4444]/30 text-[#ef4444]"}`}>{claim.claimType.replaceAll("_", " ")} · {claim.validationStatus}</span>)}</div>{capsule.data.completeness.missingStages.length > 0 && <p className="mt-5 font-mono text-[10px] leading-5 text-[#444444]">AWAITING: {capsule.data.completeness.missingStages.join(" · ").replaceAll("_", " ")}</p>}<p className="mt-4 text-xs leading-5 text-muted-foreground">Provider payloads, signatures, payment details, and customer identifiers are deliberately excluded.</p></div> : <PendingText>Evidence capsule unavailable</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Decision certificates</SectionLabel>{certificates.isLoading ? <Skeleton className="h-32 rounded-xl" /> : certificates.data?.length ? <div className="space-y-3">{certificates.data.map((certificate) => <article key={certificate.id} className="glass-panel rounded-xl p-5"><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="font-mono text-[10px] uppercase tracking-[0.16em] text-primary">{certificate.decisionType.replaceAll("_", " ")}</p><p className="mt-2 text-sm font-semibold">{certificate.selectedAction.replaceAll("_", " ")}</p></div><StateBadge value={certificate.authorizationResult} /></div><div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4"><DetailValue label="Policy" value={certificate.policyVersion} /><DetailValue label="Model" value={certificate.modelVersion} /><DetailValue label="Features" value={certificate.featureSchemaVersion} /><DetailValue label="Strategy" value={certificate.strategyVersion} /></div><div className="mt-5 grid gap-4 sm:grid-cols-2"><DetailValue label="Economic evidence" value={`${certificate.counterfactualMethod.replaceAll("_", " ")} · ${certificate.evidenceQuality.replaceAll("_", " ")}`} /><DetailValue label="Final truth" value={certificate.finalTruthState.replaceAll("_", " ")} /></div><p className="mt-5 break-all font-mono text-[9px] text-[#444444]">CERTIFICATE SHA-256 {certificate.certificateSha256}</p></article>)}</div> : <PendingText>No immutable decision certificate has been issued</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Agent diagnosis</SectionLabel>{diagnosis ? <div className="glass-panel rounded-xl p-5"><p className="text-base font-medium leading-7 text-slate-900">{diagnosis.summary}</p>{diagnosis.confidence != null && <ConfidenceBar value={diagnosis.confidence} />}</div> : <PendingText>Investigation in progress</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Recovery proposal</SectionLabel>{item.plan ? <div className="glass-panel grid gap-5 rounded-xl p-5 sm:grid-cols-2 lg:grid-cols-4"><DetailValue label="Strategy" value={item.plan.strategy.replaceAll("_", " ")} /><DetailValue label="Proposed window" value="Not specified" /><DetailValue label="Route" value={item.plan.strategy.replaceAll("_", " ")} /><DetailValue label="Confidence" value={`${(item.plan.confidence * 100).toFixed(0)}%`} /></div> : <PendingText>Awaiting proposal</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Counterfactual and timing evidence</SectionLabel><div className="grid gap-3 lg:grid-cols-2"><div className="glass-panel rounded-xl p-5">{counterfactuals.data?.length ? counterfactuals.data.slice(0, 4).map((estimate) => <div key={estimate.action} className="border-b border-slate-200 py-3 last:border-0"><div className="flex items-center justify-between gap-2"><p className="font-mono text-[10px] font-semibold">{estimate.action.replaceAll("_", " ")}</p><TruthBadge label={estimate.evidenceQuality} /></div><p className="mt-2 text-xs text-slate-500">Estimated net incremental: {estimate.estimatedNetIncrementalValueMinor == null ? "Unknown" : money(estimate.estimatedNetIncrementalValueMinor)} · {estimate.method}</p></div>) : <PartialState title="No counterfactual evidence" detail="Sentinel does not infer causal lift when the engine returns no estimate." />}</div><div className="glass-panel rounded-xl p-5">{timing.data ? <><DetailValue label="Recommended action" value={timing.data.action.replaceAll("_", " ")} /><div className="mt-4"><DetailValue label="Provider window" value={timing.data.providerWindow} /></div><div className="mt-4"><DetailValue label="Authority" value={timing.data.authorityState} /></div><div className="mt-4"><TruthBadge label={timing.data.evidenceQuality} /></div></> : <PartialState title="No timing recommendation" detail="Timing remains unknown for this incident." />}</div></div></section>
    <SectionDivider />
    <section><SectionLabel>Policy decision</SectionLabel><div className="glass-panel rounded-xl p-5"><p className={`font-mono text-2xl font-bold tracking-widest ${verdictTone}`}>{policyVerdict ?? "EVALUATING"}</p><p className="mt-3 text-sm leading-6 text-muted-foreground">{policyEvent?.narrative ?? "Policy evaluation has not produced a persisted decision."}</p>{ruleTrace.length > 0 && <div className="mt-5 space-y-2">{ruleTrace.map((rule) => <div key={rule} className="flex gap-3 border border-white/7 bg-white/[.02] p-3 text-xs"><Check className="size-4 shrink-0 text-primary" /><span>{rule}</span></div>)}</div>}</div></section>
    <SectionDivider />
    <section><SectionLabel>Execution</SectionLabel>{actionExecuted && item.action ? <div className="glass-panel rounded-xl p-5 font-mono text-xs"><p className="flex items-center gap-2 font-semibold text-[#22c55e]"><span className="size-1.5 rounded-full bg-[#22c55e]" />Executed</p><p className="mt-4 text-[#888888]">Payment Link: {item.action.providerId ?? item.action.referenceId ?? "Provider ID unavailable"}</p>{item.action.shortUrl && <a className="mt-2 inline-flex items-center gap-1 text-primary hover:underline" href={item.action.shortUrl} target="_blank" rel="noreferrer">Open Test Link <ExternalLink className="size-3" /></a>}<p className="mt-2 text-[#888888]">Executed at: {item.action.executedAt ? new Date(item.action.executedAt).toLocaleString() : "Timestamp unavailable"}</p></div> : <PendingText>Awaiting execution</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Outcome and cost</SectionLabel>{outcomeEvent || recovered ? <div className="glass-panel rounded-xl p-5"><TruthBadge label="PROVIDER CONFIRMED" /><p className="mt-4 font-mono text-xs text-slate-500">{item.action?.providerStatus === "paid" ? "payment_link.paid" : "verified provider webhook"}</p><p className="mt-2 font-mono text-xs text-slate-500">signature verified <span className="text-[#22c55e]">✓</span></p><p className="mt-5 font-mono text-xl font-bold text-[#22c55e]">{money(item.incident.recoveredAmountMinor)} RECOVERED</p><p className="mt-3 text-xs text-slate-500">Persisted recovery costs: {money((costs.data ?? []).reduce((sum, cost) => sum + cost.amountMinor, 0))}</p></div> : <div className="glass-panel rounded-xl p-5"><TruthBadge label="AWAITING RECONCILIATION" /><PendingText>Provider acceptance is not recovered revenue</PendingText></div>}</section>
    <SectionDivider />
    <section><SectionLabel>Audit trail</SectionLabel><AuditTimeline entries={audit.data ?? []} loading={audit.isLoading} /></section>
    {shouldPollIncident(item) && <div className="mt-4 flex items-center gap-2 text-xs text-muted-foreground"><RefreshCw className="size-3" />Polling every 8 seconds while persisted execution or reconciliation state can change. Polling stops at a terminal state.</div>}
  </div>;
}

function SectionLabel({ children }: { children: string }) {
  return <h2 className="mb-3 border-l-2 border-[#2563eb]/30 pl-3 font-mono text-[9px] tracking-[0.3em] text-[#444444] uppercase">{children}</h2>;
}

function SectionDivider() { return <div className="my-8 h-px w-full bg-slate-200/80" />; }

function PendingText({ children }: { children: string }) { return <p className="py-4 font-mono text-xs tracking-[0.16em] text-[#444444] uppercase">{children}</p>; }

function DetailValue({ label, value }: { label: string; value: string }) { return <div><p className="font-mono text-[9px] tracking-[0.2em] text-slate-400 uppercase">{label}</p><p className="mt-2 text-sm text-slate-600">{value}</p></div>; }

function ConfidenceBar({ value }: { value: number }) {
  const percent = Math.max(0, Math.min(100, Math.round(value * 100)));
  return <div className="mt-5"><div className="mb-2 font-mono text-xs text-slate-500">{percent}% CONFIDENCE</div><div className="flex h-1.5 overflow-hidden rounded-full bg-slate-200" aria-label={`${percent}% confidence`}>{Array.from({ length: 100 }, (_, index) => <span key={index} className={`h-full flex-1 ${index < percent ? "bg-[#2563eb]" : "bg-transparent"}`} />)}</div></div>;
}

function AuditTimeline({ entries, loading }: { entries: Awaited<ReturnType<typeof api.audit>>; loading: boolean }) {
  if (loading) return <Skeleton className="h-48 rounded-xl" />;
  if (!entries.length) return <PendingText>No audit entries</PendingText>;
  return <div className="glass-panel rounded-xl p-5"><ol className="space-y-0">{entries.map((entry, index) => <li key={entry.eventId} className="relative grid grid-cols-[20px_1fr] gap-3 pb-6 last:pb-0"><div className="flex flex-col items-center"><span className="mt-1 size-2 rounded-full bg-primary" />{index < entries.length - 1 && <span className="mt-1 h-full w-px bg-white/10" />}</div><div><div className="flex flex-wrap items-center gap-2 font-mono text-xs text-[#444444]"><span>{new Date(entry.timestamp).toLocaleString()}</span><span className="text-[#888888]">{entry.stage}</span><span>{entry.actor}</span>{entry.policyResult && <StateBadge value={entry.policyResult} />}</div><p className="mt-1 font-mono text-xs leading-5 text-[#444444]">{entry.narrative}</p></div></li>)}</ol></div>;
}
