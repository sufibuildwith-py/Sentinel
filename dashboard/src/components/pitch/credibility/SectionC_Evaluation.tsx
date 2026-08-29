"use client";

import { animate, motion, useInView, useMotionValue, useReducedMotion, useTransform } from "motion/react";
import { useEffect, useRef } from "react";
import { CredibilityShell } from "@/components/pitch/credibility/CredibilityShell";

const metrics = [
  { value: 464, suffix: "", label: "Total Scenarios", color: "text-[#f5f5f5]" },
  { value: 432, suffix: " / 432", label: "Policy Compliance", color: "text-[#f5f5f5]" },
  { value: 0, suffix: "", label: "Unsafe Autonomous Executions", color: "text-[#22c55e]" },
  { value: 8, suffix: " / 8", label: "Zero-Tolerance Gates", color: "text-[#f5f5f5]" },
] as const;

const categories = [
  "DUPLICATE PROTECTION", "CONFIDENCE THRESHOLDS", "RETRY LIMITS",
  "CUSTOMER RISK TIERS", "GATEWAY CLASSIFICATION", "POLICY DENIAL",
  "AUTONOMOUS LIMIT ENFORCEMENT", "HUMAN APPROVAL ROUTING",
  "EVIDENCE GROUNDING", "RECOVERY ELIGIBILITY", "INCIDENT CLUSTERING",
  "REVENUE QUANTIFICATION", "WEBHOOK VERIFICATION", "IDEMPOTENCY",
  "PAYMENT LINK CREATION", "EXECUTION AUTHORITY BOUNDARIES",
  "AT-RISK TRACKING", "RECOVERY RATE METRICS", "AUDIT TRAIL INTEGRITY",
  "FIXTURE MODE PARITY", "SEED REPRODUCIBILITY", "EDGE CASE COVERAGE",
  "BOUNDARY CONDITIONS", "NULL SAFETY", "STATUS TRANSITIONS",
  "CROSS-CATEGORY CONSISTENCY", "ZERO-TOLERANCE GATE ENFORCEMENT",
  "CI INTEGRATION", "DETERMINISTIC OUTCOMES",
] as const;

interface MetricTileProps {
  color: string;
  label: string;
  suffix: string;
  value: number;
}

function MetricTile({ color, label, suffix, value }: MetricTileProps) {
  const shouldReduceMotion = useReducedMotion();
  const tileRef = useRef<HTMLDivElement>(null);
  const isInView = useInView(tileRef, { once: true, amount: 0.5 });
  const counter = useMotionValue(0);
  const displayValue = useTransform(counter, (current) => `${Math.round(current)}${suffix}`);

  useEffect(() => {
    if (!isInView) return;
    if (shouldReduceMotion) {
      counter.set(value);
      return;
    }
    const controls = animate(counter, value, { duration: 1.2, ease: "easeOut" });
    return () => controls.stop();
  }, [counter, isInView, shouldReduceMotion, value]);

  return (
    <div ref={tileRef} className="rounded-sm border border-white/[0.06] bg-[#0f0f0f] p-6">
      {shouldReduceMotion ? (
        <p className={`font-mono text-3xl font-bold ${color}`}>{value}{suffix}</p>
      ) : (
        <motion.p className={`font-mono text-3xl font-bold ${color}`}>{displayValue}</motion.p>
      )}
      <p className="mt-2 font-mono text-[9px] tracking-[0.2em] text-[#444444] uppercase">{label}</p>
    </div>
  );
}

export function SectionCEvaluation() {
  const shouldReduceMotion = useReducedMotion();

  return (
    <CredibilityShell label="C / Evaluation harness" headline="464 scenarios. Zero unsafe autonomous executions.">
      <div className="mb-12 grid grid-cols-2 gap-4 md:grid-cols-4">
        {metrics.map((metric) => <MetricTile key={metric.label} {...metric} />)}
      </div>
      <div className="flex flex-wrap gap-2" aria-label="Evaluation categories">
        {categories.map((category, index) => (
          <motion.span
            key={category}
            initial={shouldReduceMotion ? false : { opacity: 0, y: 6 }}
            whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: shouldReduceMotion ? 0 : index * 0.03, ease: "easeOut" }}
            viewport={{ once: true, amount: 0.2 }}
            className="rounded-none border border-white/[0.04] px-2 py-1 font-mono text-[9px] tracking-[0.15em] text-[#444444] uppercase"
          >
            {category}
          </motion.span>
        ))}
      </div>
      <p className="mt-6 text-xs text-[#444444]">Reproducible on demand. Fixed seed 20260901. No live credentials required.</p>
    </CredibilityShell>
  );
}
