"use client";

import { useState } from "react";
import { Check, Circle, Pause, ShieldX, TriangleAlert } from "lucide-react";
import { motion } from "motion/react";
import type { AuditEntry } from "@/lib/types";
import { executionLedger } from "@/lib/recovery-session";
import { pipeline, type PipelineStageView } from "@/lib/pipeline";
import { StateBadge } from "@/components/dashboard-ui";
import { Skeleton } from "@/components/ui/skeleton";

export function LivePipeline({ stages }: { stages: PipelineStageView[] }) {
  const [selected, setSelected] = useState<number | null>(null);
  return <>
    <ol className="mt-8 grid grid-cols-3 gap-y-5 sm:grid-cols-7 xl:grid-cols-[repeat(13,minmax(0,1fr))]" aria-label="Live recovery pipeline">
      {pipeline.map((label, index) => {
        const stage = stages[index];
        return <li key={label} className="relative flex flex-col items-center text-center">
          <div className="absolute left-0 right-0 top-3 hidden h-px bg-slate-200 sm:block" />
          <button type="button" aria-label={`${label}: ${stage.state.replaceAll("_", " ")}`} aria-pressed={selected === index} onClick={() => setSelected(index)} className="relative z-10 flex flex-col items-center">
            <StageNode stage={stage} />
            <span className={`mt-2 text-[10px] font-medium ${["COMPLETE", "ACTIVE"].includes(stage.state) ? "text-foreground" : "text-muted-foreground"}`}>{label}</span>
            <span className="mt-1 font-mono text-[7px] text-slate-400">{stage.state.replaceAll("_", " ")}</span>
          </button>
        </li>;
      })}
    </ol>
    {selected != null && <div className="mt-5 border-t border-slate-200 pt-4" aria-live="polite">
      <div className="flex flex-wrap items-center gap-2"><p className="font-mono text-[9px] tracking-[0.2em] text-primary uppercase">{stages[selected].label} · {stages[selected].state.replaceAll("_", " ")}</p>{stages[selected].timestamp && <time className="font-mono text-[9px] text-slate-400">{new Date(stages[selected].timestamp).toLocaleString()}</time>}</div>
      <p className="mt-2 text-xs leading-5 text-muted-foreground">{stages[selected].evidence}</p>
      {stages[selected].actor && <p className="mt-2 font-mono text-[9px] text-slate-400">PERSISTED BY {stages[selected].actor}</p>}
    </div>}
  </>;
}

function StageNode({ stage }: { stage: PipelineStageView }) {
  const blocked = ["BLOCKED", "FAILED"].includes(stage.state);
  const held = stage.state === "HELD";
  const active = stage.state === "ACTIVE";
  const complete = stage.state === "COMPLETE";
  const className = blocked ? "border-red-300 bg-red-50 text-red-600" : held ? "border-amber-300 bg-amber-50 text-amber-600" : complete ? "border-primary/50 bg-primary text-white" : active ? "border-primary bg-blue-50 text-primary shadow-[0_0_0_4px_rgba(37,99,235,.08)]" : "border-slate-200 bg-card text-muted-foreground";
  return <motion.span initial={false} animate={active ? { opacity: [1, .62, 1] } : { opacity: 1 }} transition={active ? { duration: 1.6, repeat: Infinity, ease: "easeInOut" } : { duration: .15 }} className={`grid size-7 place-items-center rounded-full border ${className}`}>
    {complete ? <Check className="size-3.5" /> : blocked ? <ShieldX className="size-3.5" /> : held ? <Pause className="size-3.5" /> : stage.state === "FAILED" ? <TriangleAlert className="size-3.5" /> : <Circle className="size-2.5" />}
  </motion.span>;
}

export function LiveExecutionLedger({ entries, loading, watching }: { entries: AuditEntry[]; loading: boolean; watching: boolean }) {
  if (loading) return <Skeleton className="h-64 rounded-xl" />;
  const rows = executionLedger(entries);
  if (!rows.length) return <p className="py-4 font-mono text-xs tracking-[0.16em] text-[#444444] uppercase">No persisted execution events</p>;
  return <div className="glass-panel overflow-hidden rounded-xl">
    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-5 py-4"><div><p className="font-mono text-[10px] font-semibold tracking-[.18em] uppercase">Live execution ledger</p><p className="mt-1 text-xs text-muted-foreground">Append-only events from Sentinel’s persisted audit trail</p></div><div className="flex items-center gap-2"><span className={`size-1.5 rounded-full ${watching ? "bg-primary" : "bg-slate-300"}`} /><span className="font-mono text-[9px] text-slate-400">{watching ? "WATCHING PERSISTED STATE" : "HISTORY"}</span></div></div>
    <ol className="max-h-[520px] overflow-y-auto" aria-live="polite" aria-relevant="additions">
      {rows.map((row) => <motion.li key={row.eventId} initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: .12, ease: "easeOut" }} className="grid gap-2 border-b border-slate-100 px-5 py-4 last:border-0 sm:grid-cols-[86px_92px_120px_1fr_auto] sm:items-start">
        <time className="font-mono text-[9px] text-slate-400">{new Date(row.timestamp).toLocaleTimeString()}</time>
        <span className="font-mono text-[9px] font-semibold text-primary">{row.actor}</span>
        <span className="truncate font-mono text-[9px] text-slate-500" title={row.stage}>{row.stage.replaceAll("_", " ")}</span>
        <div><p className="text-xs leading-5 text-slate-600">{row.message}</p>{row.evidence.length > 0 && <p className="mt-1 line-clamp-2 font-mono text-[9px] leading-4 text-slate-400">{row.evidence.join(" · ")}</p>}</div>
        <StateBadge value={row.truthClass.replaceAll("_", " ")} />
      </motion.li>)}
    </ol>
  </div>;
}
