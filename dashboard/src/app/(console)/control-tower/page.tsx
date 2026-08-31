"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Activity, ArrowRight, BrainCircuit, CircleDollarSign, Gauge, RadioTower, ShieldAlert, Waypoints } from "lucide-react";
import { api, money, shortId } from "@/lib/api";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { TruthBadge } from "@/components/console-ui";
import { durationLabel } from "@/lib/truth";

export default function ControlTowerPage() {
  const tower = useQuery({ queryKey: ["control-tower"], queryFn: api.controlTower });
  const lostRevenue = useQuery({ queryKey: ["lost-revenue"], queryFn: api.lostRevenue });
  const refresh = () => { void tower.refetch(); void lostRevenue.refetch(); };
  if (tower.error) return <ErrorState error={tower.error} retry={refresh} />;
  if (!tower.data) return <><PageHeader eyebrow="Control tower" title="Governed recovery posture" description="Loading the operational truth model." /><Skeleton className="h-[560px] rounded-2xl" /></>;
  const data = tower.data;
  const current = data.paymentHealth.current["15m"] ?? Object.values(data.paymentHealth.current)[0];
  const activeSignals = data.paymentHealth.signals.filter((signal) => signal.active);
  const enabledSwitches = Object.entries(data.governor.killSwitches).filter(([, enabled]) => enabled);

  return <div>
    <PageHeader eyebrow="Control tower" title="Merchant recovery command plane" description="A single governed view of payment health, opportunity, authority, execution truth, and learning posture." onRefresh={refresh} refreshing={tower.isFetching} updated={new Date(data.generatedAt)} />
    <div className="mb-5 flex flex-wrap gap-2">{data.truthLabels.map((label) => <TruthBadge key={label} label={label} />)}</div>

    <section className="grid gap-4 xl:grid-cols-[1.25fr_.75fr]">
      <Panel icon={RadioTower} eyebrow="Payment health radar" title={activeSignals.length ? `${activeSignals.length} active systemic signals` : "No active systemic signal"}>
        <div className="grid gap-3 sm:grid-cols-3"><Kpi label="15m volume" value={String(current?.volume ?? 0)} /><Kpi label="Success rate" value={`${((current?.successRate ?? 0) * 100).toFixed(1)}%`} /><Kpi label="Value at risk" value={money(current?.amountAtRiskMinor ?? 0)} tone="text-[#ef4444]" /></div>
        <div className="mt-5 space-y-3">{activeSignals.slice(0, 5).map((signal) => <div key={`${signal.type}-${signal.scope}`} className="border-l-2 border-primary/40 pl-4"><div className="flex flex-wrap items-center justify-between gap-2"><p className="font-mono text-xs font-semibold">{signal.type.replaceAll("_", " ")}</p><span className="test-label">{signal.scope}</span></div><p className="mt-1 text-xs text-muted-foreground">Actual {signal.actual.toFixed(3)} · baseline {signal.baseline.toFixed(3)} · threshold {signal.threshold.toFixed(3)}</p><p className="mt-2 text-xs text-[#888888]">{signal.evidence[0]}</p></div>)}{activeSignals.length === 0 && <Empty label="No active degradation at this evaluation time." />}</div>
      </Panel>
      <Panel icon={ShieldAlert} eyebrow="Governor posture" title={enabledSwitches.length ? `${enabledSwitches.length} kill switches enabled` : "All execution lanes available"}>
        <div className="space-y-2">{Object.entries(data.governor.killSwitches).map(([name, enabled]) => <div key={name} className="flex items-center justify-between border-b border-white/[0.05] py-2 last:border-0"><span className="font-mono text-[10px] text-muted-foreground">{name.replaceAll("_", " ")}</span><StateBadge value={enabled ? "STOPPED" : "APPROVED"} /></div>)}</div>
        <div className="mt-5 grid grid-cols-2 gap-3"><Kpi label="Incident ceiling" value={String(data.governor.maxIncidents)} /><Kpi label="Canary size" value={String(data.governor.canarySize)} /></div>
      </Panel>
    </section>

    <section className="mt-4 grid gap-4 lg:grid-cols-2">
      <Panel icon={CircleDollarSign} eyebrow="Revenue leakage" title="Financial truth waterfall">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4"><Kpi label="Failed" value={money(data.financialAttribution.failedValueMinor)} tone="text-[#ef4444]" /><Kpi label="Addressable" value={money(data.financialAttribution.addressableValueMinor)} /><Kpi label="Provider confirmed" value={money(data.financialAttribution.providerConfirmedRecoveryMinor)} tone="text-[#22c55e]" /><Kpi label="Unreconciled" value={money(data.financialAttribution.unreconciledExecutedValueMinor)} tone="text-[#f59e0b]" /></div>
        <div className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-4">{Object.entries(data.financialAttribution.timings).map(([name, timing]) => <div key={name} className="rounded-lg border border-slate-200 bg-white/70 p-3"><p className="font-mono text-[10px] uppercase text-slate-400">{name}</p><p className="mt-2 font-mono text-xs">{durationLabel(timing.averageMillis)}</p><p className="mt-1 text-[9px] text-slate-400">{timing.samples} samples{timing.averageMillis != null && timing.averageMillis > 86_400_000 ? " · inspect outlier" : ""}</p></div>)}</div>
      </Panel>
      <Panel icon={Waypoints} eyebrow="Opportunity queue" title="Ranked in shadow, never granted authority">
        <div className="space-y-3">{data.opportunities.slice(0, 5).map((opportunity) => <div key={opportunity.decisionId} className="border-b border-white/[0.05] pb-3 last:border-0"><div className="flex items-center justify-between gap-3"><Link href={`/incidents/${opportunity.incidentId}`} className="font-mono text-xs hover:text-primary">{shortId(opportunity.incidentId)}</Link><TruthLabel label={opportunity.mode.replaceAll("_", " ")} /></div><p className="mt-2 text-sm font-medium">{opportunity.selectedAction.replaceAll("_", " ")}</p><div className="mt-2 flex flex-wrap gap-2"><StateBadge value={opportunity.policyState} /><StateBadge value={opportunity.governorState} />{opportunity.netIncrementalValueMinor != null && <span className="text-xs text-muted-foreground">Estimated {money(opportunity.netIncrementalValueMinor)}</span>}</div></div>)}{data.opportunities.length === 0 && <Empty label="No opportunity evaluations recorded." />}</div>
      </Panel>
    </section>

    <section className="mt-4 glass-panel rounded-2xl p-5 sm:p-6"><div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start"><div><p className="eyebrow">Lost Revenue Explorer</p><h2 className="mt-2 text-lg font-semibold">Why Sentinel deliberately did not recover this money</h2><p className="mt-2 text-xs text-muted-foreground">Observed state categories only. Unknown natural recovery and causal lift remain explicitly unavailable.</p></div>{lostRevenue.data && <div className="shrink-0"><Kpi label="Unrecovered" value={money(lostRevenue.data.unrecoveredMinor)} tone="text-[#ef4444]" /></div>}</div>{lostRevenue.isLoading ? <Skeleton className="mt-5 h-28 rounded-xl" /> : lostRevenue.data ? <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-4">{lostRevenue.data.reasons.map((reason) => <div key={reason.category} className="border border-white/[0.06] p-4"><div className="flex items-start justify-between gap-2"><p className="font-mono text-[10px] font-semibold uppercase tracking-wider">{reason.category.replaceAll("_", " ")}</p><span className="font-mono text-[9px] text-[#444444]">{reason.incidentCount}</span></div><p className="mt-3 font-mono text-lg font-bold">{money(reason.amountMinor)}</p><p className="mt-2 text-xs leading-5 text-muted-foreground">{reason.explanation}</p><TruthLabel label={reason.evidenceClass.replaceAll("_", " ")} /></div>)}</div> : <p className="mt-5 text-xs text-muted-foreground">Lost-revenue evidence is unavailable; no category or amount has been inferred.</p>}</section>

    <section className="mt-4 grid gap-4 xl:grid-cols-3">
      <Panel icon={Activity} eyebrow="Systemic incidents" title="Root-cause evidence">
        {data.systemicIncidents.slice(0, 4).map((incident) => <div key={incident.id} className="mb-4 border-l border-white/10 pl-3 last:mb-0"><div className="flex items-center justify-between"><p className="text-xs font-semibold">{incident.scope}</p><StateBadge value={incident.status} /></div><p className="mt-2 text-xs text-muted-foreground">{incident.rootCauses[0]?.cause ?? "Root cause under evaluation"}{incident.rootCauses[0] ? ` · ${(incident.rootCauses[0].confidence * 100).toFixed(0)}%` : ""}</p></div>)}{data.systemicIncidents.length === 0 && <Empty label="No open systemic incident." />}
      </Panel>
      <Panel icon={BrainCircuit} eyebrow="Model lifecycle" title="Registry and champion posture">
        <div className="space-y-3">{data.models.map((model) => <div key={model.id} className="flex items-center justify-between gap-3"><div><p className="text-sm font-medium">{model.name}</p><p className="font-mono text-[10px] text-[#444444]">{model.version} · {model.featureSchemaVersion}</p></div><StateBadge value={model.lifecycle} /></div>)}{data.models.length === 0 && <Empty label="No registered model. Deterministic baseline remains active." />}</div>
      </Panel>
      <Panel icon={Gauge} eyebrow="Replay and shadow" title="Zero-tool comparison evidence">
        <div className="grid grid-cols-3 gap-2"><Kpi label="Snapshots" value={String(data.replayAndShadow.snapshotCount)} /><Kpi label="Comparisons" value={String(data.replayAndShadow.comparisonCount)} /><Kpi label="Critical" value={String(data.replayAndShadow.criticalRegressionCount)} tone={data.replayAndShadow.criticalRegressionCount ? "text-[#ef4444]" : "text-[#22c55e]"} /></div>
        <div className="mt-4 space-y-3">{data.replayAndShadow.latestDifferences.slice(0, 3).map((difference) => <div key={difference.id} className="border-l border-primary/30 pl-3"><p className="font-mono text-[10px]">{difference.productionAction} → {difference.shadowAction}</p><p className="mt-1 text-xs text-muted-foreground">{difference.explanation}</p><TruthLabel label="SHADOW ONLY" /></div>)}{data.replayAndShadow.latestDifferences.length === 0 && <p className="text-xs text-muted-foreground">No material decision differences recorded.</p>}</div>
      </Panel>
    </section>

    <section className="mt-4 glass-panel rounded-2xl p-5 sm:p-6"><div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center"><div><p className="eyebrow">Promise-to-pay posture</p><h2 className="mt-2 text-lg font-semibold">Customer commitments remain governed and reconciled</h2><p className="mt-2 text-xs text-muted-foreground">Only aggregate state is exposed here; customer references never reach the browser.</p></div><div className="flex gap-6"><Kpi label="Open records" value={String(data.promises.total)} /><Kpi label="Fulfilled" value={money(data.promises.fulfilledAmountMinor)} tone="text-[#22c55e]" /></div></div><Button nativeButton={false} variant="outline" className="mt-5" render={<Link href="/demo" />}>Open Failure Lab <ArrowRight /></Button></section>
  </div>;
}

function Panel({ icon: Icon, eyebrow, title, children }: { icon: typeof Activity; eyebrow: string; title: string; children: React.ReactNode }) { return <div className="glass-panel rounded-2xl p-5 sm:p-6"><div className="mb-5 flex items-start gap-3"><div className="grid size-9 shrink-0 place-items-center rounded-lg border border-primary/20 bg-primary/8"><Icon className="size-4 text-primary" /></div><div><p className="eyebrow">{eyebrow}</p><h2 className="mt-1 text-lg font-semibold">{title}</h2></div></div>{children}</div>; }
function Kpi({ label, value, tone = "text-foreground" }: { label: string; value: string; tone?: string }) { return <div><p className={`font-mono text-lg font-bold ${tone}`}>{value}</p><p className="mt-1 font-mono text-[9px] uppercase tracking-[0.16em] text-[#444444]">{label}</p></div>; }
function TruthLabel({ label }: { label: string }) { return <TruthBadge label={label} />; }
function Empty({ label }: { label: string }) { return <p className="py-5 text-center font-mono text-[10px] uppercase tracking-widest text-[#444444]">{label}</p>; }
