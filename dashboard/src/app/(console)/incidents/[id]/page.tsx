"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Check, ExternalLink, LoaderCircle, Play, RefreshCw, ShieldCheck, X } from "lucide-react";
import { motion } from "motion/react";
import { toast } from "sonner";
import { api, money, shortId } from "@/lib/api";
import { mutationErrorMessage } from "@/lib/api-errors";
import { actionEligibility } from "@/lib/action-eligibility";
import { pipelineStates } from "@/lib/pipeline";
import { isTerminalOrPaused, nextRecoveryOperation, recoveryPollingInterval, sessionButtonLabel } from "@/lib/recovery-session";
import { useStatusIsland } from "@/components/providers";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { LiveExecutionLedger, LivePipeline } from "@/components/live-recovery";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { PartialState, TruthBadge } from "@/components/console-ui";

export default function IncidentDetailPage() {
  const id = useParams<{ id: string }>().id; const client = useQueryClient(); const { emit } = useStatusIsland();
  const [activeSession, setActiveSession] = useState(false);
  const [actor, setActor] = useState("");
  const [reason, setReason] = useState("");
  const detail = useQuery({ queryKey: ["incident", id], queryFn: () => api.incident(id), refetchInterval: (query) => recoveryPollingInterval(query.state.data, activeSession) });
  const audit = useQuery({ queryKey: ["audit", id], queryFn: () => api.audit(id), refetchInterval: () => recoveryPollingInterval(detail.data, activeSession) });
  const capsule = useQuery({ queryKey: ["evidence-capsule", id], queryFn: () => api.capsule(id), refetchInterval: () => recoveryPollingInterval(detail.data, activeSession) });
  const certificates = useQuery({ queryKey: ["decision-certificates", id], queryFn: () => api.decisionCertificates(id) });
  const counterfactuals = useQuery({ queryKey: ["counterfactuals", id], queryFn: () => api.counterfactuals(id) });
  const timing = useQuery({ queryKey: ["timing", id], queryFn: () => api.timingRecommendation(id) });
  const costs = useQuery({ queryKey: ["recovery-costs", id], queryFn: () => api.recoveryCosts(id) });
  const invalidate = async () => {
    const keys = [["incident", id], ["audit", id], ["evidence-capsule", id], ["decision-certificates", id], ["recovery-costs", id], ["incidents"], ["metrics"], ["financial-attribution"], ["lost-revenue"], ["approvals"], ["control-tower"]];
    await Promise.all(keys.map((queryKey) => client.invalidateQueries({ queryKey })));
    await Promise.all(keys.map((queryKey) => client.refetchQueries({ queryKey, type: "active" })));
  };
  const persistedState = async () => {
    await invalidate();
    const [currentDetail, currentAudit] = await Promise.all([
      client.fetchQuery({ queryKey: ["incident", id], queryFn: () => api.incident(id), staleTime: 0 }),
      client.fetchQuery({ queryKey: ["audit", id], queryFn: () => api.audit(id), staleTime: 0 }),
    ]);
    return { currentDetail, currentAudit };
  };
  const run = useMutation({
    mutationFn: async () => {
      let state = await persistedState();
      let executionResult: Awaited<ReturnType<typeof api.execute>> | null = null;
      for (let step = 0; step < 3; step += 1) {
        const operation = nextRecoveryOperation(state.currentDetail, state.currentAudit);
        if (!operation) return { state, executionResult };
        if (operation === "investigate") await api.investigate(id);
        if (operation === "plan") await api.plan(id);
        if (operation === "execute") executionResult = await api.execute(id);
        state = await persistedState();
        if (operation === "execute" || isTerminalOrPaused(state.currentDetail)) return { state, executionResult };
      }
      return { state, executionResult };
    },
    onMutate: () => setActiveSession(true),
    onSuccess: ({ state, executionResult }) => {
      setActiveSession(recoveryPollingInterval(state.currentDetail, false) !== false);
      const existing = executionResult?.existing === true;
      emit({ title: state.currentDetail.truth?.providerAccepted ? "Provider action accepted" : "Recovery state advanced", detail: `Incident ${shortId(id)} · persisted events refreshed` });
      toast.success(existing ? "Action already submitted. No duplicate provider action was sent." : state.currentDetail.truth?.providerAccepted ? "Provider accepted the action. Awaiting signed reconciliation." : "Recovery advanced to its current persisted gate.");
    },
    onError: async (error: unknown) => {
      await invalidate();
      setActiveSession(recoveryPollingInterval(detail.data, false) !== false);
      toast.error(mutationErrorMessage(error, "execute"));
    },
  });
  const decide = useMutation({
    mutationFn: (decision: "approve" | "reject") => api.decide(detail.data!.action!.actionId, decision, actor.trim(), reason.trim()),
    onSuccess: async (_, decision) => {
      await invalidate(); setActor(""); setReason("");
      emit({ title: decision === "approve" ? "Human approval persisted" : "Recovery denied", detail: `Incident ${shortId(id)} · immutable audit updated` });
      if (decision === "approve") run.mutate(); else setActiveSession(false);
    },
    onError: async (error: unknown, decision) => { await invalidate(); toast.error(mutationErrorMessage(error, decision)); },
  });
  const refresh = async () => { await Promise.all([detail.refetch(), audit.refetch(), capsule.refetch(), certificates.refetch(), counterfactuals.refetch(), timing.refetch(), costs.refetch()]); };
  if (detail.isLoading) return <div className="space-y-4"><Skeleton className="h-24 rounded-xl" /><Skeleton className="h-64 rounded-xl" /></div>;
  if (detail.error || !detail.data) return <ErrorState error={detail.error ?? new Error("Incident was not found")} retry={() => void detail.refetch()} />;
  const item = detail.data;
  const eligibility = actionEligibility(item, audit.data ?? []);
  const canRun = nextRecoveryOperation(item, audit.data ?? []) !== null;
  const stages = pipelineStates(item, audit.data ?? []);
  const stage = (label: (typeof stages)[number]["label"]) => stages.find((item) => item.label === label)!;
  const ruleTrace = [...new Set((audit.data ?? []).flatMap((entry) => entry.ruleTrace ?? []))];
  const diagnosis = item.findings.find((finding) => finding.source === "ROOT_CAUSE_AGENT");
  const policyEvent = (audit.data ?? []).find((entry) => entry.stage === "POLICY_DECISION" || entry.stage === "POLICY");
  const outcomeEvent = (audit.data ?? []).find((entry) => entry.stage === "OBSERVE");
  const recovered = item.incident.status === "RECOVERED";
  const policyVerdict = item.action?.policyDecision === "AUTO" ? "APPROVED" : item.action?.policyDecision === "HUMAN" ? "HUMAN_REQUIRED" : item.action?.policyDecision === "DENY" ? "DENIED" : null;
  const verdictTone = policyVerdict === "APPROVED" ? "text-[#22c55e]" : policyVerdict === "DENIED" ? "text-[#ef4444]" : policyVerdict === "HUMAN_REQUIRED" ? "text-[#f59e0b]" : "text-[#444444]";
  const actionExecuted = item.action && ["EXECUTED", "MONITORING", "RECOVERED"].includes(item.action.status);
  const watching = recoveryPollingInterval(item, activeSession) !== false;
  const reviewRequired = eligibility.kind === "HUMAN_REVIEW" && Boolean(item.action);
  const reviewValid = actor.trim().length > 1 && reason.trim().length > 4;
  return <div><Link href="/incidents" className="mb-4 inline-flex items-center gap-2 text-xs text-muted-foreground hover:text-foreground"><ArrowLeft className="size-3" /> Back to incidents</Link><PageHeader eyebrow={`Incident ${shortId(id)}`} title={item.incident.type.replaceAll("_", " ")} description={`${item.incident.severity} priority · ${item.incident.affectedPaymentCount} affected payments · detected ${new Date(item.incident.detectedAt).toLocaleString()}`} onRefresh={() => void refresh()} refreshing={detail.isFetching || audit.isFetching} updated={detail.dataUpdatedAt ? new Date(detail.dataUpdatedAt) : undefined} />
    <motion.div layoutId={`incident-${id}`} className={`glass-panel rounded-2xl p-4 sm:p-6 ${watching ? "border-primary/25" : ""}`}><div className="flex flex-wrap items-start justify-between gap-4"><div><div className="flex flex-wrap items-center gap-2"><span className="font-mono text-[10px] tracking-[0.2em] text-[#444444] uppercase">{shortId(id)}</span><StateBadge value={item.incident.status} />{watching && <span className="inline-flex items-center gap-2 font-mono text-[9px] text-primary"><span className="size-1.5 rounded-full bg-primary" />ACTIVE RECOVERY</span>}</div><div className="mt-4 flex flex-wrap items-baseline gap-x-3 gap-y-1"><p className={`font-mono text-2xl font-bold ${recovered ? "text-[#22c55e]" : "text-[#ef4444]"}`}>{money(item.incident.amountAtRiskMinor)}</p><p className="text-xs text-muted-foreground">at risk · {item.action ? "Razorpay" : "Gateway not specified"} · {new Date(item.incident.detectedAt).toLocaleString()}</p></div><span className="mt-3 inline-flex test-label">Test mode / Synthetic evaluation</span></div><div className="max-w-sm text-right"><Button onClick={() => run.mutate()} disabled={!canRun || run.isPending || decide.isPending} variant={canRun ? "default" : "outline"}>{run.isPending ? <LoaderCircle className="animate-spin" /> : <Play />}{sessionButtonLabel(item, audit.data ?? [], run.isPending)}</Button><p className="mt-2 text-xs leading-5 text-muted-foreground">{canRun ? "Sentinel will advance only through persisted eligible gates and stop at review, refusal, or provider truth." : eligibility.reason}</p></div></div>
      <LivePipeline stages={stages} />
    </motion.div>

    {reviewRequired && <section className="mt-6 rounded-2xl border border-amber-300/30 bg-amber-50/70 p-5"><div className="flex items-start gap-3"><ShieldCheck className="mt-0.5 size-5 text-amber-600" /><div><p className="font-mono text-[10px] tracking-[.18em] text-amber-700 uppercase">Human review interruption</p><h2 className="mt-2 text-lg font-semibold">Execution is paused before any provider action</h2><p className="mt-2 text-sm leading-6 text-slate-600">{item.plan?.reason ?? eligibility.reason}</p></div></div><div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4"><DetailValue label="Recommended action" value={item.plan?.strategy.replaceAll("_", " ") ?? "Unknown"} /><DetailValue label="Exposure" value={money(item.action?.amountMinor ?? 0)} /><DetailValue label="Evidence confidence" value={item.plan ? `${(item.plan.confidence * 100).toFixed(0)}%` : "Unknown"} /><DetailValue label="Governor" value={stages.find((stage) => stage.label === "Governor")?.state.replaceAll("_", " ") ?? "Not assessed"} /></div><div className="mt-5 grid gap-3 sm:grid-cols-2"><label className="space-y-2 text-xs font-medium">Actor identity<Input value={actor} onChange={(event) => setActor(event.target.value)} placeholder="ops-reviewer-01" /></label><label className="space-y-2 text-xs font-medium">Decision reason<Textarea value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Persist the accountable review reason" /></label></div><div className="mt-4 flex flex-wrap gap-2"><Button disabled={!reviewValid || decide.isPending} onClick={() => decide.mutate("approve")} className="border border-[#22c55e]/30 bg-[#22c55e]/10 text-[#15803d] hover:bg-[#22c55e]/15"><Check />{decide.isPending ? "Recording…" : "Approve and resume"}</Button><Button disabled={!reviewValid || decide.isPending} onClick={() => decide.mutate("reject")} className="border border-[#ef4444]/30 bg-[#ef4444]/10 text-[#dc2626] hover:bg-[#ef4444]/15"><X />Deny recovery</Button></div></section>}

    <section className="mt-8"><SectionLabel>Evidence</SectionLabel>{item.findings.length ? <div className="grid gap-3 lg:grid-cols-2">{item.findings.map((finding, index) => <div key={`${finding.source}-${index}`} className="glass-panel rounded-xl p-5"><div className="flex items-center justify-between gap-3"><span className="font-mono text-[10px] tracking-[0.16em] text-[#888888] uppercase">{finding.source.replaceAll("_", " ")}</span>{finding.confidence != null && <span className="font-mono text-[10px] text-primary">{(finding.confidence * 100).toFixed(0)}%</span>}</div><p className="mt-3 text-sm font-medium leading-6">{finding.summary}</p><ul className="mt-4 space-y-2">{finding.evidence.map((line) => <li key={line} className="flex gap-2 text-xs leading-5 text-muted-foreground"><Check className="mt-1 size-3 shrink-0 text-primary" />{line}</li>)}</ul></div>)}</div> : <PendingText>Awaiting evidence</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Decision evidence capsule</SectionLabel>{capsule.isLoading ? <Skeleton className="h-36 rounded-xl" /> : capsule.data ? <div className="glass-panel rounded-xl p-5"><div className="grid gap-5 sm:grid-cols-3"><DetailValue label="Evidence completeness" value={`${capsule.data.completeness.presentStages} / ${capsule.data.completeness.totalStages} stages`} /><DetailValue label="Provider truth" value={capsule.data.providerTruth.stage.replaceAll("_", " ")} /><DetailValue label="Final outcome" value={capsule.data.finalOutcome.replaceAll("_", " ")} /></div><div className="mt-5 flex flex-wrap gap-2">{capsule.data.agentClaims.map((claim) => <span key={claim.claimId} className={`border px-2 py-1 font-mono text-[9px] tracking-[0.12em] uppercase ${claim.validationStatus === "VALID" ? "border-[#22c55e]/30 text-[#22c55e]" : claim.validationStatus === "DOWNGRADED" ? "border-[#f59e0b]/30 text-[#f59e0b]" : "border-[#ef4444]/30 text-[#ef4444]"}`}>{claim.claimType.replaceAll("_", " ")} · {claim.validationStatus}</span>)}</div>{capsule.data.completeness.missingStages.length > 0 && <p className="mt-5 font-mono text-[10px] leading-5 text-[#444444]">AWAITING: {capsule.data.completeness.missingStages.join(" · ").replaceAll("_", " ")}</p>}<p className="mt-4 text-xs leading-5 text-muted-foreground">Provider payloads, signatures, payment details, and customer identifiers are deliberately excluded.</p></div> : <PendingText>Evidence capsule unavailable</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Decision certificates</SectionLabel>{certificates.isLoading ? <Skeleton className="h-32 rounded-xl" /> : certificates.data?.length ? <div className="space-y-3">{certificates.data.map((certificate) => <article key={certificate.id} className="glass-panel rounded-xl p-5"><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="font-mono text-[10px] uppercase tracking-[0.16em] text-primary">{certificate.decisionType.replaceAll("_", " ")}</p><p className="mt-2 text-sm font-semibold">{certificate.selectedAction.replaceAll("_", " ")}</p></div><StateBadge value={certificate.authorizationResult} /></div><div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4"><DetailValue label="Policy" value={certificate.policyVersion} /><DetailValue label="Model" value={certificate.modelVersion} /><DetailValue label="Features" value={certificate.featureSchemaVersion} /><DetailValue label="Strategy" value={certificate.strategyVersion} /></div><div className="mt-5 grid gap-4 sm:grid-cols-2"><DetailValue label="Economic evidence" value={`${certificate.counterfactualMethod.replaceAll("_", " ")} · ${certificate.evidenceQuality.replaceAll("_", " ")}`} /><DetailValue label="Final truth" value={certificate.finalTruthState.replaceAll("_", " ")} /></div><p className="mt-5 break-all font-mono text-[9px] text-[#444444]">CERTIFICATE SHA-256 {certificate.certificateSha256}</p></article>)}</div> : <PendingText>No immutable decision certificate has been issued</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Agent diagnosis</SectionLabel>{diagnosis ? <div className="glass-panel rounded-xl p-5"><p className="text-base font-medium leading-7 text-slate-900">{diagnosis.summary}</p>{diagnosis.confidence != null && <ConfidenceBar value={diagnosis.confidence} />}</div> : <StagePanel stage={stage("Diagnose")} waiting="Waiting for diagnosis" active="Diagnosis running" />}</section>
    <SectionDivider />
    <section><SectionLabel>Recovery proposal</SectionLabel>{item.plan ? <div className="glass-panel grid gap-5 rounded-xl p-5 sm:grid-cols-2 lg:grid-cols-4"><DetailValue label="Strategy" value={item.plan.strategy.replaceAll("_", " ")} /><DetailValue label="Proposed window" value="Not specified" /><DetailValue label="Route" value={item.plan.strategy.replaceAll("_", " ")} /><DetailValue label="Confidence" value={`${(item.plan.confidence * 100).toFixed(0)}%`} /></div> : <StagePanel stage={stage("Plan")} waiting="Awaiting persisted proposal" active="Recovery planning in progress" />}</section>
    <SectionDivider />
    <section><SectionLabel>Counterfactual and timing evidence</SectionLabel><div className="grid gap-3 lg:grid-cols-2"><div className="glass-panel rounded-xl p-5">{counterfactuals.data?.length ? counterfactuals.data.slice(0, 4).map((estimate) => <div key={estimate.action} className="border-b border-slate-200 py-3 last:border-0"><div className="flex items-center justify-between gap-2"><p className="font-mono text-[10px] font-semibold">{estimate.action.replaceAll("_", " ")}</p><TruthBadge label={estimate.evidenceQuality} /></div><p className="mt-2 text-xs text-slate-500">Estimated net incremental: {estimate.estimatedNetIncrementalValueMinor == null ? "Unknown" : money(estimate.estimatedNetIncrementalValueMinor)} · {estimate.method}</p></div>) : <PartialState title="No counterfactual evidence" detail="Sentinel does not infer causal lift when the engine returns no estimate." />}</div><div className="glass-panel rounded-xl p-5">{timing.data ? <><DetailValue label="Recommended action" value={timing.data.action.replaceAll("_", " ")} /><div className="mt-4"><DetailValue label="Provider window" value={timing.data.providerWindow} /></div><div className="mt-4"><DetailValue label="Authority" value={timing.data.authorityState} /></div><div className="mt-4"><TruthBadge label={timing.data.evidenceQuality} /></div></> : <PartialState title="No timing recommendation" detail="Timing remains unknown for this incident." />}</div></div></section>
    <SectionDivider />
    <section><SectionLabel>Policy decision</SectionLabel><div className="glass-panel rounded-xl p-5"><p className={`font-mono text-2xl font-bold tracking-widest ${verdictTone}`}>{policyVerdict ?? (stage("Policy").state === "ACTIVE" ? "EVALUATING" : "NOT EVALUATED")}</p><p className="mt-3 text-sm leading-6 text-muted-foreground">{policyEvent?.narrative ?? stage("Policy").evidence}</p>{ruleTrace.length > 0 && <div className="mt-5 space-y-2">{ruleTrace.map((rule) => <div key={rule} className="flex gap-3 border border-white/7 bg-white/[.02] p-3 text-xs"><Check className="size-4 shrink-0 text-primary" /><span>{rule}</span></div>)}</div>}</div></section>
    <SectionDivider />
    <section><SectionLabel>Recovery safety governor</SectionLabel><div className="glass-panel rounded-xl p-5"><div className="flex flex-wrap items-center gap-3"><StateBadge value={stage("Governor").state.replaceAll("_", " ")} />{stage("Governor").timestamp && <time className="font-mono text-[9px] text-slate-400">{new Date(stage("Governor").timestamp!).toLocaleString()}</time>}</div><p className="mt-3 text-sm leading-6 text-muted-foreground">{stage("Governor").evidence}</p></div></section>
    <SectionDivider />
    <section><SectionLabel>Execution</SectionLabel>{actionExecuted && item.action ? <div className="glass-panel rounded-xl p-5 font-mono text-xs"><p className="flex items-center gap-2 font-semibold text-[#22c55e]"><span className="size-1.5 rounded-full bg-[#22c55e]" />Provider action submitted</p><p className="mt-3 font-sans text-sm font-semibold text-amber-700">PROVIDER ACCEPTED · NOT RECOVERED YET</p><p className="mt-4 text-[#888888]">Payment Link: {item.action.providerId ?? item.action.referenceId ?? "Provider ID unavailable"}</p>{item.action.shortUrl && <a className="mt-2 inline-flex items-center gap-1 text-primary hover:underline" href={item.action.shortUrl} target="_blank" rel="noreferrer">Open Test Link <ExternalLink className="size-3" /></a>}<p className="mt-2 text-[#888888]">Executed at: {item.action.executedAt ? new Date(item.action.executedAt).toLocaleString() : "Timestamp unavailable"}</p></div> : <StagePanel stage={stage("Execute")} waiting="Awaiting execution authority" active="Submitting provider action" />}</section>
    <SectionDivider />
    <section><SectionLabel>Outcome and cost</SectionLabel>{outcomeEvent || recovered ? <div className="glass-panel rounded-xl p-5"><TruthBadge label="PROVIDER CONFIRMED" /><p className="mt-4 font-mono text-xs text-slate-500">{item.action?.providerStatus === "paid" ? "payment_link.paid" : "verified provider webhook"}</p><p className="mt-2 font-mono text-xs text-slate-500">signature verified <span className="text-[#22c55e]">✓</span></p><p className="mt-5 font-mono text-xl font-bold text-[#22c55e]">{money(item.incident.recoveredAmountMinor)} RECOVERED</p><p className="mt-3 text-xs text-slate-500">Persisted recovery costs: {money((costs.data ?? []).reduce((sum, cost) => sum + cost.amountMinor, 0))}</p></div> : <div className="glass-panel rounded-xl p-5"><TruthBadge label="AWAITING RECONCILIATION" /><PendingText>Provider acceptance is not recovered revenue</PendingText></div>}</section>
    <SectionDivider />
    <section><SectionLabel>Execution event ledger</SectionLabel><LiveExecutionLedger entries={audit.data ?? []} loading={audit.isLoading} watching={watching} /></section>
    {watching && <div className="mt-4 flex items-center gap-2 text-xs text-muted-foreground"><RefreshCw className={`size-3 ${detail.isFetching || audit.isFetching ? "animate-spin" : ""}`} />{item.truth?.awaitingReconciliation ? "Checking persisted provider truth every 6 seconds." : "Watching this active recovery every 1.5 seconds."} Polling stops at a terminal or human-held state.</div>}
  </div>;
}

function SectionLabel({ children }: { children: string }) {
  return <h2 className="mb-3 border-l-2 border-[#2563eb]/30 pl-3 font-mono text-[9px] tracking-[0.3em] text-[#444444] uppercase">{children}</h2>;
}

function SectionDivider() { return <div className="my-8 h-px w-full bg-slate-200/80" />; }

function PendingText({ children }: { children: string }) { return <p className="py-4 font-mono text-xs tracking-[0.16em] text-[#444444] uppercase">{children}</p>; }

function StagePanel({ stage, waiting, active }: { stage: ReturnType<typeof pipelineStates>[number]; waiting: string; active: string }) {
  const label = stage.state === "ACTIVE" ? active : ["BLOCKED", "FAILED", "HELD"].includes(stage.state) ? stage.evidence : waiting;
  return <div className="glass-panel rounded-xl p-5"><div className="flex items-center gap-2"><StateBadge value={stage.state.replaceAll("_", " ")} />{stage.timestamp && <time className="font-mono text-[9px] text-slate-400">{new Date(stage.timestamp).toLocaleString()}</time>}</div><p className="mt-3 font-mono text-xs tracking-[.1em] text-[#444444] uppercase">{label}</p></div>;
}

function DetailValue({ label, value }: { label: string; value: string }) { return <div><p className="font-mono text-[9px] tracking-[0.2em] text-slate-400 uppercase">{label}</p><p className="mt-2 text-sm text-slate-600">{value}</p></div>; }

function ConfidenceBar({ value }: { value: number }) {
  const percent = Math.max(0, Math.min(100, Math.round(value * 100)));
  return <div className="mt-5"><div className="mb-2 font-mono text-xs text-slate-500">{percent}% CONFIDENCE</div><div className="flex h-1.5 overflow-hidden rounded-full bg-slate-200" aria-label={`${percent}% confidence`}>{Array.from({ length: 100 }, (_, index) => <span key={index} className={`h-full flex-1 ${index < percent ? "bg-[#2563eb]" : "bg-transparent"}`} />)}</div></div>;
}
