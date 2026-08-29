"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts";
import { Search } from "lucide-react";
import { api, money, shortId } from "@/lib/api";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";

const chartConfig = { rate: { label: "Recovery rate", color: "var(--chart-1)" } } satisfies ChartConfig;

export default function MetricsPage() {
  const metrics = useQuery({ queryKey: ["metrics"], queryFn: api.metrics }); const incidents = useQuery({ queryKey: ["incidents"], queryFn: api.incidents }); const [selected, setSelected] = useState(""); const [filter, setFilter] = useState("");
  const incidentId = selected || incidents.data?.[0]?.incidentId || ""; const audit = useQuery({ queryKey: ["audit", incidentId], queryFn: () => api.audit(incidentId), enabled: Boolean(incidentId) });
  const entries = useMemo(() => (audit.data ?? []).filter((entry) => `${entry.stage} ${entry.narrative} ${entry.actor}`.toLowerCase().includes(filter.toLowerCase())), [audit.data, filter]);
  const refresh = () => { void metrics.refetch(); void incidents.refetch(); if (incidentId) void audit.refetch(); };
  if (metrics.error || incidents.error) return <ErrorState error={(metrics.error ?? incidents.error) as Error} retry={refresh} />;
  return <div><PageHeader eyebrow="Measurement & evidence" title="Metrics and immutable audit" description="Recovered revenue is counted only from verified, reconciled Test Mode outcomes—not from link creation." onRefresh={refresh} refreshing={metrics.isFetching || incidents.isFetching || audit.isFetching} updated={metrics.dataUpdatedAt ? new Date(metrics.dataUpdatedAt) : undefined} />
    <div className="mb-4"><span className="test-label">Test mode / Synthetic evaluation</span></div>
    {metrics.isLoading ? <Skeleton className="h-72 rounded-xl" /> : metrics.data && <><div className="grid grid-cols-2 gap-3 lg:grid-cols-4">{[
      ["Revenue at risk", money(metrics.data.revenueAtRiskMinor), "text-[#ef4444]"],
      ["Recovery attempted", money(metrics.data.attemptedRecoveryMinor), "text-[#f59e0b]"],
      ["Verified recovered", money(metrics.data.recoveredRevenueMinor), "text-[#22c55e]"],
      ["Value recovery rate (%)", `${(metrics.data.recoveryRate * 100).toFixed(1)}%`, "text-[#22c55e]"],
    ].map(([label, value, color]) => <div key={label} className="glass-panel rounded-xl p-5"><p className={`font-mono text-3xl font-bold ${color}`}>{value}</p><p className="mt-1 font-mono text-[9px] tracking-[0.2em] text-[#444444] uppercase">{label}</p></div>)}</div>
      <div className="glass-panel mt-4 rounded-2xl p-5"><div><p className="eyebrow">Strategy performance</p><h2 className="mt-2 text-lg font-semibold">Recovered share of attempted value</h2></div><ChartContainer config={chartConfig} className="mt-5 h-[260px] w-full"><BarChart accessibilityLayer data={metrics.data.strategyPerformance.map((item) => ({ ...item, rate: +(item.rate * 100).toFixed(1) }))} margin={{ left: 0, right: 12 }}><CartesianGrid vertical={false} stroke="var(--border)" /><XAxis dataKey="strategy" tickLine={false} axisLine={false} tickMargin={10} tick={{ fontSize: 11 }} /><YAxis tickLine={false} axisLine={false} tickFormatter={(v) => `${v}%`} tick={{ fontSize: 11 }} /><ChartTooltip content={<ChartTooltipContent />} /><Bar dataKey="rate" fill="var(--color-rate)" radius={[6, 6, 2, 2]} /></BarChart></ChartContainer></div></>}
    <div className="mt-4 grid gap-4 lg:grid-cols-[320px_1fr]"><aside className="glass-panel rounded-xl p-3"><p className="eyebrow px-2 py-2">Incident audit</p><div className="space-y-1">{incidents.data?.map((item) => <button key={item.incidentId} onClick={() => setSelected(item.incidentId)} className={`w-full rounded-lg border p-3 text-left ${incidentId === item.incidentId ? "border-primary/30 bg-primary/8" : "border-transparent hover:bg-white/[.025]"}`}><p className="text-xs font-medium">{item.type.replaceAll("_", " ")}</p><div className="mt-2 flex items-center justify-between"><span className="text-[10px] text-muted-foreground">{shortId(item.incidentId)}</span><StateBadge value={item.status} /></div></button>)}</div></aside><section className="glass-panel rounded-xl p-5"><div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center"><div><p className="eyebrow">Chronological decision story</p><h2 className="mt-2 font-semibold">Incident {incidentId ? shortId(incidentId) : "—"}</h2></div><label className="relative"><Search className="absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" /><span className="sr-only">Filter audit events</span><Input value={filter} onChange={(e) => setFilter(e.target.value)} placeholder="Filter audit" className="pl-9" /></label></div><ol className="mt-5 space-y-4">{entries.map((entry) => <li key={entry.eventId} className="border-l border-white/10 pl-4"><div className="flex flex-wrap items-center gap-2"><span className="text-xs font-semibold">{entry.stage}</span>{entry.policyResult && <StateBadge value={entry.policyResult} />}<span className="text-[10px] text-muted-foreground">{new Date(entry.timestamp).toLocaleString()}</span></div><p className="mt-1 text-sm leading-6 text-muted-foreground">{entry.narrative}</p>{entry.ruleTrace.length > 0 && <p className="mt-1 text-[11px] text-primary">{entry.ruleTrace.length} policy rules recorded</p>}</li>)}{!audit.isLoading && entries.length === 0 && <li className="py-8 text-center text-sm text-muted-foreground">No audit events match this filter.</li>}</ol></section></div>
  </div>;
}
