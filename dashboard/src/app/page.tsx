"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Activity, ArrowRight, CircleDollarSign, RefreshCw, ShieldCheck, Target, TrendingUp, Users } from "lucide-react";
import { motion } from "motion/react";
import { api, money, shortId } from "@/lib/api";
import { ErrorState, LoadingGrid, MetricCard, PageHeader, SelectedGlow, StateBadge } from "@/components/dashboard-ui";
import { Button } from "@/components/ui/button";

export default function OverviewPage() {
  const metrics = useQuery({ queryKey: ["metrics"], queryFn: api.metrics });
  const incidents = useQuery({ queryKey: ["incidents"], queryFn: api.incidents });
  const refresh = () => { void metrics.refetch(); void incidents.refetch(); };
  if (metrics.isLoading || incidents.isLoading) return <><PageHeader eyebrow="Overview" title="Revenue recovery, under control" description="A reconciled Test Mode view of risk, decisions, and outcomes." /><LoadingGrid /></>;
  if (metrics.error || incidents.error) return <ErrorState error={(metrics.error ?? incidents.error) as Error} retry={refresh} />;
  const data = metrics.data!;
  const focus = incidents.data?.find((item) => !["RECOVERED", "STOPPED"].includes(item.status)) ?? incidents.data?.[0];

  return <div>
    <PageHeader eyebrow="Overview" title="Revenue recovery, under control" description="One operational view from anomaly detection to verified payment outcome. Values below are Razorpay Test Mode synthetic evaluation data." onRefresh={refresh} refreshing={metrics.isFetching || incidents.isFetching} updated={metrics.dataUpdatedAt ? new Date(metrics.dataUpdatedAt) : undefined} />
    <div className="mb-4 flex items-center justify-between gap-3"><span className="test-label">Test mode / Synthetic evaluation</span><span className="text-right text-xs text-muted-foreground">Reconciled from persisted actions and outcomes</span></div>
    <section aria-label="Revenue metrics" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
      <MetricCard label="Revenue at risk" value={money(data.revenueAtRiskMinor)} note="Across active incidents" icon={Target} />
      <MetricCard label="Attempted recovery" value={money(data.attemptedRecoveryMinor)} note="Policy-approved actions only" icon={RefreshCw} />
      <MetricCard label="Recovered revenue" value={money(data.recoveredRevenueMinor)} note="Verified outcomes, exactly once" icon={CircleDollarSign} />
      <MetricCard label="Recovery rate" value={`${(data.recoveryRate * 100).toFixed(1)}%`} note="Recovered ÷ attempted value" icon={TrendingUp} />
      <MetricCard label="Active incidents" value={String(data.activeIncidents)} note="Requiring observation or action" icon={Activity} />
    </section>
    <section className="mt-4 grid gap-4 xl:grid-cols-[1.55fr_.85fr]">
      {focus ? <SelectedGlow className="glass-panel rounded-2xl p-5 sm:p-6"><motion.div layoutId={`incident-${focus.incidentId}`} className="relative z-10">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start"><div><div className="flex flex-wrap items-center gap-2"><span className="eyebrow">Highest-priority incident</span><span className="rounded-full border border-destructive/20 bg-destructive/8 px-2 py-0.5 text-[10px] font-semibold text-destructive">{focus.severity} severity</span></div><h2 className="mt-3 text-xl font-semibold tracking-tight">{focus.type.replaceAll("_", " ")}</h2><p className="mt-2 text-xs text-muted-foreground">Incident {shortId(focus.incidentId)} · {focus.affectedPaymentCount} payments · {focus.affectedCustomerCount} customers</p></div><StateBadge value={focus.status} /></div>
        <div className="mt-8 grid gap-5 border-y border-white/7 py-5 sm:grid-cols-3"><div><p className="eyebrow">At risk</p><p className="mt-2 text-lg font-semibold">{money(focus.amountAtRiskMinor)}</p></div><div><p className="eyebrow">Strategy</p><p className="mt-2 text-sm font-medium">{focus.strategy?.replaceAll("_", " ") ?? "Awaiting diagnosis"}</p></div><div><p className="eyebrow">Policy</p><div className="mt-2"><StateBadge value={focus.policyDecision} /></div></div></div>
        <div className="mt-5 flex flex-col justify-between gap-3 sm:flex-row sm:items-center"><p className="max-w-xl text-sm leading-6 text-muted-foreground">Every step is traceable: computed evidence, validated diagnosis, deterministic policy, one replay-safe action, and signed webhook reconciliation.</p><Button nativeButton={false} render={<Link href={`/incidents/${focus.incidentId}`} />} className="self-start">Open incident <ArrowRight /></Button></div>
      </motion.div></SelectedGlow> : <div className="glass-panel rounded-2xl p-10 text-center"><ShieldCheck className="mx-auto size-6 text-emerald-300" /><p className="mt-3 font-medium">No active incidents</p></div>}
      <div className="glass-panel rounded-2xl p-5 sm:p-6"><p className="eyebrow">System posture</p><h2 className="mt-3 text-lg font-semibold">Guardrails are active</h2><div className="mt-6 space-y-5">{[
        [ShieldCheck, "Deterministic policy", "Mandatory stop rules run before allow rules."], [Users, "Human review", "High-value or low-confidence actions wait for an actor."], [Activity, "Replay safe", "Duplicate actions and webhooks are rejected at persistence."],
      ].map(([Icon, title, text]) => <div key={String(title)} className="flex gap-3"><div className="grid size-8 shrink-0 place-items-center rounded-lg border border-white/8 bg-white/[.025]"><Icon className="size-4 text-primary" /></div><div><p className="text-sm font-medium">{String(title)}</p><p className="mt-1 text-xs leading-5 text-muted-foreground">{String(text)}</p></div></div>)}</div><Button nativeButton={false} variant="outline" className="mt-7 w-full" render={<Link href="/metrics" />}>View audit evidence <ArrowRight /></Button></div>
    </section>
  </div>;
}
