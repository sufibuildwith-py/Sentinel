"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Activity, AlertCircle, ArrowRight, CircleDollarSign, Clock3, ShieldAlert, ShieldCheck, Target, Users } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api, money, shortId } from "@/lib/api";
import { fixtureMode, frontendBuildSha } from "@/lib/environment";
import { financialTruthFunnel, policyDistribution as derivePolicyDistribution } from "@/lib/operations-board";
import { TruthBadge } from "@/components/console-ui";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { queryErrorPresentation } from "@/lib/api-errors";

const windows = ["5m", "15m", "60m", "24h", "7d"] as const;
const palette = ["#2563eb", "#f59e0b", "#ef4444", "#22c55e", "#64748b"];

export function OperationsCommandCenter() {
  const tower = useQuery({ queryKey: ["control-tower"], queryFn: api.controlTower });
  const incidents = useQuery({ queryKey: ["incidents"], queryFn: api.incidents });
  const approvals = useQuery({ queryKey: ["approvals"], queryFn: api.approvals });
  const backendInfo = useQuery({ queryKey: ["backend-info"], queryFn: api.backendInfo, staleTime: 60_000 });
  const historical = useQuery({ queryKey: ["historical-validation"], queryFn: api.historicalValidation, staleTime: 300_000 });
  const [healthWindow, setHealthWindow] = useState<(typeof windows)[number]>("15m");
  const refresh = () => { void tower.refetch(); void incidents.refetch(); void approvals.refetch(); void backendInfo.refetch(); void historical.refetch(); };
  const loading = tower.isLoading || incidents.isLoading;
  const error = tower.error ?? incidents.error;
  const policyDistribution = useMemo(() => derivePolicyDistribution(incidents.data ?? []), [incidents.data]);
  const failureDistribution = useMemo(() => {
    const values = new Map<string, number>();
    (incidents.data ?? []).forEach((item) => values.set(item.type, (values.get(item.type) ?? 0) + item.affectedPaymentCount));
    return [...values.entries()].sort((a, b) => b[1] - a[1]).slice(0, 6).map(([name, value]) => ({ name: name.replaceAll("_", " "), value }));
  }, [incidents.data]);

  if (loading) return <div className="space-y-4"><Skeleton className="h-24 rounded-xl" /><Skeleton className="h-[520px] rounded-2xl" /></div>;
  if (error || !tower.data) return <ErrorState error={(error ?? new Error("Control Tower unavailable")) as Error} retry={refresh} />;
  const data = tower.data;
  const attribution = data.financialAttribution;
  const activeIncidents = (incidents.data ?? []).filter((item) => !["RECOVERED", "FAILED", "STOPPED"].includes(item.status));
  const policyBlocks = (incidents.data ?? []).filter((item) => item.policyDecision === "DENY").length;
  const enabledSwitches = Object.values(data.governor.killSwitches).filter(Boolean).length;
  const currentHealth = data.paymentHealth.current[healthWindow] ?? data.paymentHealth.baseline[healthWindow];
  const funnel = financialTruthFunnel(attribution);
  const providerExecutionLabel = !backendInfo.data?.providerExecution
    ? "UNKNOWN"
    : backendInfo.data.providerExecution.enabled
      ? "ENABLED"
      : "DISABLED";

  return <div>
    <div className="-mx-4 mb-6 flex flex-wrap items-center gap-x-6 gap-y-2 border-b border-slate-200 bg-white/70 px-6 py-3 font-mono text-[10px] tracking-[0.2em] uppercase backdrop-blur sm:-mx-6 lg:-mx-8"><span className="flex items-center gap-2 text-emerald-600"><span className="size-1.5 rounded-full bg-emerald-500" />System online</span><span className="text-slate-500">Razorpay Test Mode: {providerExecutionLabel}</span><span className="text-slate-500">Policy engine active</span><span className="text-slate-500">Fixture Mode: {fixtureMode ? "ON" : "OFF"}</span><span className="text-slate-400">Frontend {frontendBuildSha.slice(0, 8)}</span>{backendInfo.data?.application?.commit && <span className="text-slate-400">Backend {backendInfo.data.application.commit.slice(0, 8)}</span>}</div>
    <PageHeader eyebrow="Overview" title="Revenue recovery operations" description="Portfolio risk, deterministic authority, provider acceptance, and confirmed financial truth in one view." onRefresh={refresh} refreshing={tower.isFetching || incidents.isFetching || approvals.isFetching} updated={tower.dataUpdatedAt ? new Date(tower.dataUpdatedAt) : undefined} />
    {approvals.error && <ScopedDataWarning error={approvals.error} retry={() => void approvals.refetch()} />}
    {historical.error && <ScopedDataWarning scope="Historical validation" error={historical.error} retry={() => void historical.refetch()} />}
    <div className="mb-5 flex flex-wrap gap-2"><TruthBadge label={data.scopeLabel} />{data.truthLabels.slice(4).map((label) => <TruthBadge key={label} label={label} />)}</div>

    <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-5" aria-label="Portfolio financial and safety posture">
      <Kpi icon={Target} label="Revenue at risk" value={money(attribution.failedValueMinor)} tone="text-red-600" detail="Observed failed value" />
      <Kpi icon={CircleDollarSign} label="Addressable" value={money(attribution.addressableValueMinor)} detail="After policy/provider eligibility" />
      <Kpi icon={CircleDollarSign} label="Incremental opportunity" value={money(attribution.expectedIncrementalOpportunityMinor)} detail="Estimator output; evidence maturity applies" />
      <Kpi icon={Clock3} label="Awaiting provider truth" value={money(attribution.unreconciledExecutedValueMinor)} tone="text-amber-600" detail="Accepted/executed, not recovered" />
      <Kpi icon={ShieldCheck} label="Provider confirmed" value={money(attribution.providerConfirmedRecoveryMinor)} tone="text-emerald-600" detail="Signed reconciled outcomes" />
      <Kpi icon={CircleDollarSign} label="Net attributed" value={money(attribution.netIncrementalValueMinor)} tone="text-emerald-600" detail={attribution.recoveryCostStatus.replaceAll("_", " ")} />
      <Kpi icon={Activity} label="Open incidents" value={String(activeIncidents.length)} detail={`${incidents.data?.length ?? 0} operational records`} />
      <Kpi icon={Users} label="Awaiting human review" value={String(approvals.data?.length ?? 0)} tone="text-amber-600" detail="Explicit actor and reason required" />
      <Kpi icon={ShieldAlert} label="Policy blocks" value={String(policyBlocks)} tone="text-red-600" detail="Deterministic refusals" />
      <Kpi icon={ShieldAlert} label="Kill switches active" value={String(enabledSwitches)} tone={enabledSwitches ? "text-red-600" : "text-emerald-600"} detail="Governor-wide execution posture" />
    </section>

    <section className="mt-4 grid gap-4 xl:grid-cols-[1.15fr_.85fr]">
      <Panel title="Financial truth funnel" eyebrow="Recovery analytics"><ResponsiveContainer width="100%" height={280}><BarChart data={funnel} margin={{ left: 8, right: 8, top: 12, bottom: 4 }}><CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" /><XAxis dataKey="name" tick={{ fontSize: 10, fill: "#64748b" }} /><YAxis hide /><Tooltip formatter={(value) => money(Number(value))} cursor={{ fill: "#eff6ff" }} /><Bar dataKey="value" radius={[6, 6, 0, 0]}>{funnel.map((item, index) => <Cell key={item.name} fill={index >= 3 ? "#22c55e" : palette[index]} />)}</Bar></BarChart></ResponsiveContainer><p className="text-xs text-muted-foreground">Executed value and provider-confirmed value are intentionally separate.</p></Panel>
      <Panel title="Policy disposition" eyebrow="Deterministic authority"><ResponsiveContainer width="100%" height={250}><PieChart><Pie data={policyDistribution} dataKey="value" nameKey="name" innerRadius={62} outerRadius={88} paddingAngle={3}>{policyDistribution.map((item, index) => <Cell key={item.name} fill={palette[index]} />)}</Pie><Tooltip /></PieChart></ResponsiveContainer><div className="grid grid-cols-2 gap-2">{policyDistribution.map((item, index) => <div key={item.name} className="flex items-center justify-between border-b border-slate-100 py-2 text-xs"><span className="flex items-center gap-2"><span className={`size-2 rounded-full ${["bg-blue-600", "bg-amber-500", "bg-red-500", "bg-emerald-500"][index]}`} />{item.name}</span><strong>{item.value}</strong></div>)}</div></Panel>
    </section>

    <section className="mt-4 grid gap-4 xl:grid-cols-[.8fr_1.2fr]">
      <Panel title="Payment Health Radar" eyebrow="5m · 15m · 60m · 24h · 7d"><div className="flex flex-wrap gap-2">{windows.map((window) => <Button key={window} size="sm" variant={healthWindow === window ? "default" : "outline"} onClick={() => setHealthWindow(window)}>{window}</Button>)}</div>{currentHealth ? <div className="mt-5 grid grid-cols-2 gap-3"><HealthFact label="Volume" value={String(currentHealth.volume)} /><HealthFact label="Failures" value={String(currentHealth.failures)} /><HealthFact label="Success rate" value={`${(currentHealth.successRate * 100).toFixed(1)}%`} /><HealthFact label="At risk" value={money(currentHealth.amountAtRiskMinor)} /></div> : <p className="mt-6 text-xs text-muted-foreground">Insufficient data for the selected window.</p>}<div className="mt-5 space-y-2">{data.paymentHealth.signals.filter((signal) => signal.active).slice(0, 4).map((signal) => <div key={`${signal.type}-${signal.scope}`} className="border-l-2 border-amber-400 pl-3"><p className="font-mono text-[10px] font-semibold">{signal.type.replaceAll("_", " ")}</p><p className="mt-1 text-xs text-muted-foreground">{signal.scope} · actual {signal.actual.toFixed(3)} · threshold {signal.threshold.toFixed(3)}</p></div>)}</div></Panel>
      <Panel title="Failure concentration" eyebrow="Affected operational payments">{failureDistribution.length ? <ResponsiveContainer width="100%" height={300}><BarChart data={failureDistribution} layout="vertical" margin={{ left: 12, right: 24 }}><CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#e2e8f0" /><XAxis type="number" hide /><YAxis type="category" dataKey="name" width={125} tick={{ fontSize: 9, fill: "#64748b" }} /><Tooltip /><Bar dataKey="value" fill="#2563eb" radius={[0, 6, 6, 0]} /></BarChart></ResponsiveContainer> : <p className="py-16 text-center text-xs text-muted-foreground">No operational failure distribution is available.</p>}</Panel>
    </section>

    <section className="mt-4 grid gap-4 lg:grid-cols-2">
      <Panel title="Active recovery portfolio" eyebrow="Operator focus"><div className="space-y-3">{activeIncidents.slice(0, 5).map((incident) => <Link key={incident.incidentId} href={`/incidents/${incident.incidentId}`} className="flex items-center justify-between gap-4 rounded-xl border border-slate-200 bg-white/70 p-4 transition-colors hover:border-primary/30"><div><p className="font-mono text-[9px] text-primary">{shortId(incident.incidentId)}</p><p className="mt-1 text-sm font-semibold">{incident.type.replaceAll("_", " ")}</p><p className="mt-1 text-xs text-muted-foreground">{money(incident.amountAtRiskMinor)} · {incident.affectedPaymentCount} payments</p></div><div className="flex items-center gap-2"><StateBadge value={incident.status} /><ArrowRight className="size-4 text-slate-400" /></div></Link>)}{activeIncidents.length === 0 && <p className="py-12 text-center text-xs text-muted-foreground">No active operational incident.</p>}</div><Button nativeButton={false} variant="outline" className="mt-4 w-full" render={<Link href="/recovery" />}>Open recovery operations board <ArrowRight /></Button></Panel>
      <Panel title="Safety prevented" eyebrow="Refusals are successful controls"><div className="grid grid-cols-2 gap-3"><SafetyFact label="Policy blocks" value={policyBlocks} /><SafetyFact label="Governor-wide stops" value={enabledSwitches} /><SafetyFact label="Shadow regressions" value={data.replayAndShadow.criticalRegressionCount} /><SafetyFact label="Human reviews" value={approvals.data?.length ?? 0} /></div><p className="mt-5 text-xs leading-5 text-muted-foreground">Counts reflect persisted operational decisions and current governor posture. Duplicate-prevention totals are not exposed by this aggregate and are not estimated.</p></Panel>
    </section>
    {historical.data && <section className="mt-4"><Panel title="Historical Razorpay validation" eyebrow="Historical public source · replay only"><div className="grid grid-cols-2 gap-3 sm:grid-cols-4"><SafetyFact label="Public cases" value={historical.data.acceptedPublicSourceCases} /><SafetyFact label="Replay passes" value={historical.data.passed} /><SafetyFact label="Safe refusals" value={historical.data.safeRefusals} /><SafetyFact label="Unsafe executions" value={historical.data.unsafeExecutions} /></div><div className="mt-5 flex flex-wrap gap-2">{Object.entries(historical.data.sourceComposition).map(([source, count]) => <span key={source} className="rounded-full border border-cyan-200 bg-cyan-50 px-3 py-1 font-mono text-[9px] text-cyan-800">{source.replaceAll("_", " ")} · {count}</span>)}</div><p className="mt-4 text-xs text-muted-foreground">These are provenance-linked public integration and failure reports. They carry no merchant INR amount and never enter provider execution.</p><Button nativeButton={false} variant="outline" className="mt-4" render={<Link href="/evaluation/historical" />}>Browse all historical cases <ArrowRight /></Button></Panel></section>}
  </div>;
}

function ScopedDataWarning({ error, retry, scope = "Review queue" }: { error: Error; retry: () => void; scope?: string }) { const state = queryErrorPresentation(error); return <div className="mb-5 flex flex-col gap-3 rounded-xl border border-amber-200 bg-amber-50/70 p-4 text-sm sm:flex-row sm:items-center sm:justify-between"><div className="flex items-start gap-3"><AlertCircle className="mt-0.5 size-4 shrink-0 text-amber-600" /><div><p className="font-semibold text-amber-900">{scope}: {state.label}</p><p className="mt-1 text-xs text-amber-800">{state.message}{state.requestId ? ` Request ID ${state.requestId}.` : ""} Other command-center data remains live.</p></div></div><Button size="sm" variant="outline" onClick={retry}>Retry {scope.toLowerCase()}</Button></div>; }

function Kpi({ icon: Icon, label, value, detail, tone = "text-slate-900" }: { icon: typeof Activity; label: string; value: string; detail: string; tone?: string }) { return <div className="glass-panel rounded-xl p-5"><div className="flex items-center justify-between"><p className="eyebrow">{label}</p><Icon className="size-4 text-slate-400" /></div><p className={`mt-5 font-mono text-xl font-bold ${tone}`}>{value}</p><p className="mt-2 text-[11px] leading-4 text-muted-foreground">{detail}</p></div>; }
function Panel({ title, eyebrow, children }: { title: string; eyebrow: string; children: React.ReactNode }) { return <div className="glass-panel rounded-2xl p-5 sm:p-6"><p className="eyebrow">{eyebrow}</p><h2 className="mt-2 text-lg font-semibold">{title}</h2><div className="mt-5">{children}</div></div>; }
function HealthFact({ label, value }: { label: string; value: string }) { return <div className="rounded-xl border border-slate-200 bg-white/70 p-3"><p className="font-mono text-[9px] text-slate-400 uppercase">{label}</p><p className="mt-2 font-mono text-sm font-semibold">{value}</p></div>; }
function SafetyFact({ label, value }: { label: string; value: number }) { return <div className="rounded-xl border border-slate-200 bg-white/70 p-4"><p className="font-mono text-2xl font-bold">{value}</p><p className="mt-1 font-mono text-[9px] tracking-[.12em] text-slate-400 uppercase">{label}</p></div>; }
