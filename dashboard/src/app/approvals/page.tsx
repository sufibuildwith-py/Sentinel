"use client";

import Link from "next/link";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, Check, ShieldAlert, X } from "lucide-react";
import { toast } from "sonner";
import { api, money, shortId } from "@/lib/api";
import type { Approval } from "@/lib/types";
import { useStatusIsland } from "@/components/providers";
import { ErrorState, PageHeader } from "@/components/dashboard-ui";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";

export default function ApprovalsPage() {
  const query = useQuery({ queryKey: ["approvals"], queryFn: api.approvals });
  return <div><PageHeader eyebrow="Human safeguards" title="Approval queue" description="Actions that crossed a deterministic policy boundary wait here for an accountable human decision." onRefresh={() => void query.refetch()} refreshing={query.isFetching} updated={query.dataUpdatedAt ? new Date(query.dataUpdatedAt) : undefined} />
    <div className="mb-4"><span className="test-label">Test mode / Synthetic evaluation</span></div>
    {query.isLoading ? <div className="space-y-3"><Skeleton className="h-52 rounded-xl" /><Skeleton className="h-52 rounded-xl" /></div> : query.error ? <ErrorState error={query.error} retry={() => void query.refetch()} /> : query.data?.length ? <div className="grid gap-4 lg:grid-cols-2">{query.data.map((item) => <ApprovalCard key={item.actionId} item={item} />)}</div> : <div className="glass-panel rounded-xl p-12 text-center"><Check className="mx-auto size-7 text-emerald-300" /><h2 className="mt-3 font-semibold">Queue is clear</h2><p className="mt-2 text-sm text-muted-foreground">No recovery actions currently require human review.</p></div>}
  </div>;
}

function ApprovalCard({ item }: { item: Approval }) {
  const [decision, setDecision] = useState<"approve" | "reject" | null>(null); const [actor, setActor] = useState(""); const [reason, setReason] = useState("");
  const client = useQueryClient(); const { emit } = useStatusIsland();
  const mutation = useMutation({ mutationFn: () => api.decide(item.actionId, decision!, actor.trim(), reason.trim()), onSuccess: async () => { await Promise.all([client.invalidateQueries({ queryKey: ["approvals"] }), client.invalidateQueries({ queryKey: ["incidents"] }), client.invalidateQueries({ queryKey: ["incident", item.incidentId] }), client.invalidateQueries({ queryKey: ["audit", item.incidentId] })]); emit({ title: decision === "approve" ? "Action approved" : "Action rejected", detail: `${shortId(item.incidentId)} · actor persisted` }); toast.success(`Action ${decision === "approve" ? "approved" : "rejected"}`); setDecision(null); setActor(""); setReason(""); }, onError: (error: Error) => toast.error(error.message) });
  const valid = actor.trim().length > 1 && reason.trim().length > 4;
  return <div className="glass-panel rounded-2xl p-5 sm:p-6"><div className="flex items-start justify-between gap-4"><div><p className="eyebrow">Incident {shortId(item.incidentId)}</p><h2 className="mt-2 text-lg font-semibold">{item.incidentType.replaceAll("_", " ")}</h2></div><span className="rounded-full border border-amber-300/20 bg-amber-300/7 px-2.5 py-1 text-[11px] font-semibold text-amber-200">Review required</span></div>
    <div className="mt-6 grid grid-cols-2 gap-4 border-y border-white/7 py-4"><div><p className="eyebrow">Action amount</p><p className="mt-2 font-semibold">{money(item.amountMinor)}</p></div><div><p className="eyebrow">Confidence</p><p className="mt-2 font-semibold">{(item.confidence * 100).toFixed(0)}%</p></div></div>
    <p className="mt-4 text-sm leading-6 text-muted-foreground">{item.reason}</p><div className="mt-4 rounded-xl border border-amber-300/15 bg-amber-300/[.035] p-4"><p className="flex items-center gap-2 text-xs font-semibold"><ShieldAlert className="size-4 text-amber-200" /> Why policy stopped auto-action</p><ul className="mt-3 space-y-2">{item.failedPolicyRules.map((rule) => <li key={rule} className="text-xs leading-5 text-muted-foreground">• {rule}</li>)}</ul></div>
    <div className="mt-5 flex flex-wrap items-center gap-2"><Button onClick={() => setDecision("approve")}><Check /> Approve</Button><Button variant="destructive" onClick={() => setDecision("reject")}><X /> Reject</Button><Button nativeButton={false} variant="ghost" render={<Link href={`/incidents/${item.incidentId}`} />} className="ml-auto">View evidence <ArrowRight /></Button></div>
    <Dialog open={decision !== null} onOpenChange={(open) => !open && setDecision(null)}><DialogContent><DialogHeader><DialogTitle>{decision === "approve" ? "Approve recovery action?" : "Reject recovery action?"}</DialogTitle><DialogDescription>Your identity and reason are persisted in the immutable incident audit trail.</DialogDescription></DialogHeader><label className="space-y-2 text-xs font-medium">Actor identity<Input value={actor} onChange={(e) => setActor(e.target.value)} placeholder="e.g. ops-reviewer-01" autoFocus /></label><label className="space-y-2 text-xs font-medium">Decision reason<textarea value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Explain why this decision is safe and appropriate" className="min-h-24 w-full rounded-lg border border-input bg-background p-3 text-sm" /></label><DialogFooter><Button variant="outline" onClick={() => setDecision(null)}>Cancel</Button><Button variant={decision === "reject" ? "destructive" : "default"} disabled={!valid || mutation.isPending} onClick={() => mutation.mutate()}>{mutation.isPending ? "Recording…" : `Confirm ${decision}`}</Button></DialogFooter></DialogContent></Dialog>
  </div>;
}
