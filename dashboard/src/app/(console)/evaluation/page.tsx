"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AnimatePresence, motion } from "motion/react";
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts";
import { Check, CircleDollarSign, FileJson2, FileText, FlaskConical, Gauge, Info, Play, Search, ShieldCheck, TriangleAlert } from "lucide-react";
import { toast } from "sonner";
import { api, money } from "@/lib/api";
import { mutationErrorMessage } from "@/lib/api-errors";
import type { EvaluationReport, EvaluationScenarioResult } from "@/lib/types";
import { useStatusIsland } from "@/components/providers";
import { ErrorState, LoadingGrid, PageHeader, SelectedGlow, StateBadge } from "@/components/dashboard-ui";
import { Button } from "@/components/ui/button";
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

const chartConfig = {
  detected: { label: "Detected", color: "var(--chart-1)" },
  eligible: { label: "Policy eligible", color: "var(--chart-2)" },
  attempted: { label: "Attempted", color: "var(--chart-4)" },
  recovered: { label: "Verified recovered", color: "var(--chart-1)" },
  rate: { label: "Recovery rate", color: "var(--chart-1)" },
} satisfies ChartConfig;

const percent = (value: number) => `${(value * 100).toFixed(1)}%`;
const compactMoney = (minor: number) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", notation: "compact", maximumFractionDigits: 1 }).format(minor / 100);

function ScoreCard({ label, score, note, icon: Icon }: { label: string; score: number; note: string; icon: typeof Gauge }) {
  return <motion.article layout className="glass-panel rounded-xl p-4 sm:p-5"><div className="flex items-center justify-between"><p className="eyebrow">{label}</p><Icon className="size-4 text-primary" /></div><motion.p key={score} initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mt-4 text-2xl font-semibold tracking-[-.04em]">{percent(score)}</motion.p><p className="mt-2 text-[11px] leading-4 text-muted-foreground">{note}</p></motion.article>;
}

function ConfusionMatrix({ report }: { report: EvaluationReport }) {
  const matrix = report.detectionConfusionMatrix;
  const cells = [
    { label: "True positive", value: matrix.truePositive, detail: "Incident expected and detected", tone: "border-primary/30 bg-primary/8" },
    { label: "False negative", value: matrix.falseNegative, detail: "Incident expected but missed", tone: "border-destructive/25 bg-destructive/7" },
    { label: "False positive", value: matrix.falsePositive, detail: "No incident expected but detected", tone: "border-destructive/25 bg-destructive/7" },
    { label: "True negative", value: matrix.trueNegative, detail: "No incident expected or detected", tone: "border-emerald-300/20 bg-emerald-300/6" },
  ];
  return <section className="glass-panel rounded-2xl p-5" aria-labelledby="confusion-heading"><p className="eyebrow">Detection evidence</p><h2 id="confusion-heading" className="mt-2 text-lg font-semibold">Confusion matrix</h2><p className="mt-1 text-xs text-muted-foreground">Every cell includes its meaning; status is not conveyed by colour alone.</p><div className="mt-5 grid grid-cols-2 gap-2" role="group" aria-label="Detection confusion matrix">{cells.map((cell) => <div key={cell.label} role="group" aria-label={`${cell.label}: ${cell.value}. ${cell.detail}`} className={`rounded-xl border p-4 ${cell.tone}`}><p className="text-[10px] font-semibold uppercase tracking-[.12em] text-muted-foreground">{cell.label}</p><p className="mt-2 text-2xl font-semibold">{cell.value}</p><p className="mt-1 text-[10px] leading-4 text-muted-foreground">{cell.detail}</p></div>)}</div></section>;
}

function Funnel({ report }: { report: EvaluationReport }) {
  const funnel = report.recoveryFunnel;
  const data = [{ stage: "Detected", value: funnel.detectedIncidents, fill: "var(--chart-1)" }, { stage: "Eligible", value: funnel.policyEligible, fill: "var(--chart-2)" }, { stage: "Attempted", value: funnel.attempted, fill: "var(--chart-4)" }, { stage: "Recovered", value: funnel.verifiedRecovered, fill: "var(--chart-1)" }];
  return <section className="glass-panel rounded-2xl p-5" aria-labelledby="funnel-heading"><p className="eyebrow">Revenue recovery funnel</p><div className="mt-2 flex flex-wrap items-end justify-between gap-2"><h2 id="funnel-heading" className="text-lg font-semibold">At risk to verified outcome</h2><span className="test-label">Synthetic values</span></div><p className="mt-2 text-xs text-muted-foreground">{compactMoney(funnel.amountAtRiskMinor)} at risk → {money(report.recoveredAmountMinor)} verified recovered</p><ChartContainer config={chartConfig} className="mt-4 h-[220px] w-full"><BarChart accessibilityLayer data={data}><CartesianGrid vertical={false} stroke="var(--border)" /><XAxis dataKey="stage" tickLine={false} axisLine={false} tick={{ fontSize: 10 }} /><YAxis tickLine={false} axisLine={false} tick={{ fontSize: 10 }} /><ChartTooltip content={<ChartTooltipContent />} /><Bar dataKey="value" radius={[7, 7, 2, 2]} /></BarChart></ChartContainer></section>;
}

function ScenarioExplorer({ scenarios }: { scenarios: EvaluationScenarioResult[] }) {
  const [filter, setFilter] = useState(""); const [selected, setSelected] = useState<string | null>(null);
  const visible = useMemo(() => scenarios.filter((item) =>
    `${item.scenarioId} ${item.category} ${item.actualPolicyDecision} ${item.actualProviderOutcome}`
      .replaceAll("_", " ").toLowerCase().includes(filter.toLowerCase()),
  ).slice(0, 80), [filter, scenarios]);
  const selectedScenario = scenarios.find((item) => item.scenarioId === selected);
  return <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]"><section className="glass-panel min-w-0 rounded-xl p-4"><div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center"><div><p className="eyebrow">Ground truth vs actual</p><p className="mt-1 text-xs text-muted-foreground">Showing {visible.length} of {scenarios.length} scenarios</p></div><label className="relative"><span className="sr-only">Filter evaluation scenarios</span><Search className="absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" /><Input className="pl-9" placeholder="Filter category, policy, outcome" value={filter} onChange={(event) => setFilter(event.target.value)} /></label></div><div className="mt-4 overflow-x-auto"><table className="w-full min-w-[760px] text-left text-xs"><thead><tr className="border-b text-[10px] uppercase tracking-[.12em] text-muted-foreground"><th className="px-3 py-3">Scenario</th><th className="px-3 py-3">Incident</th><th className="px-3 py-3">Policy</th><th className="px-3 py-3">Provider outcome</th><th className="px-3 py-3">Result</th></tr></thead><tbody>{visible.map((item) => <tr key={item.scenarioId} className="cursor-pointer border-b border-white/5 hover:bg-white/[.025]" onClick={() => setSelected(item.scenarioId)} tabIndex={0} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") setSelected(item.scenarioId); }}><td className="px-3 py-3"><p className="font-medium">{item.category.replaceAll("_", " ")}</p><p className="mt-1 font-mono text-[9px] text-muted-foreground">{item.scenarioId}</p></td><td className="px-3 py-3">{item.expectedIncident === item.actualIncident ? "Match" : "Mismatch"}</td><td className="px-3 py-3"><StateBadge value={item.actualPolicyDecision} /></td><td className="px-3 py-3 text-muted-foreground">{item.actualProviderOutcome.replaceAll("_", " ")}</td><td className="px-3 py-3">{item.passed ? <span className="inline-flex items-center gap-1 text-emerald-200"><Check className="size-3" />Pass</span> : <span className="text-destructive">Fail</span>}</td></tr>)}</tbody></table></div></section><AnimatePresence mode="wait">{selectedScenario ? <SelectedGlow key={selectedScenario.scenarioId} className="glass-panel h-fit rounded-xl border-primary/25 p-5"><motion.div initial={{ opacity: 0, x: 8 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0 }}><p className="eyebrow">Selected evidence</p><h3 className="mt-2 font-semibold">{selectedScenario.category.replaceAll("_", " ")}</h3><dl className="mt-4 grid gap-3 text-xs"><div><dt className="text-muted-foreground">Root cause</dt><dd className="mt-1">{selectedScenario.expectedRootCauseCategory} → {selectedScenario.actualRootCauseCategory}</dd></div><div><dt className="text-muted-foreground">Execution</dt><dd className="mt-1">{selectedScenario.actualExecutionBehavior.replaceAll("_", " ")}</dd></div><div><dt className="text-muted-foreground">Financial mutation</dt><dd className="mt-1">{money(selectedScenario.actualFinancialMutationMinor)}</dd></div><div><dt className="text-muted-foreground">Logical end-to-end latency</dt><dd className="mt-1">{selectedScenario.logicalLatencyMillis.endToEnd} ms</dd></div></dl><ol className="mt-5 space-y-2 border-l border-white/10 pl-3">{selectedScenario.auditEvents.map((event) => <li key={event} className="text-[11px] text-muted-foreground">{event.replaceAll("_", " ")}</li>)}</ol></motion.div></SelectedGlow> : <aside className="glass-panel h-fit rounded-xl p-6 text-center"><Info className="mx-auto size-5 text-muted-foreground" /><p className="mt-2 text-xs text-muted-foreground">Select a scenario to inspect its expected and actual evidence.</p></aside>}</AnimatePresence></div>;
}

export default function EvaluationPage() {
  const queryClient = useQueryClient(); const { emit } = useStatusIsland();
  const evaluation = useQuery({ queryKey: ["evaluation"], queryFn: api.evaluation, staleTime: 60_000 });
  const run = useMutation({ mutationFn: api.runEvaluation, onSuccess: (report) => { queryClient.setQueryData(["evaluation"], report); emit({ title: "Evaluation completed", detail: `${report.datasetSize} labelled scenarios verified` }); toast.success("Evaluation report regenerated from the fixed seed"); }, onError: (error: unknown) => toast.error(mutationErrorMessage(error)) });
  if (evaluation.isLoading) return <div><PageHeader eyebrow="Proof phase" title="Sentinel Evaluation Lab" description="Loading deterministic safety and quality evidence." /><LoadingGrid /></div>;
  if (evaluation.error || !evaluation.data) return <ErrorState error={evaluation.error as Error} retry={() => void evaluation.refetch()} />;
  const report = evaluation.data;
  const strategy = report.strategyPerformance.map((item) => ({ ...item, name: item.strategy.replaceAll("_", " "), rate: +(item.recoveryRate * 100).toFixed(1) }));
  return <div><PageHeader eyebrow="Proof phase" title="Sentinel Evaluation Lab" description="Reproducible evidence that detection, policy, recovery and webhook safeguards behave as specified under deterministic failures." onRefresh={() => void evaluation.refetch()} refreshing={evaluation.isFetching || run.isPending} updated={evaluation.dataUpdatedAt ? new Date(evaluation.dataUpdatedAt) : undefined} />
    <div className="mb-5 border-b border-white/[0.06] py-4 font-mono text-xs tracking-[0.15em] text-[#888888] uppercase">{report.datasetSize} scenarios · {report.policyCompliance.numerator}/{report.policyCompliance.denominator} policy compliance · {report.duplicateFinancialEffects} unsafe executions · {report.safetyGates.filter((gate) => gate.passed).length}/{report.safetyGates.length} gates</div>
    <div className="mb-5 grid gap-3 sm:grid-cols-2"><Link href="/evaluation/recovery-olympics" className="glass-panel rounded-xl p-4 transition-colors hover:border-primary/30"><p className="eyebrow">Synthetic / controlled benchmark</p><p className="mt-2 text-sm font-semibold">Open 10,000-case Recovery Olympics →</p><p className="mt-2 text-xs text-muted-foreground">Seven arms with economic, safety, latency and confidence-interval evidence.</p></Link><Link href="/evaluation/historical" className="glass-panel rounded-xl p-4 transition-colors hover:border-primary/30"><p className="eyebrow">Public-source historical validation</p><p className="mt-2 text-sm font-semibold">Open Razorpay Historical Case Explorer →</p><p className="mt-2 text-xs text-muted-foreground">Generated provenance counts and source-derived safety replays—never merchant transaction claims.</p></Link></div>
    <div className="mb-5 flex flex-col justify-between gap-3 rounded-xl border border-primary/20 bg-primary/[.055] p-4 sm:flex-row sm:items-center">
      <div>
        <p className="text-xs font-semibold text-primary">Sentinel Evaluation Lab — Razorpay Test Mode / Synthetic Evaluation</p>
        <p className="mt-1 text-[11px] text-muted-foreground">Report {report.reportVersion} · seed {report.seed} · {report.datasetSize} balanced scenarios</p>
      </div>
      <div className="flex flex-wrap gap-2">
        <Button size="sm" className="bg-[#1767d5] hover:bg-[#1459b8]" onClick={() => run.mutate()} disabled={run.isPending}>
          <Play />{run.isPending ? "Evaluating…" : "Run evaluation"}
        </Button>
        <Button variant="outline" size="sm" nativeButton={false} render={<a href={api.evaluationDownloadUrl("json")} />}>
          <FileJson2 />JSON
        </Button>
        <Button variant="outline" size="sm" nativeButton={false} render={<a href={api.evaluationDownloadUrl("md")} />}>
          <FileText />Markdown
        </Button>
        <Dialog>
          <DialogTrigger render={<Button variant="ghost" size="sm" />}><Info />Definitions</DialogTrigger>
          <DialogContent className="max-h-[80vh] overflow-y-auto sm:max-w-2xl">
            <DialogHeader>
              <DialogTitle>Metric definitions and evidence</DialogTitle>
              <DialogDescription>Authoritative metrics use deterministic comparisons, never an LLM-only grader.</DialogDescription>
            </DialogHeader>
            <div className="space-y-3">
              {report.metricDefinitions.map((item) => (
                <div key={item.metric} className="rounded-lg border p-3">
                  <p className="text-xs font-semibold">{item.metric}</p>
                  <p className="mt-1 font-mono text-[10px] text-primary">{item.formula}</p>
                  <p className="mt-2 text-xs text-muted-foreground">{item.numerator} / {item.denominator} · {item.evidence}</p>
                </div>
              ))}
            </div>
          </DialogContent>
        </Dialog>
      </div>
    </div>
    <section aria-label="Executive evaluation scorecard" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4"><ScoreCard label="Detection F1" score={report.detectionF1.value} note={`${report.detectionF1.numerator} / ${report.detectionF1.denominator} weighted terms`} icon={Gauge} /><ScoreCard label="Root-cause accuracy" score={report.rootCauseCategoryAccuracy.value} note={`${report.rootCauseCategoryAccuracy.numerator} / ${report.rootCauseCategoryAccuracy.denominator} labelled incidents`} icon={FlaskConical} /><ScoreCard label="Policy compliance" score={report.policyCompliance.value} note="Required gate: 100%" icon={ShieldCheck} /><ScoreCard label="Verified recovery" score={report.verifiedRecoveryRate.value} note={`${report.verifiedRecoveryRate.numerator} / ${report.verifiedRecoveryRate.denominator} attempts · ${compactMoney(report.recoveredAmountMinor)}`} icon={CircleDollarSign} /></section>
    <section className="mt-4 glass-panel rounded-xl p-4" aria-labelledby="safety-heading"><div className="flex items-center justify-between gap-3"><div><p className="eyebrow">Hard acceptance gates</p><h2 id="safety-heading" className="mt-2 font-semibold">Safety invariants</h2></div><span className="test-label">{report.safetyGates.every((gate) => gate.passed) ? "All gates pass" : "Gate failure"}</span></div><div className="mt-4 grid gap-2 sm:grid-cols-2 xl:grid-cols-4">{report.safetyGates.map((gate) => <div key={gate.gate} className={`rounded-lg border p-3 ${gate.passed ? "border-emerald-300/15 bg-emerald-300/[.035]" : "border-destructive/30 bg-destructive/8"}`}><div className="flex items-center gap-2">{gate.passed ? <Check className="size-3.5 text-emerald-200" /> : <TriangleAlert className="size-3.5 text-destructive" />}<p className="text-[11px] font-semibold">{gate.gate}</p></div><p className="mt-2 text-[10px] text-muted-foreground">Actual {gate.actual} · required {gate.required}</p></div>)}</div></section>
    <div className="mt-4 grid gap-4 lg:grid-cols-2"><ConfusionMatrix report={report} /><Funnel report={report} /></div>
    <section className="glass-panel mt-4 rounded-2xl p-5"><div className="flex flex-wrap items-end justify-between gap-2"><div><p className="eyebrow">Strategy comparison</p><h2 className="mt-2 text-lg font-semibold">Verified recovery by strategy</h2></div><p className="text-[10px] text-muted-foreground">Sample counts shown; synthetic mix is balanced, not prevalence-weighted.</p></div><ChartContainer config={chartConfig} className="mt-5 h-[250px] w-full"><BarChart accessibilityLayer data={strategy}><CartesianGrid vertical={false} stroke="var(--border)" /><XAxis dataKey="name" tickLine={false} axisLine={false} tick={{ fontSize: 10 }} /><YAxis tickLine={false} axisLine={false} tickFormatter={(value) => `${value}%`} tick={{ fontSize: 10 }} /><ChartTooltip content={<ChartTooltipContent />} /><Bar dataKey="rate" fill="var(--color-rate)" radius={[7, 7, 2, 2]} /></BarChart></ChartContainer><div className="mt-3 flex flex-wrap gap-2">{strategy.map((item) => <span key={item.strategy} className="rounded-full border px-2.5 py-1 text-[10px] text-muted-foreground">{item.name}: n={item.sampleCount}</span>)}</div></section>
    <Tabs defaultValue="scenarios" className="mt-4"><TabsList variant="line"><TabsTrigger value="scenarios">Scenario explorer</TabsTrigger><TabsTrigger value="failures">Failure laboratory</TabsTrigger><TabsTrigger value="limitations">Interpretation</TabsTrigger></TabsList><TabsContent value="scenarios" className="mt-4"><ScenarioExplorer scenarios={report.scenarios} /></TabsContent><TabsContent value="failures" className="mt-4"><section className="glass-panel rounded-xl p-5"><p className="eyebrow">Failure-injection timeline</p><ol className="mt-5 grid gap-3 lg:grid-cols-2">{report.failureInjectionMatrix.map((item, index) => <motion.li key={item.failure} initial={{ opacity: 0, y: 5 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: index * .03 }} className="rounded-xl border p-4"><div className="flex items-start justify-between gap-3"><div><p className="text-sm font-semibold">{item.failure}</p><p className="mt-1 text-[11px] text-muted-foreground">{item.scenarioCount} labelled scenarios</p></div><StateBadge value={item.bounded ? "BOUNDED" : "FAILED"} /></div><p className="mt-3 text-xs leading-5 text-muted-foreground">{item.observedBehavior}</p><p className="mt-2 text-[10px] text-primary">{item.evidence}</p></motion.li>)}</ol></section></TabsContent><TabsContent value="limitations" className="mt-4"><section className="glass-panel rounded-xl p-5"><p className="eyebrow">What this proves / what it does not prove</p><div className="mt-5 grid gap-4 md:grid-cols-2"><div className="rounded-xl border border-emerald-300/15 bg-emerald-300/[.035] p-4"><h3 className="text-sm font-semibold">This proves</h3><ul className="mt-3 space-y-2 text-xs leading-5 text-muted-foreground"><li>Deterministic policy, approval and stop gates match the independent oracle.</li><li>Duplicate and invalid webhook cases create no duplicate financial effect.</li><li>Provider and LLM failures remain bounded and auditable.</li><li>Identical seed and configuration produce identical authoritative metrics.</li></ul></div><div className="rounded-xl border p-4"><h3 className="text-sm font-semibold">This does not prove</h3><ul className="mt-3 space-y-2 text-xs leading-5 text-muted-foreground">{report.limitations.map((item) => <li key={item}>{item}</li>)}</ul></div></div></section></TabsContent></Tabs>
  </div>;
}
