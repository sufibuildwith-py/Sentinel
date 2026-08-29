"use client";

import type { ReactNode } from "react";
import { motion, useReducedMotion } from "motion/react";

interface CredibilityShellProps {
  children: ReactNode;
  headline: string;
  label: string;
}

export function CredibilityShell({ children, headline, label }: CredibilityShellProps) {
  const shouldReduceMotion = useReducedMotion();

  return (
    <motion.section
      initial={shouldReduceMotion ? false : { opacity: 0, y: 20 }}
      whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: "easeOut" }}
      viewport={{ once: true, amount: 0.2 }}
      className="mx-auto max-w-4xl px-6 py-24"
    >
      <div className="my-0 h-px w-full bg-white/[0.04]" />
      <p className="mt-24 mb-3 font-mono text-[10px] tracking-[0.3em] text-[#444444] uppercase">{label}</p>
      <h2 className="mb-10 font-mono text-xl font-bold tracking-tight text-[#f5f5f5]">{headline}</h2>
      {children}
    </motion.section>
  );
}
