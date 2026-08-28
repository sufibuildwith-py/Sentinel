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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

export default function IncidentDetailPage() {
  const id = useParams<{ id: string }>().id; const client = useQueryClient(); const { emit } = useStatusIsland();
  const detail = useQuery({ queryKey: ["incident", id], queryFn: () => api.incident(id), refetchInterval: (query) => ["EXECUTING", "MONITORING"].includes(query.state.data?.incident.status ?? "") ? 8_000 : false });
  const audit = useQuery({ queryKey: ["audit", id], queryFn: () => api.audit(id), refetchInterval: detail.data?.incident.status === "MONITORING" ? 8_000 : false });
  const invalidate = async () => { await Promise.all([client.invalidateQueries({ queryKey: ["incident", id] }), client.invalidateQueries({ queryKey: ["audit", id] }), client.invalidateQueries({ queryKey: ["incidents"] }), client.invalidateQueries({ queryKey: ["metrics"] }), client.invalidateQueries({ queryKey: ["approvals"] })]); };
  const run = useMutation({ mutationFn: async (kind: "investigate" | "plan" | "execute") => kind === "investigate" ? api.investigate(id) : kind === "plan" ? api.plan(id) : api.execute(id), onSuccess: async (_, kind) => { await invalidate(); const titles = { investigate: "Incident diagnosed", plan: "Recovery plan evaluated", execute: "Payment Link created" }; emit({ title: titles[kind], detail: `Incident ${shortId(id)} advanced` }); toast.success(titles[kind]); }, onError: (error: Error) => toast.error(error.message) });
  const refresh = () => { void detail.refetch(); void audit.refetch(); };
  if (detail.isLoading) return <div className="space-y-4"><Skeleton className="h-24 rounded-xl" /><Skeleton className="h-64 rounded-xl" /></div>;
  if (detail.error || !detail.data) return <ErrorState error={detail.error ?? new Error("Incident was not found")} retry={() => void detail.refetch()} />;
  const item = detail.data; const step = progressFor(item.incident.status); const next = item.incident.status === "DETECTED" ? "investigate" : item.incident.status === "DIAGNOSED" ? "plan" : item.incident.status === "APPROVED" ? "execute" : null;
  const ruleTrace = (audit.data ?? []).flatMap((entry) => entry.ruleTrace ?? []);
  return <div><Link href="/incidents" className="mb-4 inline-flex items-center gap-2 text-xs text-muted-foreground hover:text-foreground"><ArrowLeft className="size-3" /> Back to incidents</Link><PageHeader eyebrow={`Incident ${shortId(id)}`} title={item.incident.type.replaceAll("_", " ")} description={`${item.incident.severity} severity · ${money(item.incident.amountAtRiskMinor)} at risk · ${item.incident.affectedPaymentCount} affected payments`} onRefresh={refresh} refreshing={detail.isFetching || audit.isFetching} updated={detail.dataUpdatedAt ? new Date(detail.dataUpdatedAt) : undefined} />
    <motion.div layoutId={`incident-${id}`} className="glass-panel rounded-2xl p-4 sm:p-6"><div className="flex flex-wrap items-center justify-between gap-3"><div className="flex items-center gap-2"><StateBadge value={item.incident.status} /><span className="test-label">Test mode / Synthetic evaluation</span></div>{next && <Button onClick={() => run.mutate(next)} disabled={run.isPending}><Play />{run.isPending ? "Working…" : next === "investigate" ? "Run investigation" : next === "plan" ? "Build recovery plan" : "Create Payment Link"}</Button>}</div>
      <ol className="mt-8 grid grid-cols-3 gap-y-5 sm:grid-cols-6" aria-label="Incident pipeline">{pipeline.map((label, index) => <li key={label} className="relative flex flex-col items-center text-center"><div className="absolute left-0 right-0 top-3 hidden h-px bg-white/10 sm:block" /><motion.span initial={false} animate={{ scale: index === step ? 1.08 : 1 }} className={`relative z-10 grid size-7 place-items-center rounded-full border ${index <= step ? "border-primary/50 bg-primary text-white" : "border-white/10 bg-card text-muted-foreground"}`}>{index < step ? <Check className="size-3.5" /> : <Circle className="size-2.5" />}</motion.span><span className={`mt-2 text-[11px] font-medium ${index <= step ? "text-foreground" : "text-muted-foreground"}`}>{label}</span></li>)}</ol>
    </motion.div>
    <Tabs defaultValue="evidence" className="mt-4"><TabsList><TabsTrigger value="evidence">Evidence</TabsTrigger><TabsTrigger value="policy">Policy trace</TabsTrigger><TabsTrigger value="action">Action & link</TabsTrigger><TabsTrigger value="audit">Audit trail</TabsTrigger></TabsList>
      <TabsContent value="evidence" className="mt-3 grid gap-3 lg:grid-cols-2">{item.findings.map((finding, index) => <div key={`${finding.source}-${index}`} className="glass-panel rounded-xl p-5"><div className="flex items-center justify-between"><span className="eyebrow">{finding.source.replaceAll("_", " ")}</span>{finding.confidence != null && <span className="text-xs font-semibold text-primary">{(finding.confidence * 100).toFixed(0)}% confidence</span>}</div><p className="mt-3 text-sm font-medium leading-6">{finding.summary}</p><ul className="mt-4 space-y-2">{finding.evidence.map((line) => <li key={line} className="flex gap-2 text-xs leading-5 text-muted-foreground"><Check className="mt-1 size-3 shrink-0 text-primary" />{line}</li>)}</ul></div>)}</TabsContent>
      <TabsContent value="policy" className="mt-3"><div className="glass-panel rounded-xl p-5"><div className="flex items-center justify-between"><div><p className="eyebrow">Deterministic decision</p><h2 className="mt-2 font-semibold">Mandatory stops evaluated first</h2></div><StateBadge value={item.action?.policyDecision} /></div><div className="mt-5 space-y-2">{ruleTrace.length ? ruleTrace.map((rule) => <div key={rule} className="flex gap-3 rounded-lg border border-white/7 bg-white/[.02] p-3 text-xs"><Check className="size-4 shrink-0 text-primary" /><span>{rule}</span></div>) : <p className="text-sm text-muted-foreground">Policy has not been evaluated yet.</p>}</div></div></TabsContent>
      <TabsContent value="action" className="mt-3"><div className="glass-panel rounded-xl p-5">{item.action ? <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4"><div><p className="eyebrow">Action state</p><div className="mt-2"><StateBadge value={item.action.status} /></div></div><div><p className="eyebrow">Amount</p><p className="mt-2 font-semibold">{money(item.action.amountMinor)}</p></div><div><p className="eyebrow">Provider</p><p className="mt-2 text-sm">Razorpay · {item.action.providerStatus ?? "Not created"}</p></div><div><p className="eyebrow">Payment link</p>{item.action.shortUrl ? <a className="mt-2 inline-flex items-center gap-1 text-sm text-primary hover:underline" href={item.action.shortUrl} target="_blank" rel="noreferrer">Open Test Link <ExternalLink className="size-3" /></a> : <p className="mt-2 text-sm text-muted-foreground">Unavailable</p>}</div></div> : <p className="text-sm text-muted-foreground">No recovery action has been proposed.</p>}</div></TabsContent>
      <TabsContent value="audit" className="mt-3"><AuditTimeline entries={audit.data ?? []} loading={audit.isLoading} /></TabsContent>
    </Tabs>
    {item.incident.status === "MONITORING" && <div className="mt-4 flex items-center gap-2 text-xs text-muted-foreground"><RefreshCw className="size-3" />Polling every 8 seconds while awaiting a signed outcome. This is not a streaming feed.</div>}
  </div>;
}

function AuditTimeline({ entries, loading }: { entries: Awaited<ReturnType<typeof api.audit>>; loading: boolean }) {
  if (loading) return <Skeleton className="h-48 rounded-xl" />;
  return <div className="glass-panel rounded-xl p-5"><ol className="space-y-0">{entries.map((entry, index) => <li key={entry.eventId} className="relative grid grid-cols-[20px_1fr] gap-3 pb-6 last:pb-0"><div className="flex flex-col items-center"><span className="mt-1 size-2 rounded-full bg-primary" />{index < entries.length - 1 && <span className="mt-1 h-full w-px bg-white/10" />}</div><div><div className="flex flex-wrap items-center gap-2"><span className="text-xs font-semibold">{entry.stage}</span><span className="text-[11px] text-muted-foreground">{new Date(entry.timestamp).toLocaleString()} · {entry.actor}</span>{entry.policyResult && <StateBadge value={entry.policyResult} />}</div><p className="mt-1 text-sm leading-6 text-muted-foreground">{entry.narrative}</p></div></li>)}</ol></div>;
}
