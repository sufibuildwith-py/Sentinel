"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, CircleDollarSign, ShieldCheck, Sparkles } from "lucide-react";
import { api, money, shortId } from "@/lib/api";
import { ConsolePanel, PartialState, TruthBadge } from "@/components/console-ui";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

export default function RecoveryPage() {
  const incidents = useQuery({ queryKey: ["incidents"], queryFn: api.incidents });
  if (incidents.isLoading) return <Skeleton className="h-[560px] rounded-2xl" />;
  if (incidents.error) return <ErrorState error={incidents.error} retry={() => void incidents.refetch()} />;
  const active = (incidents.data ?? []).filter((item) => !["RECOVERED", "FAILED", "STOPPED"].includes(item.status));
  return <div><PageHeader eyebrow="Recovery" title="Governed recovery workbench" description="Move from evidence to a policy-bounded recovery action. Provider acceptance is never presented as recovered revenue." onRefresh={() => void incidents.refetch()} refreshing={incidents.isFetching} updated={incidents.dataUpdatedAt ? new Date(incidents.dataUpdatedAt) : undefined} />
    <div className="mb-5 flex flex-wrap gap-2"><TruthBadge label="RAZORPAY TEST MODE" /><TruthBadge label="AWAITING RECONCILIATION" /><TruthBadge label="POLICY AUTHORITY REQUIRED" /></div>
    <section className="grid gap-4 sm:grid-cols-3"><Mini icon={Sparkles} label="Open opportunities" value={String(active.length)} /><Mini icon={CircleDollarSign} label="Value under review" value={money(active.reduce((sum, item) => sum + item.amountAtRiskMinor, 0))} /><Mini icon={ShieldCheck} label="Execution rule" value="Policy + governor" /></section>
    <ConsolePanel eyebrow="Operator queue" title="Select an incident to open the complete recovery trace" className="mt-4">
      {active.length === 0 ? <PartialState title="No open recovery work" detail="Sentinel is monitoring. No unresolved incident is currently eligible for operator review." /> : <div className="grid gap-3 lg:grid-cols-2">{active.map((incident) => <article key={incident.incidentId} className="rounded-xl border border-slate-200 bg-white/70 p-4 shadow-sm"><div className="flex items-start justify-between gap-3"><div><p className="font-mono text-[10px] text-primary">{shortId(incident.incidentId)}</p><h3 className="mt-2 font-semibold text-slate-900">{incident.type.replaceAll("_", " ")}</h3></div><StateBadge value={incident.status} /></div><div className="mt-5 grid grid-cols-2 gap-4"><Fact label="At risk" value={money(incident.amountAtRiskMinor)} /><Fact label="Current strategy" value={incident.strategy?.replaceAll("_", " ") ?? "Not proposed"} /></div><Button nativeButton={false} render={<Link href={`/incidents/${incident.incidentId}`} />} className="mt-5 w-full">Open workbench <ArrowRight /></Button></article>)}</div>}
    </ConsolePanel>
  </div>;
}
function Mini({ icon: Icon, label, value }: { icon: typeof Sparkles; label: string; value: string }) { return <div className="glass-panel rounded-2xl p-5"><Icon className="size-4 text-primary" /><p className="mt-5 text-2xl font-semibold tracking-tight text-slate-900">{value}</p><p className="mt-1 font-mono text-[9px] tracking-[.14em] text-slate-400 uppercase">{label}</p></div>; }
function Fact({ label, value }: { label: string; value: string }) { return <div><p className="font-mono text-[9px] tracking-[.14em] text-slate-400 uppercase">{label}</p><p className="mt-1 text-sm text-slate-700">{value}</p></div>; }
