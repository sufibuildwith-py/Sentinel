"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, ChevronLeft, ChevronRight, ExternalLink, Search } from "lucide-react";
import { api, money, shortId } from "@/lib/api";
import { paginate, safeExternalUrl } from "@/lib/truth";
import { filterHistoricalCases, filterOperationalCases } from "@/lib/operations-board";
import { TruthBadge } from "@/components/console-ui";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";

type Dataset = "OPERATIONAL" | "HISTORICAL" | "REPORT_IMPORTS" | "SYNTHETIC" | "REPLAY";
const datasets: Array<{ id: Dataset; label: string }> = [{ id: "OPERATIONAL", label: "Operational" }, { id: "HISTORICAL", label: "Historical Razorpay" }, { id: "REPORT_IMPORTS", label: "Report imports" }, { id: "SYNTHETIC", label: "Evaluation / Failure Lab" }, { id: "REPLAY", label: "Replay / shadow" }];

export function CaseBrowser() {
  const [dataset, setDataset] = useState<Dataset>("OPERATIONAL");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ALL");
  const [page, setPage] = useState(1);
  const incidents = useQuery({ queryKey: ["incidents"], queryFn: api.incidents });
  const historical = useQuery({ queryKey: ["historical-validation"], queryFn: api.historicalValidation, staleTime: 300_000 });
  const failureLab = useQuery({ queryKey: ["failure-lab-scenarios"], queryFn: api.failureLabScenarios, staleTime: 300_000 });
  const tower = useQuery({ queryKey: ["control-tower"], queryFn: api.controlTower });
  const refresh = () => { void incidents.refetch(); void historical.refetch(); void failureLab.refetch(); void tower.refetch(); };
  const q = search.trim().toLowerCase();
  const operationalRows = useMemo(() => filterOperationalCases(incidents.data ?? [], q, status), [incidents.data, q, status]);
  const historicalRows = useMemo(() => filterHistoricalCases(historical.data?.cases ?? [], q, status), [historical.data, q, status]);
  const syntheticRows = useMemo(() => (failureLab.data ?? []).filter((item) => `${item.title} ${item.mode} ${item.expectedSafetyOutcome}`.toLowerCase().includes(q)), [failureLab.data, q]);
  const replayRows = useMemo(() => (tower.data?.replayAndShadow.latestDifferences ?? []).filter((item) => `${item.productionAction} ${item.shadowAction} ${item.explanation}`.toLowerCase().includes(q)), [q, tower.data]);
  const count = dataset === "OPERATIONAL" ? operationalRows.length : dataset === "HISTORICAL" ? historicalRows.length : dataset === "REPORT_IMPORTS" ? 0 : dataset === "SYNTHETIC" ? syntheticRows.length : replayRows.length;
  const pageSize = 25;
  const pages = Math.max(1, Math.ceil(count / pageSize));
  const currentPage = Math.min(page, pages);
  const loading = incidents.isLoading || historical.isLoading || failureLab.isLoading || tower.isLoading;
  const error = incidents.error ?? historical.error ?? failureLab.error ?? tower.error;
  if (loading) return <Skeleton className="h-[660px] rounded-2xl" />;
  if (error) return <ErrorState error={error} retry={refresh} />;

  return <div><PageHeader eyebrow="Cases" title="Recovery case portfolio" description="Operational incidents, provenance-linked historical reports, synthetic safety scenarios, and shadow comparisons remain separate evidence universes." onRefresh={refresh} refreshing={incidents.isFetching || historical.isFetching || failureLab.isFetching || tower.isFetching} updated={incidents.dataUpdatedAt ? new Date(incidents.dataUpdatedAt) : undefined} />
    <div className="flex gap-2 overflow-x-auto pb-2" role="tablist" aria-label="Case dataset">{datasets.map((item) => <Button key={item.id} role="tab" aria-selected={dataset === item.id} variant={dataset === item.id ? "default" : "outline"} onClick={() => { setDataset(item.id); setStatus("ALL"); setPage(1); }}>{item.label}</Button>)}</div>
    {dataset !== "REPORT_IMPORTS" && <div className="mt-4 flex flex-col gap-3 sm:flex-row"><label className="relative flex-1"><span className="sr-only">Search cases</span><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input value={search} onChange={(event) => { setSearch(event.target.value); setPage(1); }} placeholder="Search case, failure, policy, or source" className="pl-9" /></label>{dataset !== "SYNTHETIC" && dataset !== "REPLAY" && <select value={status} onChange={(event) => { setStatus(event.target.value); setPage(1); }} aria-label="Filter case status" className="h-10 rounded-lg border border-input bg-white px-3 text-sm"><option value="ALL">All states</option>{(dataset === "HISTORICAL" ? ["PASS", "PARTIAL", "FAIL"] : ["DETECTED", "INVESTIGATING", "DIAGNOSED", "PLANNING", "POLICY_REVIEW", "APPROVED", "HUMAN_REVIEW", "EXECUTING", "MONITORING", "RECOVERED", "FAILED", "STOPPED"]).map((value) => <option key={value}>{value}</option>)}</select>}</div>}
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3"><p className="text-xs text-muted-foreground">{count} matching cases</p><TruthBadge label={dataset === "OPERATIONAL" ? "RAZORPAY TEST MODE" : dataset === "HISTORICAL" ? "HISTORICAL PUBLIC SOURCE" : dataset === "REPORT_IMPORTS" ? "MERCHANT-OWNED · ANALYSIS ONLY" : dataset === "SYNTHETIC" ? "SYNTHETIC BENCHMARK" : "SHADOW ONLY"} /></div>

    <section className="mt-4">
      {dataset === "OPERATIONAL" && <OperationalTable rows={paginate(operationalRows, currentPage, pageSize)} />}
      {dataset === "HISTORICAL" && <HistoricalTable rows={paginate(historicalRows, currentPage, pageSize)} />}
      {dataset === "REPORT_IMPORTS" && <Empty title="No merchant report imported" detail="No private Razorpay export is bundled with Sentinel. Payments, Combined, and Settlement Reconciliation files require a merchant-owned upload and must be sanitized before analysis; imported historical records never authorize provider execution." />}
      {dataset === "SYNTHETIC" && <SyntheticGrid rows={paginate(syntheticRows, currentPage, pageSize)} />}
      {dataset === "REPLAY" && <ReplayGrid rows={paginate(replayRows, currentPage, pageSize)} />}
    </section>
    {count > pageSize && <div className="mt-5 flex items-center justify-between"><p className="font-mono text-[9px] text-slate-400">PAGE {currentPage} / {pages}</p><div className="flex gap-2"><Button variant="outline" size="sm" disabled={currentPage === 1} onClick={() => setPage((value) => Math.max(1, value - 1))}><ChevronLeft />Previous</Button><Button variant="outline" size="sm" disabled={currentPage === pages} onClick={() => setPage((value) => Math.min(pages, value + 1))}>Next<ChevronRight /></Button></div></div>}
  </div>;
}

function OperationalTable({ rows }: { rows: Awaited<ReturnType<typeof api.incidents>> }) {
  if (!rows.length) return <Empty title="No operational cases" detail="Synthetic reset may have cleared the current operational view. Historical and evaluation evidence remain available in their own datasets." />;
  return <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white/75"><table className="w-full min-w-[920px] text-left"><thead><tr className="border-b border-slate-200 font-mono text-[9px] tracking-[.12em] text-slate-400 uppercase">{["Failure / ID", "Truth", "Status", "At risk", "Severity", "Policy", "Provider / outcome", ""].map((label) => <th key={label} className="px-4 py-3 font-medium">{label}</th>)}</tr></thead><tbody>{rows.map((item) => <tr key={item.incidentId} className="border-b border-slate-100 last:border-0"><td className="px-4 py-4"><p className="text-sm font-semibold">{item.type.replaceAll("_", " ")}</p><p className="mt-1 font-mono text-[9px] text-slate-400">{shortId(item.incidentId)}</p></td><td className="px-4 py-4"><TruthBadge label="OPERATIONAL · TEST MODE" /></td><td className="px-4 py-4"><StateBadge value={item.status} /></td><td className="px-4 py-4 font-mono text-sm font-semibold">{money(item.amountAtRiskMinor)}</td><td className="px-4 py-4 text-xs">{item.severity}</td><td className="px-4 py-4"><StateBadge value={item.policyDecision} /></td><td className="px-4 py-4 text-xs text-muted-foreground">{item.latestOutcome?.replaceAll("_", " ") ?? item.actionStatus?.replaceAll("_", " ") ?? "Not submitted"}</td><td className="px-4 py-4"><Button nativeButton={false} size="sm" variant="ghost" render={<Link href={`/incidents/${item.incidentId}`} />} aria-label={`Open ${item.type}`}><ArrowRight /></Button></td></tr>)}</tbody></table></div>;
}

function HistoricalTable({ rows }: { rows: Awaited<ReturnType<typeof api.historicalValidation>>["cases"] }) {
  if (!rows.length) return <Empty title="No historical cases match" detail="Adjust the search or result filter. No merchant transaction is inferred from this corpus." />;
  return <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white/75"><table className="w-full min-w-[1180px] text-left"><caption className="sr-only">Provenance-linked historical Razorpay failure and integration cases derived from public sources.</caption><thead><tr className="border-b border-slate-200 font-mono text-[9px] tracking-[.12em] text-slate-400 uppercase">{["Case", "Source class", "Date", "Surface", "Rail", "Failure", "Provider state", "Policy", "Result", "Provenance"].map((label) => <th key={label} className="px-4 py-3 font-medium">{label}</th>)}</tr></thead><tbody>{rows.map((item) => { const url = safeExternalUrl(item.sourceUrl); return <tr key={item.caseId} className="border-b border-slate-100 last:border-0"><td className="px-4 py-4 font-mono text-[10px] text-primary">{item.caseId}</td><td className="px-4 py-4"><TruthBadge label={item.evidenceLabel || "HISTORICAL PUBLIC SOURCE"} /></td><td className="px-4 py-4 text-xs">{item.sourceDate}</td><td className="px-4 py-4 text-xs">{item.productSurface.replaceAll("_", " ")}</td><td className="px-4 py-4 text-xs">{item.paymentRail.replaceAll("_", " ")}</td><td className="px-4 py-4 text-xs font-medium" title={item.normalizedFailureReason}>{item.normalizedFailureClass.replaceAll("_", " ")}</td><td className="px-4 py-4 text-xs">{item.providerState.replaceAll("_", " ")}</td><td className="px-4 py-4"><StateBadge value={item.policyDisposition} /></td><td className="px-4 py-4"><StateBadge value={item.result} /></td><td className="px-4 py-4">{url ? <a href={url} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 text-xs text-primary hover:underline">Public source <ExternalLink className="size-3" /></a> : <span className="text-xs text-slate-400">Unavailable</span>}</td></tr>; })}</tbody></table></div>;
}

function SyntheticGrid({ rows }: { rows: Awaited<ReturnType<typeof api.failureLabScenarios>> }) { if (!rows.length) return <Empty title="No synthetic scenarios match" detail="The Failure Lab remains separate from operational Test Mode truth." />; return <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">{rows.map((item) => <article key={item.id} className="glass-panel rounded-xl p-5"><div className="flex flex-wrap gap-2"><TruthBadge label={item.mode} /><StateBadge value={item.runnable ? "APPROVED" : "HUMAN_REVIEW"} /></div><h2 className="mt-4 text-sm font-semibold">{item.title}</h2><p className="mt-2 text-xs leading-5 text-muted-foreground">{item.description}</p><p className="mt-4 font-mono text-[9px] text-slate-500">EXPECTED · {item.expectedSafetyOutcome.replaceAll("_", " ")}</p><Button nativeButton={false} variant="outline" size="sm" className="mt-4" render={<Link href="/demo" />}>Open Failure Lab <ArrowRight /></Button></article>)}</div>; }

function ReplayGrid({ rows }: { rows: NonNullable<Awaited<ReturnType<typeof api.controlTower>>["replayAndShadow"]>["latestDifferences"] }) { if (!rows.length) return <Empty title="No replay differences" detail="No material shadow comparison is currently exposed by the backend." />; return <div className="grid gap-3 md:grid-cols-2">{rows.map((item) => <article key={item.id} className="glass-panel rounded-xl p-5"><div className="flex items-center justify-between gap-3"><TruthBadge label="SHADOW ONLY" /><StateBadge value={item.criticalRegression ? "FAILED" : "APPROVED"} /></div><p className="mt-4 font-mono text-xs">{item.productionAction} → {item.shadowAction}</p><p className="mt-3 text-xs leading-5 text-muted-foreground">{item.explanation}</p><div className="mt-4 grid grid-cols-2 gap-3 text-xs"><Fact label="Production policy" value={item.productionPolicy} /><Fact label="Shadow policy" value={item.shadowPolicy} /></div><p className="mt-4 text-[10px] text-slate-400">Advisory comparison only. No provider or customer tool path.</p></article>)}</div>; }

function Empty({ title, detail }: { title: string; detail: string }) { return <div className="glass-panel rounded-xl py-16 text-center"><p className="font-mono text-xs tracking-[.16em] text-slate-500 uppercase">{title}</p><p className="mx-auto mt-3 max-w-lg text-xs leading-5 text-muted-foreground">{detail}</p></div>; }
function Fact({ label, value }: { label: string; value: string }) { return <div><p className="font-mono text-[8px] text-slate-400 uppercase">{label}</p><p className="mt-1">{value}</p></div>; }
