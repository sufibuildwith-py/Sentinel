"use client";

import { motion, useReducedMotion } from "motion/react";
import { CredibilityShell } from "@/components/pitch/credibility/CredibilityShell";

const signals = [
  { label: "Payment event", value: "payment.failed", color: "text-[#888888]" },
  { label: "Failure reason", value: "Gateway timeout", color: "text-[#888888]" },
  { label: "Customer tenure", value: "18 months", color: "text-[#f5f5f5]" },
  { label: "Prior payments", value: "13 successful", color: "text-[#22c55e]" },
  { label: "Risk signal", value: "LOW", color: "text-[#22c55e]" },
  { label: "Gateway context", value: "Razorpay Test Mode", color: "text-[#888888]" },
  { label: "Policy context", value: "Retry window: +20 min", color: "text-[#888888]" },
] as const;

export function SectionASignals() {
  const shouldReduceMotion = useReducedMotion();

  return (
    <CredibilityShell label="A / Signal ingestion" headline="What Sentinel sees before it thinks">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {signals.map((signal, index) => (
          <motion.div
            key={signal.label}
            initial={shouldReduceMotion ? false : { opacity: 0, y: 12 }}
            whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: shouldReduceMotion ? 0 : index * 0.08, ease: "easeOut" }}
            viewport={{ once: true, amount: 0.2 }}
            className="rounded-sm border border-white/[0.06] bg-[#0f0f0f] p-4"
          >
            <p className="mb-2 font-mono text-[9px] tracking-[0.25em] text-[#444444] uppercase">{signal.label}</p>
            <p className={`font-mono text-sm ${signal.color}`}>{signal.value}</p>
          </motion.div>
        ))}
      </div>
      <p className="mt-8 text-sm text-[#444444]">Sentinel does not guess. Every diagnostic is grounded in the signals above.</p>
    </CredibilityShell>
  );
}
