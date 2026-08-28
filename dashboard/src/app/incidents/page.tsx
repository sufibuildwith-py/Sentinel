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

export default function IncidentsPage() {
  const query = useQuery({ queryKey: ["incidents"], queryFn: api.incidents });
  const [search, setSearch] = useState(""); const [state, setState] = useState("ALL");
  const rows = useMemo(() => (query.data ?? []).filter((incident) => (state === "ALL" || incident.status === state) && `${incident.type} ${incident.incidentId}`.toLowerCase().includes(search.toLowerCase())), [query.data, search, state]);
  return <div><PageHeader eyebrow="Incident operations" title="Revenue incidents" description="Search every detected anomaly and follow its evidence-backed recovery state." onRefresh={() => void query.refetch()} refreshing={query.isFetching} updated={query.dataUpdatedAt ? new Date(query.dataUpdatedAt) : undefined} />
    <div className="mb-4 flex flex-col gap-3 sm:flex-row"><label className="relative flex-1"><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><span className="sr-only">Search incidents</span><Input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search type or incident ID" className="h-10 pl-9" /></label><label><span className="sr-only">Filter by state</span><select value={state} onChange={(e) => setState(e.target.value)} className="h-10 w-full rounded-lg border border-input bg-card px-3 text-sm sm:w-48"><option>ALL</option><option>DETECTED</option><option>HUMAN_REVIEW</option><option>MONITORING</option><option>RECOVERED</option><option>STOPPED</option></select></label></div>
    {query.isLoading ? <div className="space-y-2">{Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="h-20 rounded-xl" />)}</div> : query.error ? <ErrorState error={query.error} retry={() => void query.refetch()} /> : rows.length === 0 ? <div className="glass-panel rounded-xl p-12 text-center"><p className="font-medium">No incidents match this view</p><p className="mt-2 text-sm text-muted-foreground">Change the search or inject a synthetic UPI outage.</p></div> : <div className="overflow-hidden rounded-xl border border-white/8 bg-card/65">
      <div className="hidden grid-cols-[1.5fr_.8fr_.8fr_.8fr_.9fr_32px] gap-4 border-b border-white/8 px-4 py-3 text-[10px] font-semibold tracking-[.12em] text-muted-foreground uppercase md:grid"><span>Incident</span><span>State</span><span>At risk</span><span>Strategy</span><span>Outcome</span><span /></div>
      {rows.map((incident) => <motion.div layoutId={`incident-${incident.incidentId}`} key={incident.incidentId}><Link href={`/incidents/${incident.incidentId}`} className="grid gap-3 border-b border-white/6 px-4 py-4 transition-colors last:border-0 hover:bg-white/[.025] md:grid-cols-[1.5fr_.8fr_.8fr_.8fr_.9fr_32px] md:items-center md:gap-4"><div><p className="text-sm font-medium">{incident.type.replaceAll("_", " ")}</p><p className="mt-1 text-[11px] text-muted-foreground">{shortId(incident.incidentId)} · {incident.severity} severity</p></div><StateBadge value={incident.status} /><div><span className="md:hidden eyebrow">At risk · </span><span className="text-sm font-medium">{money(incident.amountAtRiskMinor)}</span></div><p className="text-xs text-muted-foreground">{incident.strategy?.replaceAll("_", " ") ?? "Not planned"}</p><StateBadge value={incident.latestOutcome ?? incident.actionStatus} /><ArrowRight className="hidden size-4 text-muted-foreground md:block" /></Link></motion.div>)}
    </div>}
  </div>;
}
