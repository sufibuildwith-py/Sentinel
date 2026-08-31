"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, Search } from "lucide-react";
import { motion } from "motion/react";
import { api, money, shortId } from "@/lib/api";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";

const statusOptions = ["ALL", "DETECTED", "INVESTIGATING", "DIAGNOSED", "PLANNING", "POLICY_REVIEW", "APPROVED", "HUMAN_REVIEW", "EXECUTING", "MONITORING", "RECOVERED", "FAILED", "STOPPED"];

export default function IncidentsPage() {
  const query = useQuery({ queryKey: ["incidents"], queryFn: api.incidents });
  const [search, setSearch] = useState(""); const [state, setState] = useState("ALL");
  const rows = useMemo(() => (query.data ?? []).filter((incident) => (state === "ALL" || incident.status === state) && `${incident.type} ${incident.incidentId}`.toLowerCase().includes(search.toLowerCase())), [query.data, search, state]);
  return <div><PageHeader eyebrow="Incident operations" title="Revenue incidents" description="Search every detected anomaly and follow its evidence-backed recovery state." onRefresh={() => void query.refetch()} refreshing={query.isFetching} updated={query.dataUpdatedAt ? new Date(query.dataUpdatedAt) : undefined} />
    <div className="mb-4 flex flex-col gap-3 sm:flex-row"><label className="relative flex-1"><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><span className="sr-only">Search incidents</span><Input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search type or incident ID" className="h-10 pl-9" /></label><label><span className="sr-only">Filter by state</span><select value={state} onChange={(e) => setState(e.target.value)} className="h-10 w-full rounded-lg border border-input bg-card px-3 text-sm sm:w-48">{statusOptions.map((status) => <option key={status}>{status}</option>)}</select></label></div>
    {query.isLoading ? <div className="space-y-2">{Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="h-20 rounded-xl" />)}</div> : query.error ? <ErrorState error={query.error} retry={() => void query.refetch()} /> : rows.length === 0 ? <div className="py-16 text-center font-mono text-xs text-slate-500"><p className="tracking-[0.2em] uppercase">No incidents detected</p><p className="mt-2">System is monitoring. Revenue is protected.</p></div> : <div className="overflow-hidden rounded-xl border border-slate-200 bg-white/70 shadow-sm">
      <div className="hidden grid-cols-[1.35fr_.75fr_.8fr_.75fr_1fr_.75fr_32px] gap-4 border-b border-slate-200 px-4 py-3 text-[10px] font-semibold tracking-[.12em] text-muted-foreground uppercase lg:grid"><span>Failure type</span><span>Status</span><span>Amount at risk</span><span>Priority</span><span>Strategy</span><span>Policy</span><span /></div>
      {rows.map((incident) => {
        const recovered = incident.status === "RECOVERED";
        return <motion.div layoutId={`incident-${incident.incidentId}`} key={incident.incidentId}><Link href={`/incidents/${incident.incidentId}`} className="grid gap-3 border-b border-slate-100 px-4 py-4 transition-colors last:border-0 hover:bg-blue-50/50 lg:grid-cols-[1.35fr_.75fr_.8fr_.75fr_1fr_.75fr_32px] lg:items-center lg:gap-4"><div><p className="text-sm font-semibold text-slate-900">{incident.type.replaceAll("_", " ")}</p><p className="mt-1 font-mono text-[10px] text-slate-400">{shortId(incident.incidentId)}</p></div><StateBadge value={incident.status} /><div><span className="lg:hidden eyebrow">Amount at risk · </span><span className={`font-mono text-sm font-semibold ${recovered ? "text-[#22c55e]" : "text-[#ef4444]"}`}>{money(incident.amountAtRiskMinor)}</span></div><p className="text-xs text-muted-foreground"><span className="lg:hidden eyebrow">Priority · </span>{incident.severity}</p><p className="text-xs text-slate-500">{incident.strategy?.replaceAll("_", " ") ?? "Not planned"}</p><StateBadge value={incident.policyDecision} /><ArrowRight className="hidden size-4 text-muted-foreground lg:block" /></Link></motion.div>;
      })}
    </div>}
  </div>;
}
