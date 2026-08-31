"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, ExternalLink, Search, ShieldCheck } from "lucide-react";
import { api } from "@/lib/api";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";

export default function HistoricalValidationPage() {
  const [filter, setFilter] = useState("");
  const query = useQuery({ queryKey: ["historical-validation"], queryFn: api.historicalValidation, staleTime: 300_000 });
  const visible = useMemo(() => (query.data?.cases ?? []).filter((item) => `${item.caseId} ${item.sourceClass} ${item.productSurface} ${item.normalizedFailureClass} ${item.policyDisposition}`.replaceAll("_", " ").toLowerCase().includes(filter.toLowerCase())).slice(0, 100), [filter, query.data]);
  if (query.isLoading) return <Skeleton className="h-[600px] rounded-2xl" />;
  if (query.error || !query.data) return <ErrorState error={query.error as Error} retry={() => void query.refetch()} />;
  const report = query.data;
  return <div><Link href="/evaluation" className="mb-4 inline-flex items-center gap-2 text-xs text-muted-foreground hover:text-foreground"><ArrowLeft className="size-3" />Back to Evaluation Lab</Link>
    <PageHeader eyebrow="Historical validation" title="Razorpay public-source case corpus" description="Provenance-linked public failure and integration reports, normalized into deterministic safety replays without inventing merchant transactions." onRefresh={() => void query.refetch()} refreshing={query.isFetching} updated={query.dataUpdatedAt ? new Date(query.dataUpdatedAt) : undefined} />
    <div className="mb-5 flex flex-wrap gap-2"><Truth label={report.truthLabel} /><Truth label={`CORPUS ${report.corpusVersion}`} /><Truth label={`SHA-256 ${report.manifestSha256.slice(0, 12)}`} /></div>
    {report.acceptedPublicSourceCases === 0 ? <Empty text={report.limitations[0]} /> : <>
      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5"><Kpi label="Verified sources" value={String(report.acceptedPublicSourceCases)} /><Kpi label="Derived replays" value={String(report.derivedReplayCount)} /><Kpi label="Unsafe executions" value={String(report.unsafeExecutions)} good /><Kpi label="Duplicate effects" value={String(report.duplicateFinancialEffects)} good /><Kpi label="Unverified claims" value={String(report.unverifiedRecoveryClaims)} good /></section>
      <section className="glass-panel mt-4 rounded-2xl p-5"><div className="grid gap-5 lg:grid-cols-[1fr_auto]"><div><p className="eyebrow">Corpus provenance</p><h2 className="mt-2 text-lg font-semibold">{report.oldestSourceDate} → {report.newestSourceDate}</h2><div className="mt-4 flex flex-wrap gap-2">{Object.entries(report.sourceComposition).map(([source, count]) => <span key={source} className="border border-white/10 px-2 py-1 font-mono text-[9px] uppercase tracking-wider text-[#888888]">{source.replaceAll("_", " ")} · {count}</span>)}</div></div><div className="flex items-center gap-2"><ShieldCheck className="size-4 text-[#22c55e]" /><span className="font-mono text-xs text-[#22c55e]">{report.passed}/{report.acceptedPublicSourceCases} PASS</span></div></div></section>
      <section className="glass-panel mt-4 rounded-2xl p-5"><div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center"><div><p className="eyebrow">Case explorer</p><p className="mt-1 text-xs text-muted-foreground">Showing {visible.length} of {report.cases.length}; public source opens in a new tab.</p></div><label className="relative"><span className="sr-only">Filter historical cases</span><Search className="absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" /><Input className="pl-9" placeholder="Filter case, surface, failure" value={filter} onChange={(event) => setFilter(event.target.value)} /></label></div><div className="mt-5 grid gap-3 md:grid-cols-2">{visible.map((item) => <article key={item.caseId} className="border border-white/[0.06] p-4"><div className="flex items-start justify-between gap-3"><div><p className="font-mono text-[10px] text-primary">{item.caseId}</p><h3 className="mt-2 text-sm font-semibold">{item.normalizedFailureClass.replaceAll("_", " ")}</h3></div><StateBadge value={item.result} /></div><dl className="mt-4 grid grid-cols-2 gap-3 text-[10px]"><Fact label="Source" value={item.sourceClass.replaceAll("_", " ")} /><Fact label="Date" value={item.sourceDate} /><Fact label="Surface" value={item.productSurface.replaceAll("_", " ")} /><Fact label="Policy" value={item.policyDisposition} /></dl><div className="mt-4 flex items-center justify-between gap-3"><span className="font-mono text-[9px] text-[#22c55e]">{item.safeRefusal ? "SAFE REFUSAL" : "NO EXECUTION"}</span><a href={item.sourceUrl} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 font-mono text-[9px] uppercase tracking-wider text-primary hover:underline">Public source <ExternalLink className="size-3" /></a></div></article>)}</div></section>
      <section className="glass-panel mt-4 rounded-2xl p-5"><p className="eyebrow">Methodology and limitations</p><ul className="mt-4 space-y-2">{report.limitations.map((line) => <li key={line} className="text-xs leading-5 text-muted-foreground">{line}</li>)}</ul></section>
    </>}
  </div>;
}

function Kpi({ label, value, good = false }: { label: string; value: string; good?: boolean }) { return <div className="glass-panel rounded-xl p-5"><p className={`font-mono text-2xl font-bold ${good ? "text-[#22c55e]" : ""}`}>{value}</p><p className="mt-2 font-mono text-[9px] uppercase tracking-[0.18em] text-[#444444]">{label}</p></div>; }
function Fact({ label, value }: { label: string; value: string }) { return <div><dt className="font-mono uppercase tracking-wider text-[#444444]">{label}</dt><dd className="mt-1 text-[#888888]">{value}</dd></div>; }
function Truth({ label }: { label: string }) { return <span className="border border-[#f59e0b]/30 px-2 py-1 font-mono text-[9px] uppercase tracking-[0.14em] text-[#f59e0b]">{label}</span>; }
function Empty({ text }: { text: string }) { return <div className="glass-panel rounded-2xl py-20 text-center"><p className="font-mono text-xs uppercase tracking-widest text-[#444444]">Historical corpus unavailable in fixture mode</p><p className="mx-auto mt-3 max-w-lg text-xs text-muted-foreground">{text}</p></div>; }
