"use client";

import Link from "next/link";
import type { PointerEvent, ReactNode } from "react";
import { motion, useReducedMotion, useScroll, useSpring } from "motion/react";
import {
  Activity,
  ArrowRight,
  BrainCircuit,
  Check,
  CircleDollarSign,
  Database,
  Fingerprint,
  Gauge,
  LockKeyhole,
  Radar,
  ShieldCheck,
  Sparkles,
  WalletCards,
  Webhook,
} from "lucide-react";
import { PitchShell } from "@/components/pitch/PitchShell";
import { cn } from "@/lib/utils";

const spring = { type: "spring" as const, stiffness: 120, damping: 24, mass: 0.8 };

function Reveal({ children, className }: { children: ReactNode; className?: string }) {
  const reduced = useReducedMotion();
  return <motion.div initial={reduced ? false : { opacity: 0, y: 28 }} whileInView={reduced ? undefined : { opacity: 1, y: 0 }} viewport={{ once: true, amount: 0.22 }} transition={{ duration: 0.7, ease: "easeOut" }} className={className}>{children}</motion.div>;
}

function SpotlightCard({ children, className }: { children: ReactNode; className?: string }) {
  const move = (event: PointerEvent<HTMLDivElement>) => {
    const bounds = event.currentTarget.getBoundingClientRect();
    event.currentTarget.style.setProperty("--spot-x", `${event.clientX - bounds.left}px`);
    event.currentTarget.style.setProperty("--spot-y", `${event.clientY - bounds.top}px`);
  };
  return <motion.div onPointerMove={move} whileHover={{ y: -4 }} transition={spring} className={cn("pitch-glass pitch-spotlight", className)}>{children}</motion.div>;
}

function Label({ children }: { children: ReactNode }) {
  return <span className="font-mono text-[10px] font-semibold tracking-[0.22em] text-slate-500 uppercase">{children}</span>;
}

function Chapter({ id, label, title, intro, children }: { id: string; label: string; title: string; intro: string; children: ReactNode }) {
  return <section id={id} className="relative mx-auto w-full max-w-7xl px-5 py-28 sm:px-8 lg:py-36"><Reveal><Label>{label}</Label><div className="mt-5 grid gap-5 lg:grid-cols-[1.05fr_.75fr] lg:items-end"><h2 className="max-w-3xl text-4xl font-semibold tracking-[-0.055em] text-slate-950 sm:text-6xl">{title}</h2><p className="max-w-xl text-base leading-7 text-slate-600 lg:justify-self-end">{intro}</p></div></Reveal><div className="mt-14">{children}</div></section>;
}

const pipeline = [
  [Radar, "Detect", "Rule-based anomaly detection isolates revenue-impacting clusters."],
  [BrainCircuit, "Investigate", "Specialist agents correlate evidence, context, and historical memory."],
  [ShieldCheck, "Govern", "Deterministic policy decides AUTO, HUMAN, or DENY."],
  [WalletCards, "Recover", "One replay-safe Razorpay Test Mode action is executed."],
] as const;

const proof = [
  ["464", "Deterministic scenarios"],
  ["432 / 432", "Policy-compliant decisions"],
  ["0", "Unsafe autonomous executions"],
  ["8 / 8", "Zero-tolerance gates"],
] as const;

const doctrine = ["AI proposes.", "Evidence supports.", "Policy decides.", "Tools execute.", "Outcomes teach."] as const;

function DoctrineRibbon() {
  const reduced = useReducedMotion();
  const fullDoctrine = doctrine.join(" ");

  return <section id="doctrine" className="relative mx-auto w-full max-w-7xl scroll-mt-24 px-5 pb-28 sm:px-8">
    <Reveal>
      <div className="pitch-glass relative overflow-hidden rounded-[2rem] px-6 py-10 text-center shadow-[0_30px_90px_rgba(37,99,235,.12)] sm:px-10 sm:py-14">
        {!reduced && <motion.div aria-hidden initial={{ x: "-140%" }} whileInView={{ x: "140%" }} viewport={{ once: true, amount: 0.6 }} transition={{ duration: 1.7, delay: 0.25, ease: "easeInOut" }} className="absolute inset-y-0 z-10 w-1/2 skew-x-[-18deg] bg-[linear-gradient(90deg,transparent,rgba(255,255,255,.86),transparent)] blur-xl" />}
        <div aria-hidden className="absolute inset-x-[12%] top-0 h-px bg-[linear-gradient(90deg,transparent,rgba(37,99,235,.55),transparent)]" />
        <Label>The Sentinel doctrine</Label>
        <p aria-label={fullDoctrine} className="relative z-20 mx-auto mt-6 flex max-w-5xl flex-wrap justify-center gap-x-3 gap-y-2 text-2xl font-semibold tracking-[-.035em] text-slate-950 sm:gap-x-4 sm:text-4xl">
          {doctrine.map((clause, index) => <motion.span key={clause} aria-hidden="true" initial={reduced ? false : { opacity: 0, y: 12, filter: "blur(8px)" }} whileInView={reduced ? undefined : { opacity: 1, y: 0, filter: "blur(0px)" }} viewport={{ once: true, amount: 0.7 }} transition={{ duration: 0.55, delay: index * 0.13, ease: "easeOut" }} className={index === 2 ? "text-blue-600" : index === 4 ? "text-emerald-600" : "text-slate-950"}>{clause}</motion.span>)}
        </p>
        <p className="relative z-20 mt-5 text-sm text-slate-500">A governed system where intelligence never bypasses authority.</p>
      </div>
    </Reveal>
  </section>;
}

export function FinalPitch() {
  const reduced = useReducedMotion();
  const { scrollYProgress } = useScroll();
  const progress = useSpring(scrollYProgress, { stiffness: 110, damping: 28, mass: 0.4 });

  return <PitchShell>
    <div className="pitch-canvas min-h-screen overflow-clip text-slate-950 selection:bg-blue-200/70">
      <motion.div aria-hidden className="fixed inset-x-0 top-0 z-[70] h-0.5 origin-left bg-blue-600" style={{ scaleX: reduced ? 0 : progress }} />
      <nav className="fixed inset-x-0 top-4 z-50 mx-auto flex w-[calc(100%-2rem)] max-w-5xl items-center justify-between rounded-full border border-white/70 bg-white/65 px-3 py-2 shadow-[0_14px_50px_rgba(15,23,42,0.10)] backdrop-blur-2xl" aria-label="Pitch navigation">
        <a href="#top" className="flex items-center gap-2 rounded-full px-3 py-2"><span className="grid size-7 place-items-center rounded-full bg-slate-950 text-white"><ShieldCheck className="size-3.5" /></span><span className="font-mono text-[11px] font-bold tracking-[0.18em]">SENTINEL</span></a>
        <div className="hidden items-center gap-1 rounded-full border border-slate-200/80 bg-white/70 p-1 md:flex"><a href="#intelligence" className="rounded-full px-4 py-2 text-xs text-slate-500 transition-colors hover:bg-white hover:text-slate-950">Intelligence</a><a href="#governance" className="rounded-full px-4 py-2 text-xs text-slate-500 transition-colors hover:bg-white hover:text-slate-950">Governance</a><a href="#proof" className="rounded-full px-4 py-2 text-xs text-slate-500 transition-colors hover:bg-white hover:text-slate-950">Proof</a></div>
        <motion.div whileHover={reduced ? undefined : { scale: 1.025 }} whileTap={reduced ? undefined : { scale: 0.98 }}><Link href="/console" className="inline-flex items-center gap-2 rounded-full bg-blue-600 px-4 py-2.5 text-xs font-semibold text-white shadow-[0_8px_24px_rgba(37,99,235,0.28)] transition-colors hover:bg-blue-700">Open console <ArrowRight className="size-3.5" /></Link></motion.div>
      </nav>

      <main id="top">
        <section className="relative flex min-h-[105svh] items-center px-5 pb-20 pt-32 sm:px-8">
          <div aria-hidden className="pitch-orb pitch-orb-one" /><div aria-hidden className="pitch-orb pitch-orb-two" />
          <div className="relative mx-auto grid w-full max-w-7xl gap-14 lg:grid-cols-[.9fr_1.1fr] lg:items-center">
            <Reveal>
              <div className="inline-flex items-center gap-2 rounded-full border border-blue-200/80 bg-blue-50/75 px-3 py-1.5 text-[11px] font-semibold text-blue-700 shadow-sm backdrop-blur-xl"><Sparkles className="size-3.5" />Governed AI revenue recovery</div>
              <h1 className="mt-7 max-w-3xl text-5xl font-semibold leading-[0.96] tracking-[-0.07em] text-slate-950 sm:text-7xl xl:text-[5.6rem]">Revenue recovery,<br /><span className="text-blue-600">under control.</span></h1>
              <p className="mt-7 max-w-xl text-lg leading-8 text-slate-600">Sentinel detects payment failure patterns, investigates root cause, applies deterministic policy, and recovers eligible revenue—without giving AI execution authority.</p>
              <div className="mt-9 flex flex-col gap-3 sm:flex-row"><motion.div whileHover={reduced ? undefined : { y: -2 }} transition={spring}><Link href="/console" className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-slate-950 px-6 py-3.5 text-sm font-semibold text-white shadow-[0_16px_36px_rgba(15,23,42,.22)] sm:w-auto">Launch Sentinel <ArrowRight className="size-4" /></Link></motion.div><a href="#story" className="inline-flex items-center justify-center rounded-full border border-white/90 bg-white/60 px-6 py-3.5 text-sm font-medium text-slate-700 shadow-sm backdrop-blur-xl transition-colors hover:bg-white">Watch the system work</a></div>
              <div className="mt-12 flex flex-wrap gap-x-8 gap-y-4 border-t border-slate-300/50 pt-6"><div><p className="font-mono text-xl font-bold">100%</p><p className="mt-1 text-xs text-slate-500">Policy compliance</p></div><div><p className="font-mono text-xl font-bold text-emerald-600">0</p><p className="mt-1 text-xs text-slate-500">Unsafe executions</p></div><div><p className="font-mono text-xl font-bold">464</p><p className="mt-1 text-xs text-slate-500">Proof scenarios</p></div></div>
            </Reveal>

            <Reveal className="relative">
              <div className="pitch-window relative overflow-hidden rounded-[2rem] border border-white/80 bg-white/55 p-2 shadow-[0_40px_100px_rgba(30,64,175,.18)] backdrop-blur-3xl">
                <div className="flex items-center justify-between border-b border-slate-200/60 px-4 py-3"><div className="flex gap-1.5"><span className="size-2.5 rounded-full bg-red-400" /><span className="size-2.5 rounded-full bg-amber-400" /><span className="size-2.5 rounded-full bg-emerald-400" /></div><span className="font-mono text-[9px] tracking-[.2em] text-slate-400">SENTINEL / INCIDENT 7B47C4EE</span><span className="rounded-full bg-emerald-50 px-2 py-1 text-[9px] font-bold text-emerald-600">SYSTEM ONLINE</span></div>
                <div className="pitch-grid rounded-[1.55rem] bg-white/76 p-5 sm:p-7">
                  <div className="flex flex-wrap items-start justify-between gap-4"><div><Label>UPI degradation</Label><p className="mt-2 text-3xl font-semibold tracking-tight">₹2,840 at risk</p><p className="mt-2 text-xs text-slate-500">41 payments · 38 customers · Bank X cohort</p></div><span className="rounded-full border border-blue-200 bg-blue-50 px-3 py-1.5 text-[10px] font-bold text-blue-700">MONITORING</span></div>
                  <div className="mt-7 grid grid-cols-4 gap-2">{["Detect", "Investigate", "Policy", "Recover"].map((step, index) => <motion.div key={step} initial={reduced ? false : { opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: .6 + index * .16 }} className="text-center"><div className={cn("mx-auto grid size-8 place-items-center rounded-full border", index < 3 ? "border-blue-200 bg-blue-600 text-white" : "border-emerald-200 bg-emerald-500 text-white")}>{index < 3 ? <Check className="size-4" /> : <Activity className="size-4" />}</div><p className="mt-2 text-[9px] font-semibold text-slate-500">{step}</p></motion.div>)}</div>
                  <SpotlightCard className="mt-7 rounded-2xl p-5"><div className="relative z-10 flex items-start gap-4"><div className="grid size-10 shrink-0 place-items-center rounded-xl bg-blue-600 text-white shadow-lg shadow-blue-200"><BrainCircuit className="size-5" /></div><div><p className="text-xs font-semibold text-slate-950">Root cause identified · 91% confidence</p><p className="mt-2 text-xs leading-5 text-slate-500">UPI issuer degradation. Historical memory: alternative links recovered 72.3% of value across similar incidents.</p></div></div></SpotlightCard>
                  <div className="mt-3 grid gap-3 sm:grid-cols-2"><div className="rounded-2xl border border-slate-200/70 bg-white/75 p-4"><Label>Policy decision</Label><p className="mt-2 font-mono text-lg font-bold text-emerald-600">APPROVED</p></div><div className="rounded-2xl border border-slate-200/70 bg-white/75 p-4"><Label>Verified outcome</Label><p className="mt-2 font-mono text-lg font-bold text-emerald-600">₹723 RECOVERED</p></div></div>
                </div>
              </div>
              <div className="absolute -bottom-5 -left-5 hidden rounded-2xl border border-white/80 bg-white/70 px-4 py-3 shadow-xl backdrop-blur-2xl sm:block"><p className="font-mono text-[9px] tracking-widest text-slate-400">TEST MODE</p><p className="mt-1 text-xs font-semibold text-slate-700">Signed webhook · exactly once</p></div>
            </Reveal>
          </div>
        </section>

        <DoctrineRibbon />

        <section id="story" className="relative mx-3 overflow-hidden rounded-[2.5rem] bg-slate-950 text-white sm:mx-6">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_15%,rgba(37,99,235,.35),transparent_32%),radial-gradient(circle_at_80%_70%,rgba(14,165,233,.2),transparent_30%)]" />
          <div className="relative mx-auto max-w-7xl px-5 py-28 sm:px-8 lg:py-40"><Reveal><Label>01 / The invisible leak</Label><div className="mt-6 grid gap-10 lg:grid-cols-[1fr_.8fr]"><h2 className="text-4xl font-semibold tracking-[-.055em] sm:text-6xl">Payments fail in milliseconds.<br /><span className="text-slate-400">Revenue disappears quietly.</span></h2><div><p className="text-lg leading-8 text-slate-300">Most systems record a failure. Sentinel asks whether failures form a recoverable incident—and proves every decision with numbers.</p><div className="mt-8 rounded-3xl border border-white/10 bg-white/[.06] p-6 backdrop-blur-xl"><div className="flex items-center justify-between"><span className="font-mono text-xs text-slate-400">ROLLING BASELINE</span><span className="font-mono text-xs text-red-300">−31.4 pts</span></div><div className="mt-5 flex h-28 items-end gap-2">{[78,82,80,79,83,77,51].map((height, index) => <motion.div key={index} initial={reduced ? false : { height: 0 }} whileInView={{ height: `${height}%` }} viewport={{ once: true }} transition={{ duration: .7, delay: index * .08, ease: "easeOut" }} className={cn("flex-1 rounded-t-md", index === 6 ? "bg-red-400" : "bg-blue-400/60")} />)}</div><div className="mt-3 flex justify-between text-[10px] text-slate-500"><span>Normal traffic</span><span>UPI degradation detected</span></div></div></div></div></Reveal></div>
        </section>

        <Chapter id="intelligence" label="02 / Agent intelligence" title="Many minds. One evidence chain." intro="Each agent owns one bounded reasoning role. Tools compute. Memory retrieves. The LLM never becomes the source of truth.">
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">{pipeline.map(([Icon, title, text], index) => <SpotlightCard key={title} className="min-h-64 rounded-[1.75rem] p-6"><div className="relative z-10"><div className="flex items-center justify-between"><span className="grid size-11 place-items-center rounded-2xl bg-blue-50 text-blue-600"><Icon className="size-5" /></span><span className="font-mono text-[10px] text-slate-400">0{index + 1}</span></div><h3 className="mt-10 text-xl font-semibold tracking-tight">{title}</h3><p className="mt-3 text-sm leading-6 text-slate-500">{text}</p></div></SpotlightCard>)}</div>
          <Reveal className="mt-6 grid gap-4 lg:grid-cols-[1.4fr_.6fr]"><SpotlightCard className="rounded-[1.75rem] p-7"><div className="relative z-10"><div className="flex items-center gap-3"><Database className="size-5 text-blue-600" /><Label>Historical memory / cosine similarity</Label></div><p className="mt-6 text-2xl font-semibold tracking-tight">“14 similar incidents. Alternative payment links recovered 72.3% of value.”</p><p className="mt-4 text-sm text-slate-500">Computed from persisted HistoricalIncident outcomes—not generated copy.</p></div></SpotlightCard><SpotlightCard className="rounded-[1.75rem] p-7"><div className="relative z-10"><Fingerprint className="size-6 text-blue-600" /><p className="mt-8 font-mono text-4xl font-bold">91%</p><p className="mt-2 text-sm text-slate-500">Validated diagnosis confidence</p></div></SpotlightCard></Reveal>
        </Chapter>

        <section id="governance" className="relative mx-3 overflow-hidden rounded-[2.5rem] border border-blue-100 bg-[linear-gradient(135deg,#eff6ff_0%,#ffffff_48%,#ecfeff_100%)] sm:mx-6"><div className="mx-auto max-w-7xl px-5 py-28 sm:px-8 lg:py-40"><Reveal><Label>03 / The governance contract</Label><h2 className="mt-5 max-w-4xl text-4xl font-semibold tracking-[-.055em] sm:text-6xl">AI proposes.<br />Policy decides.</h2><p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600">Execution authority is structurally separate from model reasoning. Mandatory stop rules always run first and cannot be skipped.</p></Reveal><div className="mt-14 grid gap-5 lg:grid-cols-[.8fr_1.2fr]"><Reveal><div className="rounded-[2rem] bg-slate-950 p-7 text-white shadow-[0_28px_80px_rgba(15,23,42,.22)]"><div className="flex items-center justify-between"><Label>Recovery proposal</Label><BrainCircuit className="size-5 text-blue-400" /></div><p className="mt-8 text-2xl font-semibold">Alternative payment link</p><dl className="mt-7 space-y-4 text-sm">{[["Confidence","91%"],["Amount","₹1,000"],["Risk","LOW"],["Authority","NONE"]].map(([key,value]) => <div key={key} className="flex justify-between border-b border-white/10 pb-3"><dt className="text-slate-500">{key}</dt><dd className={key === "Authority" ? "font-mono font-bold text-red-300" : "font-mono text-slate-200"}>{value}</dd></div>)}</dl></div></Reveal><Reveal><div className="pitch-glass rounded-[2rem] p-7"><div className="flex items-center justify-between"><Label>Deterministic policy scan</Label><LockKeyhole className="size-5 text-blue-600" /></div><div className="mt-6 divide-y divide-slate-200/70">{["Payment not already recovered","No duplicate-charge signal","Retry attempts below maximum","Strategy allowlisted","Amount within autonomous limit"].map((rule,index) => <motion.div key={rule} initial={reduced ? false : { opacity: 0, x: -10 }} whileInView={{ opacity: 1, x: 0 }} viewport={{ once: true }} transition={{ delay: index * .1 }} className="flex items-center justify-between py-4"><span className="text-sm text-slate-600">{rule}</span><span className="inline-flex items-center gap-1.5 font-mono text-[10px] font-bold text-emerald-600"><Check className="size-3.5" />PASS</span></motion.div>)}</div><div className="mt-7 flex items-end justify-between rounded-2xl border border-emerald-200 bg-emerald-50/70 p-5"><div><Label>Policy decision</Label><p className="mt-2 font-mono text-3xl font-bold tracking-tight text-emerald-600">APPROVED</p></div><ShieldCheck className="size-10 text-emerald-500" /></div></div></Reveal></div></div></section>

        <Chapter id="execution" label="04 / Closed-loop recovery" title="One action. One verified outcome." intro="Provider uncertainty, duplicate delivery, and webhook ordering are treated as expected operational conditions—not edge cases.">
          <div className="grid gap-5 lg:grid-cols-3">{[[WalletCards,"Create","A unique Razorpay reference safely recovers uncertain creates."],[Webhook,"Verify","Raw-body HMAC verification authenticates the webhook."],[CircleDollarSign,"Reconcile","Recovered revenue changes once, only after verified payment." ]].map(([Icon,title,text],index) => <Reveal key={String(title)}><div className="pitch-glass h-full rounded-[1.75rem] p-7"><div className="flex items-center gap-4"><span className="grid size-12 place-items-center rounded-2xl bg-slate-950 text-white"><Icon className="size-5" /></span><span className="font-mono text-[10px] text-slate-400">0{index+1}</span></div><h3 className="mt-10 text-2xl font-semibold">{String(title)}</h3><p className="mt-3 text-sm leading-6 text-slate-500">{String(text)}</p></div></Reveal>)}</div>
          <Reveal className="mt-6"><div className="overflow-hidden rounded-[2rem] border border-emerald-200/80 bg-[linear-gradient(120deg,rgba(236,253,245,.9),rgba(255,255,255,.82))] p-7 shadow-[0_30px_90px_rgba(16,185,129,.12)] sm:p-10"><div className="grid gap-8 md:grid-cols-[1fr_auto] md:items-center"><div><div className="flex items-center gap-2 font-mono text-[10px] font-bold tracking-[.18em] text-emerald-600"><span className="size-2 rounded-full bg-emerald-500" />SIGNED WEBHOOK VERIFIED</div><p className="mt-5 text-4xl font-semibold tracking-[-.05em] sm:text-6xl">₹723 recovered.</p><p className="mt-3 text-sm text-slate-500">Duplicate financial effects: 0 · immutable audit event persisted</p></div><div className="grid size-28 place-items-center rounded-full border border-emerald-200 bg-white/80 shadow-lg"><Check className="size-12 text-emerald-500" /></div></div></div></Reveal>
        </Chapter>

        <section id="proof" className="relative mx-3 overflow-hidden rounded-[2.5rem] bg-slate-950 text-white sm:mx-6"><div className="absolute inset-0 pitch-dark-grid opacity-70" /><div className="relative mx-auto max-w-7xl px-5 py-28 sm:px-8 lg:py-40"><Reveal><div className="flex items-center gap-2 text-blue-300"><Gauge className="size-4" /><Label>05 / Deterministic proof</Label></div><h2 className="mt-6 max-w-4xl text-4xl font-semibold tracking-[-.055em] sm:text-6xl">Trust is measured,<br /><span className="text-slate-400">not declared.</span></h2></Reveal><div className="mt-14 grid grid-cols-2 gap-3 lg:grid-cols-4">{proof.map(([value,label],index) => <motion.div key={label} initial={reduced ? false : { opacity: 0, y: 18 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }} transition={{ duration: .55, delay: index * .1 }} className="rounded-[1.5rem] border border-white/10 bg-white/[.06] p-5 backdrop-blur-xl sm:p-7"><p className={cn("font-mono text-3xl font-bold sm:text-4xl", value === "0" ? "text-emerald-400" : "text-white")}>{value}</p><p className="mt-4 text-xs leading-5 text-slate-400">{label}</p></motion.div>)}</div><Reveal className="mt-6"><div className="grid gap-0 overflow-hidden rounded-[2rem] border border-white/10 bg-white/[.05] md:grid-cols-3">{[["Evidence-bound","Every diagnosis traces to computed signals or validated output."],["Replay-safe","Database uniqueness prevents duplicate active recovery actions."],["Failure-bounded","Gemini and provider outages degrade safely and remain auditable."]].map(([title,text]) => <div key={title} className="border-b border-white/10 p-7 last:border-0 md:border-b-0 md:border-r md:last:border-r-0"><p className="font-semibold">{title}</p><p className="mt-3 text-sm leading-6 text-slate-400">{text}</p></div>)}</div></Reveal></div></section>

        <section className="relative mx-auto flex min-h-[90svh] max-w-5xl flex-col items-center justify-center px-5 py-32 text-center"><Reveal><div className="mx-auto grid size-16 place-items-center rounded-[1.4rem] bg-blue-600 text-white shadow-[0_20px_50px_rgba(37,99,235,.32)]"><ShieldCheck className="size-8" /></div><Label>Sentinel</Label><h2 className="mt-6 text-5xl font-semibold tracking-[-.065em] sm:text-7xl">Don’t just detect<br />lost revenue.</h2><p className="mt-5 text-4xl font-semibold tracking-[-.055em] text-blue-600 sm:text-6xl">Recover it—governed.</p><p className="mx-auto mt-7 max-w-xl text-base leading-7 text-slate-500">Explore the working operational console, inspect every decision, and run the complete synthetic Test Mode story.</p><div className="mt-10 flex flex-col justify-center gap-3 sm:flex-row"><motion.div whileHover={reduced ? undefined : { y: -3, scale: 1.015 }} transition={spring}><Link href="/console" className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-blue-600 px-7 py-4 text-sm font-semibold text-white shadow-[0_18px_40px_rgba(37,99,235,.25)] sm:w-auto">Enter Sentinel OS <ArrowRight className="size-4" /></Link></motion.div><a href="https://github.com/sufibuildwith-py/Sentinel" target="_blank" rel="noreferrer" className="inline-flex items-center justify-center rounded-full border border-slate-200 bg-white/70 px-7 py-4 text-sm font-medium text-slate-700 shadow-sm backdrop-blur-xl transition-colors hover:bg-white">View engineering proof</a></div></Reveal></section>
      </main>

      <footer className="mx-auto flex max-w-7xl flex-col gap-4 border-t border-slate-200/70 px-6 py-10 text-xs text-slate-400 sm:flex-row sm:items-center sm:justify-between"><span>Sentinel · Governed AI Revenue Recovery</span><span className="font-mono text-[10px] tracking-widest">TEST MODE / SYNTHETIC EVALUATION</span></footer>
    </div>
  </PitchShell>;
}
