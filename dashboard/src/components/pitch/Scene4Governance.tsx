"use client";

import { motion, useReducedMotion } from "motion/react";
import { MonoLabel } from "@/components/pitch/shared/MonoLabel";
import { SceneWrapper } from "@/components/pitch/shared/SceneWrapper";
import { StatusRow } from "@/components/pitch/shared/StatusRow";

const governanceStatement = "AI DOES NOT HAVE EXECUTION AUTHORITY";
const policyChecks = [
  ["AUTONOMOUS LIMIT", "PASS"],
  ["CUSTOMER RISK", "LOW"],
  ["CONFIDENCE", "94%"],
  ["DUPLICATE ACTION", "NONE"],
  ["RETRY POLICY", "ALLOWED"],
  ["TOOL PERMISSION", "GRANTED"],
] as const;

export function Scene4Governance() {
  const shouldReduceMotion = useReducedMotion();

  return (
    <SceneWrapper id="scene-4">
      <div className="w-full max-w-lg">
        <motion.div
          initial={shouldReduceMotion ? false : { opacity: 0, y: 12 }}
          whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
          transition={{ duration: 0.45, ease: "easeOut" }}
          viewport={{ once: true, amount: 0.7 }}
          className="border border-white/[0.06] bg-[#0f0f0f] p-6 font-mono"
        >
          <MonoLabel className="text-[#444444]">Recovery proposal</MonoLabel>
          <div className="my-5 h-px bg-white/[0.06]" />
          <dl className="space-y-3 text-sm text-[#888888]">
            <div className="flex justify-between gap-6"><dt>Action</dt><dd>Retry payment</dd></div>
            <div className="flex justify-between gap-6"><dt>Window</dt><dd>+20 min</dd></div>
            <div className="flex justify-between gap-6"><dt>Route</dt><dd>Alternate</dd></div>
            <div className="flex justify-between gap-6"><dt>Confidence</dt><dd className="text-[#f5f5f5]">94%</dd></div>
          </dl>
        </motion.div>

        <p className="mt-12 text-center font-mono text-[10px] leading-6 tracking-[0.16em] text-[#888888] uppercase sm:text-xs sm:tracking-[0.3em]" aria-label={governanceStatement}>
          {shouldReduceMotion ? governanceStatement : governanceStatement.split("").map((character, index) => (
            <motion.span
              key={`${character}-${index}`}
              aria-hidden="true"
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              transition={{ duration: 0.2, delay: 1 + index * 0.03, ease: "easeOut" }}
              viewport={{ once: true }}
            >
              {character}
            </motion.span>
          ))}
        </p>

        <div className="mt-10 border-t border-white/[0.06]">
          {policyChecks.map(([label, status], index) => (
            <StatusRow key={label} label={label} status={status} delay={shouldReduceMotion ? 0 : 2.3 + index * 0.15} />
          ))}
        </div>

        <motion.div
          initial={shouldReduceMotion ? false : { opacity: 0, scale: 0.95 }}
          whileInView={shouldReduceMotion ? undefined : { opacity: 1, scale: 1 }}
          transition={{ duration: 0.5, delay: 3.6, ease: "easeOut" }}
          viewport={{ once: true }}
          className="mt-12 text-center"
        >
          <MonoLabel className="text-[#444444]">Policy decision</MonoLabel>
          <p className="mt-4 font-mono text-3xl font-bold tracking-[0.2em] text-[#22c55e]">APPROVED</p>
        </motion.div>
      </div>
    </SceneWrapper>
  );
}
