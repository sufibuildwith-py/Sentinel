"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, FlaskConical, ShieldCheck } from "lucide-react";
import { api, money } from "@/lib/api";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { Skeleton } from "@/components/ui/skeleton";
import { TruthBadge } from "@/components/console-ui";

const percent = (value: number) => `${(value * 100).toFixed(1)}%`;

export default function RecoveryOlympicsPage() {
  const query = useQuery({ queryKey: ["recovery-olympics"], queryFn: api.recoveryOlympics, staleTime: 300_000 });
  if (query.isLoading) return <Skeleton className="h-[600px] rounded-2xl" />;
  if (query.error || !query.data) return <ErrorState error={query.error as Error} retry={() => void query.refetch()} />;
  const report = query.data;
  return <div><Link href="/evaluation" className="mb-4 inline-flex items-center gap-2 text-xs text-muted-foreground hover:text-foreground"><ArrowLeft className="size-3" />Back to Evaluation Lab</Link>
    <PageHeader eyebrow="Recovery Olympics" title="10,000-case controlled economics benchmark" description="Identical counterfactual fixtures compare recovery value, cost, safety and latency without provider execution." onRefresh={() => void query.refetch()} refreshing={query.isFetching} updated={query.dataUpdatedAt ? new Date(query.dataUpdatedAt) : undefined} />
    <div className="mb-5 flex flex-wrap gap-2"><TruthBadge label={report.truthLabel} /><TruthBadge label="CONTROLLED HOLDOUT" /><TruthBadge label={`SEED ${report.seed}`} /><TruthBadge label={report.datasetVersion} /></div>
    {report.datasetSize === 0 ? <Empty text={report.limitations[0]} /> : <>
      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4"><Kpi label="Frozen cases" value={String(report.datasetSize)} /><Kpi label="Held-out" value={String(report.frozenSplit.HELD_OUT)} /><Kpi label="Adversarial" value={String(report.frozenSplit.ADVERSARIAL)} /><Kpi label="Comparison arms" value={String(report.arms.length)} /></section>
      <section className="glass-panel mt-4 overflow-hidden rounded-2xl"><div className="border-b border-slate-200 p-5"><p className="eyebrow">Same cases, visible trade-offs</p><h2 className="mt-2 text-lg font-semibold">Arm scorecard</h2></div><div className="overflow-x-auto"><table className="min-w-[1180px] w-full text-left text-xs"><thead><tr className="border-b border-slate-200 font-mono text-[9px] uppercase tracking-widest text-slate-400"><th className="p-4">Arm</th><th>Net increment</th><th>Gross / natural</th><th>Cost</th><th>Increment rate (95% CI)</th><th>Refusals</th><th>Unsafe</th><th>Audit</th><th>p95</th></tr></thead><tbody>{report.arms.map((arm) => <tr key={arm.arm} className="border-b border-slate-100 last:border-0"><td className="p-4"><div className="flex items-center gap-2"><span className="font-mono text-primary">{arm.arm}</span><span className="font-medium">{arm.label}</span></div><span className="mt-2 inline-flex border border-slate-200 px-2 py-1 font-mono text-[8px] uppercase tracking-wider text-slate-500">{arm.methodologyLabel.replaceAll("_", " ")}</span></td><td className={arm.netIncrementalValueMinor >= 0 ? "text-[#22c55e]" : "text-[#ef4444]"}>{money(arm.netIncrementalValueMinor)}</td><td>{money(arm.grossRecoveryMinor)} / {money(arm.naturalRecoveryMinor)}</td><td>{money(arm.recoveryCostMinor)}</td><td>{arm.incrementalRecoveryRate.denominator <= 1 ? <span className="font-semibold text-amber-700">Insufficient sample (n={arm.incrementalRecoveryRate.denominator})</span> : percent(arm.incrementalRecoveryRate.value)} <span className="text-muted-foreground">({percent(arm.incrementalRecoveryRate.lower95)}–{percent(arm.incrementalRecoveryRate.upper95)})</span></td><td>{arm.refusals}</td><td><StateBadge value={arm.unsafeExecutions === 0 ? "PASS" : "FAILED"} /></td><td>{percent(arm.auditCompleteness)}</td><td>{arm.decisionLatencyMillis.p95}ms</td></tr>)}</tbody></table></div></section>
      <section className="mt-4 grid gap-4 lg:grid-cols-2"><Info icon={ShieldCheck} title="Benchmark integrity" lines={report.integrityRules} /><Info icon={FlaskConical} title="Assumptions and limits" lines={[...report.simulatorAssumptions, ...report.limitations]} /></section>
    </>}
  </div>;
}

function Kpi({ label, value }: { label: string; value: string }) { return <div className="glass-panel rounded-xl p-5"><p className="font-mono text-2xl font-bold">{value}</p><p className="mt-2 font-mono text-[9px] uppercase tracking-[0.18em] text-[#444444]">{label}</p></div>; }
function Empty({ text }: { text: string }) { return <div className="glass-panel rounded-2xl py-20 text-center"><p className="font-mono text-xs uppercase tracking-widest text-[#444444]">Benchmark unavailable in fixture mode</p><p className="mx-auto mt-3 max-w-lg text-xs text-muted-foreground">{text}</p></div>; }
function Info({ icon: Icon, title, lines }: { icon: typeof ShieldCheck; title: string; lines: string[] }) { return <section className="glass-panel rounded-2xl p-5"><div className="flex items-center gap-2"><Icon className="size-4 text-primary" /><h2 className="font-semibold">{title}</h2></div><ul className="mt-4 space-y-2">{lines.map((line) => <li key={line} className="text-xs leading-5 text-muted-foreground">{line}</li>)}</ul></section>; }
