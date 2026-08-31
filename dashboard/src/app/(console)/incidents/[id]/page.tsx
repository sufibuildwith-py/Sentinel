"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Check, Circle, ExternalLink, Play, RefreshCw } from "lucide-react";
import { motion } from "motion/react";
import { toast } from "sonner";
import { api, money, shortId } from "@/lib/api";
import { pipeline, progressFor } from "@/lib/pipeline";
import { useStatusIsland } from "@/components/providers";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

export default function IncidentDetailPage() {
  const id = useParams<{ id: string }>().id; const client = useQueryClient(); const { emit } = useStatusIsland();
  const detail = useQuery({ queryKey: ["incident", id], queryFn: () => api.incident(id), refetchInterval: (query) => ["EXECUTING", "MONITORING"].includes(query.state.data?.incident.status ?? "") ? 8_000 : false });
  const audit = useQuery({ queryKey: ["audit", id], queryFn: () => api.audit(id), refetchInterval: detail.data?.incident.status === "MONITORING" ? 8_000 : false });
  const capsule = useQuery({ queryKey: ["evidence-capsule", id], queryFn: () => api.capsule(id), refetchInterval: detail.data?.incident.status === "MONITORING" ? 8_000 : false });
  const invalidate = async () => { await Promise.all([client.invalidateQueries({ queryKey: ["incident", id] }), client.invalidateQueries({ queryKey: ["audit", id] }), client.invalidateQueries({ queryKey: ["evidence-capsule", id] }), client.invalidateQueries({ queryKey: ["incidents"] }), client.invalidateQueries({ queryKey: ["metrics"] }), client.invalidateQueries({ queryKey: ["approvals"] })]); };
  const run = useMutation({ mutationFn: async (kind: "investigate" | "plan" | "execute") => kind === "investigate" ? api.investigate(id) : kind === "plan" ? api.plan(id) : api.execute(id), onSuccess: async (_, kind) => { await invalidate(); const titles = { investigate: "Incident diagnosed", plan: "Recovery plan evaluated", execute: "Payment Link created" }; emit({ title: titles[kind], detail: `Incident ${shortId(id)} advanced` }); toast.success(titles[kind]); }, onError: (error: Error) => toast.error(error.message) });
  const refresh = () => { void detail.refetch(); void audit.refetch(); void capsule.refetch(); };
  if (detail.isLoading) return <div className="space-y-4"><Skeleton className="h-24 rounded-xl" /><Skeleton className="h-64 rounded-xl" /></div>;
  if (detail.error || !detail.data) return <ErrorState error={detail.error ?? new Error("Incident was not found")} retry={() => void detail.refetch()} />;
  const item = detail.data; const step = progressFor(item.incident.status); const next = item.incident.status === "DETECTED" ? "investigate" : item.incident.status === "DIAGNOSED" ? "plan" : item.incident.status === "APPROVED" ? "execute" : null;
  const ruleTrace = (audit.data ?? []).flatMap((entry) => entry.ruleTrace ?? []);
  const diagnosis = item.findings.find((finding) => finding.source === "ROOT_CAUSE_AGENT");
  const policyEvent = (audit.data ?? []).find((entry) => entry.stage === "POLICY");
  const outcomeEvent = (audit.data ?? []).find((entry) => entry.stage === "OBSERVE");
  const recovered = item.incident.status === "RECOVERED" || item.incident.latestOutcome === "RECOVERED";
  const policyVerdict = item.action?.policyDecision === "AUTO" ? "APPROVED" : item.action?.policyDecision === "HUMAN" ? "HUMAN_REQUIRED" : item.action?.policyDecision === "DENY" ? "DENIED" : null;
  const verdictTone = policyVerdict === "APPROVED" ? "text-[#22c55e]" : policyVerdict === "DENIED" ? "text-[#ef4444]" : policyVerdict === "HUMAN_REQUIRED" ? "text-[#f59e0b]" : "text-[#444444]";
  const actionExecuted = item.action && ["EXECUTED", "MONITORING", "RECOVERED"].includes(item.action.status);
  return <div><Link href="/incidents" className="mb-4 inline-flex items-center gap-2 text-xs text-muted-foreground hover:text-foreground"><ArrowLeft className="size-3" /> Back to incidents</Link><PageHeader eyebrow={`Incident ${shortId(id)}`} title={item.incident.type.replaceAll("_", " ")} description={`${item.incident.severity} priority · ${item.incident.affectedPaymentCount} affected payments · detected ${new Date(item.incident.detectedAt).toLocaleString()}`} onRefresh={refresh} refreshing={detail.isFetching || audit.isFetching} updated={detail.dataUpdatedAt ? new Date(detail.dataUpdatedAt) : undefined} />
    <motion.div layoutId={`incident-${id}`} className="glass-panel rounded-2xl p-4 sm:p-6"><div className="flex flex-wrap items-start justify-between gap-4"><div><div className="flex flex-wrap items-center gap-2"><span className="font-mono text-[10px] tracking-[0.2em] text-[#444444] uppercase">{shortId(id)}</span><StateBadge value={item.incident.status} /></div><div className="mt-4 flex flex-wrap items-baseline gap-x-3 gap-y-1"><p className={`font-mono text-2xl font-bold ${recovered ? "text-[#22c55e]" : "text-[#ef4444]"}`}>{money(item.incident.amountAtRiskMinor)}</p><p className="text-xs text-muted-foreground">at risk · {item.action ? "Razorpay" : "Gateway not specified"} · {new Date(item.incident.detectedAt).toLocaleString()}</p></div><span className="mt-3 inline-flex test-label">Test mode / Synthetic evaluation</span></div>{next && <Button onClick={() => run.mutate(next)} disabled={run.isPending}><Play />{run.isPending ? "Working…" : next === "investigate" ? "Run investigation" : next === "plan" ? "Build recovery plan" : "Create Payment Link"}</Button>}</div>
      <ol className="mt-8 grid grid-cols-3 gap-y-5 sm:grid-cols-6" aria-label="Incident pipeline">{pipeline.map((label, index) => <li key={label} className="relative flex flex-col items-center text-center"><div className="absolute left-0 right-0 top-3 hidden h-px bg-white/10 sm:block" /><motion.span initial={false} animate={{ scale: index === step ? 1.08 : 1 }} className={`relative z-10 grid size-7 place-items-center rounded-full border ${index <= step ? "border-primary/50 bg-primary text-white" : "border-white/10 bg-card text-muted-foreground"}`}>{index < step ? <Check className="size-3.5" /> : <Circle className="size-2.5" />}</motion.span><span className={`mt-2 text-[11px] font-medium ${index <= step ? "text-foreground" : "text-muted-foreground"}`}>{label}</span></li>)}</ol>
    </motion.div>

    <section className="mt-8"><SectionLabel>Evidence</SectionLabel>{item.findings.length ? <div className="grid gap-3 lg:grid-cols-2">{item.findings.map((finding, index) => <div key={`${finding.source}-${index}`} className="glass-panel rounded-xl p-5"><div className="flex items-center justify-between gap-3"><span className="font-mono text-[10px] tracking-[0.16em] text-[#888888] uppercase">{finding.source.replaceAll("_", " ")}</span>{finding.confidence != null && <span className="font-mono text-[10px] text-primary">{(finding.confidence * 100).toFixed(0)}%</span>}</div><p className="mt-3 text-sm font-medium leading-6">{finding.summary}</p><ul className="mt-4 space-y-2">{finding.evidence.map((line) => <li key={line} className="flex gap-2 text-xs leading-5 text-muted-foreground"><Check className="mt-1 size-3 shrink-0 text-primary" />{line}</li>)}</ul></div>)}</div> : <PendingText>Awaiting evidence</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Decision evidence capsule</SectionLabel>{capsule.isLoading ? <Skeleton className="h-36 rounded-xl" /> : capsule.data ? <div className="glass-panel rounded-xl p-5"><div className="grid gap-5 sm:grid-cols-3"><DetailValue label="Evidence completeness" value={`${capsule.data.completeness.presentStages} / ${capsule.data.completeness.totalStages} stages`} /><DetailValue label="Provider truth" value={capsule.data.providerTruth.stage.replaceAll("_", " ")} /><DetailValue label="Final outcome" value={capsule.data.finalOutcome.replaceAll("_", " ")} /></div><div className="mt-5 flex flex-wrap gap-2">{capsule.data.agentClaims.map((claim) => <span key={claim.claimId} className={`border px-2 py-1 font-mono text-[9px] tracking-[0.12em] uppercase ${claim.validationStatus === "VALID" ? "border-[#22c55e]/30 text-[#22c55e]" : claim.validationStatus === "DOWNGRADED" ? "border-[#f59e0b]/30 text-[#f59e0b]" : "border-[#ef4444]/30 text-[#ef4444]"}`}>{claim.claimType.replaceAll("_", " ")} · {claim.validationStatus}</span>)}</div>{capsule.data.completeness.missingStages.length > 0 && <p className="mt-5 font-mono text-[10px] leading-5 text-[#444444]">AWAITING: {capsule.data.completeness.missingStages.join(" · ").replaceAll("_", " ")}</p>}<p className="mt-4 text-xs leading-5 text-muted-foreground">Provider payloads, signatures, payment details, and customer identifiers are deliberately excluded.</p></div> : <PendingText>Evidence capsule unavailable</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Agent diagnosis</SectionLabel>{diagnosis ? <div className="glass-panel rounded-xl p-5"><p className="text-base font-medium leading-7 text-[#f5f5f5]">{diagnosis.summary}</p>{diagnosis.confidence != null && <ConfidenceBar value={diagnosis.confidence} />}</div> : <PendingText>Investigation in progress</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Recovery proposal</SectionLabel>{item.plan ? <div className="glass-panel grid gap-5 rounded-xl p-5 sm:grid-cols-2 lg:grid-cols-4"><DetailValue label="Strategy" value={item.plan.strategy.replaceAll("_", " ")} /><DetailValue label="Proposed window" value="Not specified" /><DetailValue label="Route" value={item.plan.strategy.replaceAll("_", " ")} /><DetailValue label="Confidence" value={`${(item.plan.confidence * 100).toFixed(0)}%`} /></div> : <PendingText>Awaiting proposal</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Policy decision</SectionLabel><div className="glass-panel rounded-xl p-5"><p className={`font-mono text-2xl font-bold tracking-widest ${verdictTone}`}>{policyVerdict ?? "EVALUATING"}</p><p className="mt-3 text-sm leading-6 text-muted-foreground">{policyEvent?.narrative ?? "Policy evaluation has not produced a persisted decision."}</p>{ruleTrace.length > 0 && <div className="mt-5 space-y-2">{ruleTrace.map((rule) => <div key={rule} className="flex gap-3 border border-white/7 bg-white/[.02] p-3 text-xs"><Check className="size-4 shrink-0 text-primary" /><span>{rule}</span></div>)}</div>}</div></section>
    <SectionDivider />
    <section><SectionLabel>Execution</SectionLabel>{actionExecuted && item.action ? <div className="glass-panel rounded-xl p-5 font-mono text-xs"><p className="flex items-center gap-2 font-semibold text-[#22c55e]"><span className="size-1.5 rounded-full bg-[#22c55e]" />Executed</p><p className="mt-4 text-[#888888]">Payment Link: {item.action.providerId ?? item.action.referenceId ?? "Provider ID unavailable"}</p>{item.action.shortUrl && <a className="mt-2 inline-flex items-center gap-1 text-primary hover:underline" href={item.action.shortUrl} target="_blank" rel="noreferrer">Open Test Link <ExternalLink className="size-3" /></a>}<p className="mt-2 text-[#888888]">Executed at: {item.action.executedAt ? new Date(item.action.executedAt).toLocaleString() : "Timestamp unavailable"}</p></div> : <PendingText>Awaiting execution</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Outcome</SectionLabel>{outcomeEvent || recovered ? <div className="glass-panel rounded-xl p-5"><p className="font-mono text-xs text-[#888888]">{item.action?.providerStatus === "paid" ? "payment_link.paid" : "verified provider webhook"}</p><p className="mt-2 font-mono text-xs text-[#888888]">signature verified <span className="text-[#22c55e]">✓</span></p><p className="mt-5 font-mono text-xl font-bold text-[#22c55e]">{money(item.incident.recoveredAmountMinor)} RECOVERED</p></div> : <PendingText>Awaiting webhook</PendingText>}</section>
    <SectionDivider />
    <section><SectionLabel>Audit trail</SectionLabel><AuditTimeline entries={audit.data ?? []} loading={audit.isLoading} /></section>
    {item.incident.status === "MONITORING" && <div className="mt-4 flex items-center gap-2 text-xs text-muted-foreground"><RefreshCw className="size-3" />Polling every 8 seconds while awaiting a signed outcome. This is not a streaming feed.</div>}
  </div>;
}

function SectionLabel({ children }: { children: string }) {
  return <h2 className="mb-3 border-l-2 border-[#2563eb]/30 pl-3 font-mono text-[9px] tracking-[0.3em] text-[#444444] uppercase">{children}</h2>;
}

function SectionDivider() { return <div className="my-8 h-px w-full bg-white/[0.04]" />; }

function PendingText({ children }: { children: string }) { return <p className="py-4 font-mono text-xs tracking-[0.16em] text-[#444444] uppercase">{children}</p>; }

function DetailValue({ label, value }: { label: string; value: string }) { return <div><p className="font-mono text-[9px] tracking-[0.2em] text-[#444444] uppercase">{label}</p><p className="mt-2 text-sm text-[#888888]">{value}</p></div>; }

function ConfidenceBar({ value }: { value: number }) {
  const percent = Math.max(0, Math.min(100, Math.round(value * 100)));
  return <div className="mt-5"><div className="mb-2 font-mono text-xs text-[#888888]">{percent}% CONFIDENCE</div><div className="flex h-1.5 overflow-hidden bg-white/[0.06]" aria-label={`${percent}% confidence`}>{Array.from({ length: 100 }, (_, index) => <span key={index} className={`h-full flex-1 ${index < percent ? "bg-[#2563eb]" : "bg-transparent"}`} />)}</div></div>;
}

function AuditTimeline({ entries, loading }: { entries: Awaited<ReturnType<typeof api.audit>>; loading: boolean }) {
  if (loading) return <Skeleton className="h-48 rounded-xl" />;
  if (!entries.length) return <PendingText>No audit entries</PendingText>;
  return <div className="glass-panel rounded-xl p-5"><ol className="space-y-0">{entries.map((entry, index) => <li key={entry.eventId} className="relative grid grid-cols-[20px_1fr] gap-3 pb-6 last:pb-0"><div className="flex flex-col items-center"><span className="mt-1 size-2 rounded-full bg-primary" />{index < entries.length - 1 && <span className="mt-1 h-full w-px bg-white/10" />}</div><div><div className="flex flex-wrap items-center gap-2 font-mono text-xs text-[#444444]"><span>{new Date(entry.timestamp).toLocaleString()}</span><span className="text-[#888888]">{entry.stage}</span><span>{entry.actor}</span>{entry.policyResult && <StateBadge value={entry.policyResult} />}</div><p className="mt-1 font-mono text-xs leading-5 text-[#444444]">{entry.narrative}</p></div></li>)}</ol></div>;
}
