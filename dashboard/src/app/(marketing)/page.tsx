"use client";

import dynamic from "next/dynamic";
import Link from "next/link";
import { motion, useReducedMotion } from "motion/react";
import { PitchShell } from "@/components/pitch/PitchShell";
import { Scene1Problem } from "@/components/pitch/Scene1Problem";
import { Scene2Identity } from "@/components/pitch/Scene2Identity";
import { Scene4Governance } from "@/components/pitch/Scene4Governance";
import { Scene5Execution } from "@/components/pitch/Scene5Execution";
import { Scene6Closing } from "@/components/pitch/Scene6Closing";
import { SectionASignals } from "@/components/pitch/credibility/SectionA_Signals";
import { SectionBReasoning } from "@/components/pitch/credibility/SectionB_Reasoning";
import { SectionCEvaluation } from "@/components/pitch/credibility/SectionC_Evaluation";
import { SectionDGovernance } from "@/components/pitch/credibility/SectionD_Governance";

const Scene3Pipeline = dynamic(() => import("@/components/pitch/Scene3Pipeline"), {
  ssr: false,
  loading: () => <section id="scene-3" className="min-h-screen bg-[#080808]" aria-label="Loading intelligence pipeline" />,
});

export default function MarketingPage() {
  const shouldReduceMotion = useReducedMotion();

  return (
    <PitchShell>
      <motion.nav
        initial={shouldReduceMotion ? false : { opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.6, delay: shouldReduceMotion ? 0 : 0.3, ease: "easeOut" }}
        className="fixed inset-x-0 top-0 z-50 border-b border-white/[0.04] bg-[#080808]/80 backdrop-blur-sm"
        aria-label="Pitch navigation"
      >
        <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-6">
          <span className="font-mono text-xs tracking-[0.3em] text-[#444444] uppercase">Sentinel</span>
          <Link href="/console" className="bg-[#2563eb] px-4 py-1.5 font-mono text-xs tracking-widest text-white transition-colors duration-200 ease-out hover:bg-[#1d4ed8] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2563eb]">
            Launch Sentinel →
          </Link>
        </div>
      </motion.nav>

      <main>
        <Scene1Problem />
        <Scene2Identity />
        <Scene3Pipeline />
        <Scene4Governance />
        <Scene5Execution />
        <Scene6Closing />
        <div id="technical-evidence" className="flex w-full flex-col items-center gap-3 py-24">
          <div className="h-16 w-px bg-gradient-to-b from-transparent to-white/10" />
          <span className="font-mono text-[9px] tracking-[0.3em] text-[#333333] uppercase">Technical Evidence</span>
          <div className="h-16 w-px bg-gradient-to-b from-white/10 to-transparent" />
        </div>
        <SectionASignals />
        <SectionBReasoning />
        <SectionCEvaluation />
        <SectionDGovernance />
      </main>
      <footer className="mx-auto flex w-full max-w-4xl flex-col items-center justify-between gap-4 border-t border-white/[0.04] px-6 py-12 sm:flex-row">
        <span className="font-mono text-[9px] tracking-[0.3em] text-[#333333] uppercase">Sentinel — Governed AI Revenue Recovery</span>
        <div className="flex gap-6">
          <a href="/console" className="font-mono text-[9px] tracking-[0.2em] text-[#444444] uppercase transition-colors duration-200 hover:text-[#888888]">Launch Console</a>
          <a href="https://github.com/sufibuildwith-py/Sentinel" target="_blank" rel="noopener noreferrer" className="font-mono text-[9px] tracking-[0.2em] text-[#444444] uppercase transition-colors duration-200 hover:text-[#888888]">GitHub</a>
        </div>
      </footer>
    </PitchShell>
  );
}
