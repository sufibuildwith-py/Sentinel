"use client";

import { motion, useReducedMotion } from "motion/react";

interface StatusRowProps {
  label: string;
  status: string;
  delay?: number;
  tone?: "success" | "danger";
}

export function StatusRow({ label, status, delay = 0, tone = "success" }: StatusRowProps) {
  const shouldReduceMotion = useReducedMotion();
  const statusColor = tone === "success" ? "text-[#22c55e]" : "text-[#ef4444]";

  return (
    <motion.div
      initial={shouldReduceMotion ? false : { opacity: 0, y: 8 }}
      whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
      transition={{ duration: 0.3, delay: shouldReduceMotion ? 0 : delay, ease: "easeOut" }}
      viewport={{ once: true, amount: 0.8 }}
      className="flex items-center justify-between gap-6 border-b border-white/[0.06] py-3"
    >
      <span className="font-mono text-xs tracking-widest text-[#444444] uppercase">{label}</span>
      <span className={`flex items-center gap-2 font-mono text-xs tracking-widest uppercase ${statusColor}`}>
        <span aria-hidden="true">●</span>
        {status}
      </span>
    </motion.div>
  );
}
