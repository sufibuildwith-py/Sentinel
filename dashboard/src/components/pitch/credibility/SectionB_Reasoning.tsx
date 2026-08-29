"use client";

import { motion, useReducedMotion } from "motion/react";
import { CredibilityShell } from "@/components/pitch/credibility/CredibilityShell";

const roles = [
  {
    label: "Triage",
    body: "Classifies the incident. Determines severity, customer risk tier, and recovery eligibility. Delegates to specialist agents.",
  },
  {
    label: "Evidence",
    body: "Retrieves historical incident memory via RAG. Cosine similarity search over the postmortem corpus grounds the diagnosis in past outcomes.",
  },
  {
    label: "Recovery",
    body: "Proposes a recovery strategy and confidence score. Does not execute. The proposal is a recommendation, not an action.",
  },
] as const;

export function SectionBReasoning() {
  const shouldReduceMotion = useReducedMotion();

  return (
    <CredibilityShell label="B / Agent reasoning" headline="Three reasoning roles. One governed decision.">
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {roles.map((role, index) => (
          <motion.article
            key={role.label}
            initial={shouldReduceMotion ? false : { opacity: 0, y: 12 }}
            whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
            transition={{ duration: 0.45, delay: shouldReduceMotion ? 0 : index * 0.12, ease: "easeOut" }}
            viewport={{ once: true, amount: 0.2 }}
            className="rounded-sm border border-white/[0.06] bg-[#0f0f0f] p-6"
          >
            <h3 className="mb-4 border-l-2 border-[#2563eb] pl-3 font-mono text-xs tracking-widest text-[#f5f5f5] uppercase">{role.label}</h3>
            <p className="text-sm leading-relaxed text-[#888888]">{role.body}</p>
          </motion.article>
        ))}
      </div>
      <p className="mt-8 border-l border-white/[0.06] pl-4 font-mono text-xs leading-relaxed text-[#444444]">
        ↓ All three agents converge at the Policy Engine.<br />The Policy Engine does not reason. It enforces rules.
      </p>
    </CredibilityShell>
  );
}
