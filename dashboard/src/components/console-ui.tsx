"use client";

import { AlertTriangle, CheckCircle2, CircleDashed, Database, Eye, FlaskConical, History, ShieldCheck } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";
import { truthKind } from "@/lib/truth";

const truthStyles = {
  PROVIDER: "border-emerald-200 bg-emerald-50 text-emerald-700",
  PENDING: "border-amber-200 bg-amber-50 text-amber-700",
  SIMULATION: "border-violet-200 bg-violet-50 text-violet-700",
  HISTORICAL: "border-cyan-200 bg-cyan-50 text-cyan-700",
  SHADOW: "border-slate-300 bg-slate-100 text-slate-650",
  OBSERVATIONAL: "border-sky-200 bg-sky-50 text-sky-700",
  CONTROLLED: "border-indigo-200 bg-indigo-50 text-indigo-700",
  NEUTRAL: "border-slate-200 bg-white text-slate-600",
};

const truthIcons = { PROVIDER: CheckCircle2, PENDING: CircleDashed, SIMULATION: FlaskConical, HISTORICAL: History, SHADOW: Eye, OBSERVATIONAL: Database, CONTROLLED: ShieldCheck, NEUTRAL: ShieldCheck };

export function TruthBadge({ label, className }: { label: string; className?: string }) {
  const kind = truthKind(label);
  const Icon = truthIcons[kind];
  return <span className={cn("truth-chip", truthStyles[kind], className)}><Icon className="size-3" />{label.replaceAll("_", " ")}</span>;
}

export function ConsolePanel({ title, eyebrow, children, className }: { title: string; eyebrow?: string; children: ReactNode; className?: string }) {
  return <section className={cn("glass-panel rounded-2xl p-5 sm:p-6", className)}>{eyebrow && <p className="eyebrow">{eyebrow}</p>}<h2 className={cn("font-semibold tracking-[-.02em] text-slate-900", eyebrow && "mt-2")}>{title}</h2><div className="mt-5">{children}</div></section>;
}

export function PartialState({ title, detail }: { title: string; detail: string }) {
  return <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50/70 px-5 py-8 text-center"><AlertTriangle className="mx-auto size-5 text-amber-500" /><p className="mt-3 font-mono text-[10px] font-semibold tracking-[.16em] text-slate-600 uppercase">{title}</p><p className="mx-auto mt-2 max-w-lg text-xs leading-5 text-slate-500">{detail}</p></div>;
}

export function EvidenceLine({ label, value }: { label: string; value: ReactNode }) {
  return <div className="flex flex-col gap-1 border-b border-slate-200/70 py-3 last:border-0 sm:flex-row sm:items-start sm:justify-between sm:gap-6"><span className="font-mono text-[9px] font-semibold tracking-[.14em] text-slate-400 uppercase">{label}</span><span className="max-w-2xl text-sm text-slate-700 sm:text-right">{value}</span></div>;
}
