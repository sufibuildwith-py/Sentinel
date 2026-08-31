"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, CheckCircle2, FileKey2 } from "lucide-react";
import { api, shortId } from "@/lib/api";
import { ConsolePanel, PartialState, TruthBadge } from "@/components/console-ui";
import { ErrorState, PageHeader, StateBadge } from "@/components/dashboard-ui";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

export default function AuditPage() {
  const incidents = useQuery({ queryKey: ["incidents"], queryFn: api.incidents });
  const [selected, setSelected] = useState("");
  const incidentId = selected || incidents.data?.[0]?.incidentId || "";
  const audit = useQuery({ queryKey: ["audit", incidentId], queryFn: () => api.audit(incidentId), enabled: Boolean(incidentId) });
  const certificates = useQuery({ queryKey: ["certificates", incidentId], queryFn: () => api.decisionCertificates(incidentId), enabled: Boolean(incidentId) });
  if (incidents.isLoading) return <Skeleton className="h-[620px] rounded-2xl" />;
  if (incidents.error) return <ErrorState error={incidents.error} retry={() => void incidents.refetch()} />;
  return <div><PageHeader eyebrow="Audit" title="Immutable decision evidence" description="Reconstruct agent results, policy trace, authority, execution and reconciliation without exposing customer PII, provider payloads or secrets." />
    <div className="mb-5 flex flex-wrap gap-2"><TruthBadge label="APPEND ONLY" /><TruthBadge label="PROVIDER CONFIRMED" /><TruthBadge label="AWAITING RECONCILIATION" /></div>
    <label className="mb-4 block max-w-xl"><span className="eyebrow">Incident</span><select className="mt-2 h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm" value={incidentId} onChange={(event) => setSelected(event.target.value)}>{(incidents.data ?? []).map((item) => <option key={item.incidentId} value={item.incidentId}>{shortId(item.incidentId)} · {item.type.replaceAll("_", " ")}</option>)}</select></label>
    <section className="grid gap-4 xl:grid-cols-[1.25fr_.75fr]"><ConsolePanel eyebrow="Chronological trace" title={`${audit.data?.length ?? 0} persisted audit events`}><div className="space-y-0">{audit.data?.map((entry) => <article key={entry.eventId} className="relative border-l border-slate-200 py-3 pl-5 before:absolute before:-left-1 before:top-5 before:size-2 before:rounded-full before:bg-primary"><div className="flex flex-wrap items-center gap-2"><time className="font-mono text-[9px] text-slate-400">{new Date(entry.timestamp).toLocaleString()}</time><StateBadge value={entry.policyResult ?? entry.stage} /></div><p className="mt-2 text-sm font-medium text-slate-800">{entry.narrative}</p><p className="mt-1 text-xs text-slate-500">Actor: {entry.actor} · Stage: {entry.stage.replaceAll("_", " ")}</p></article>)}{!audit.isLoading && audit.data?.length === 0 && <PartialState title="No audit entries" detail="No audit event exists for this incident; Sentinel does not synthesize a trace." />}</div></ConsolePanel>
      <ConsolePanel eyebrow="Decision certificates" title={`${certificates.data?.length ?? 0} verifiable records`}><FileKey2 className="size-4 text-primary" /><div className="mt-4 space-y-3">{certificates.data?.map((certificate) => <article key={certificate.id} className="rounded-xl border border-slate-200 bg-white/70 p-4"><div className="flex items-start justify-between gap-2"><p className="font-mono text-[10px] font-semibold">{certificate.decisionType.replaceAll("_", " ")}</p><TruthBadge label={certificate.finalTruthState} /></div><p className="mt-3 text-sm font-semibold">{certificate.selectedAction.replaceAll("_", " ")}</p><p className="mt-2 break-all font-mono text-[9px] text-slate-400">SHA-256 {certificate.certificateSha256}</p><div className="mt-3 flex items-center gap-2 text-xs text-emerald-700"><CheckCircle2 className="size-3" />Policy {certificate.policyVersion} · model {certificate.modelVersion}</div></article>)}{!certificates.isLoading && certificates.data?.length === 0 && <PartialState title="No certificate yet" detail="A certificate appears only after a qualifying persisted decision." />}</div><Button nativeButton={false} variant="outline" className="mt-5 w-full" render={<Link href={incidentId ? `/incidents/${incidentId}` : "/incidents"} />}>Open incident evidence <ArrowRight /></Button></ConsolePanel></section>
  </div>;
}
