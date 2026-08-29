"use client";

import { motion, useReducedMotion } from "motion/react";
import { AlertCircle, ArrowUpRight, CheckCircle2, Clock3, RefreshCw, ShieldAlert } from "lucide-react";
import type { CSSProperties, PointerEvent, ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { updatedAt } from "@/lib/api";
import { fixtureMode } from "@/lib/environment";

export function PageHeader({ eyebrow, title, description, onRefresh, refreshing, updated }: { eyebrow: string; title: string; description: string; onRefresh?: () => void; refreshing?: boolean; updated?: Date; }) {
  return <div className="mb-6 flex flex-col justify-between gap-4 md:flex-row md:items-end"><div><p className="eyebrow">{eyebrow}</p><h1 className="mt-2 text-2xl font-semibold tracking-[-.03em] sm:text-3xl">{title}</h1><p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">{description}</p></div><div className="flex items-center gap-3 self-start md:self-auto"><span className="text-[11px] text-muted-foreground">Last updated {updated ? updatedAt(updated) : "awaiting data"}</span>{onRefresh && <Button variant="outline" size="sm" onClick={onRefresh} disabled={refreshing} aria-label="Refresh data"><RefreshCw className={cn(refreshing && "animate-spin")} /> Refresh</Button>}</div></div>;
}

export function StateBadge({ value }: { value?: string | null }) {
  const shouldReduceMotion = useReducedMotion();
  if (!value) return <span className="text-xs text-muted-foreground">Not started</span>;
  const normalized = value === "HUMAN_REVIEW" || value === "PENDING_APPROVAL" || value === "HUMAN" ? "HUMAN_REQUIRED" : value === "AUTO" ? "APPROVED" : value === "DENY" || value === "REJECTED" || value === "STOPPED" ? "DENIED" : value;
  const tones: Record<string, string> = {
    DETECTED: "border-[#333333] text-[#888888]",
    INVESTIGATING: "border-[#2563eb]/40 text-[#2563eb]",
    DIAGNOSED: "border-[#2563eb]/40 text-[#2563eb]",
    PLANNING: "border-[#f59e0b]/40 text-[#f59e0b]",
    POLICY_REVIEW: "border-[#f59e0b]/40 text-[#f59e0b]",
    PROPOSED: "border-[#f59e0b]/40 text-[#f59e0b]",
    APPROVED: "border-[#22c55e]/40 text-[#22c55e]",
    DENIED: "border-[#ef4444]/40 text-[#ef4444]",
    EXECUTING: "border-[#2563eb]/40 text-[#2563eb]",
    MONITORING: "border-[#2563eb]/40 text-[#2563eb]",
    RECOVERED: "border-[#22c55e]/40 text-[#22c55e]",
    EXECUTED: "border-[#22c55e]/40 text-[#22c55e]",
    FAILED: "border-[#ef4444]/40 text-[#ef4444]",
    HUMAN_REQUIRED: "border-[#f59e0b]/40 text-[#f59e0b]",
    BOUNDED: "border-[#22c55e]/40 text-[#22c55e]",
  };
  const danger = normalized === "DENIED" || normalized === "FAILED";
  const pending = ["PROPOSED", "PLANNING", "POLICY_REVIEW", "HUMAN_REQUIRED"].includes(normalized);
  const Icon = danger ? ShieldAlert : pending ? Clock3 : CheckCircle2;
  return <span className={cn("inline-flex items-center gap-1.5 rounded-full border bg-transparent px-2.5 py-1 text-[11px] font-semibold", tones[normalized] ?? "border-[#333333] text-[#888888]")}>
    {normalized === "EXECUTING" ? <span className={cn("size-1.5 rounded-full bg-[#2563eb]", !shouldReduceMotion && "animate-pulse")} /> : <Icon className="size-3" />}
    {normalized.replaceAll("_", " ")}
  </span>;
}

export function MetricCard({ label, value, note, icon: Icon }: { label: string; value: string; note: string; icon: typeof ArrowUpRight }) {
  return <div className="glass-panel rounded-xl p-5"><div className="flex items-center justify-between"><p className="eyebrow">{label}</p><Icon className="size-4 text-muted-foreground" /></div><motion.p key={value} initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} className="mt-5 text-2xl font-semibold tracking-[-.04em]">{value}</motion.p><p className="mt-2 text-xs text-muted-foreground">{note}</p></div>;
}

export function SelectedGlow({ children, className }: { children: ReactNode; className?: string }) {
  const move = (event: PointerEvent<HTMLDivElement>) => { const box = event.currentTarget.getBoundingClientRect(); event.currentTarget.style.setProperty("--pointer-x", `${event.clientX - box.left}px`); event.currentTarget.style.setProperty("--pointer-y", `${event.clientY - box.top}px`); };
  return <motion.div layout onPointerMove={move} className={cn("selected-glow", className)} style={{ "--pointer-x": "50%", "--pointer-y": "50%" } as CSSProperties}>{children}</motion.div>;
}

export function LoadingGrid() { return <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">{Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-36 rounded-xl" />)}</div>; }
export function ErrorState({ error, retry }: { error: Error; retry: () => void }) { return <div className="glass-panel rounded-xl p-8 text-center"><AlertCircle className="mx-auto size-6 text-destructive" /><h2 className="mt-3 font-mono text-xs tracking-[0.2em] text-[#444444] uppercase">Data unavailable</h2><p className="mt-2 font-mono text-xs text-[#444444]">The backend may be offline. Fixture mode: {fixtureMode ? "ON" : "OFF"}.</p><p className="sr-only">{error.message}</p><Button variant="outline" className="mt-4" onClick={retry}>Try again</Button></div>; }
