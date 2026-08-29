"use client";

import { motion, useReducedMotion } from "motion/react";
import { CredibilityShell } from "@/components/pitch/credibility/CredibilityShell";

const contract = "AI proposes. Policy decides. Tools execute. Outcomes teach.";
const principles = [
  {
    label: "Separation of Authority",
    body: "The AI investigation layer and the execution layer are architecturally separate. The LLM produces a proposal. A deterministic engine approves or denies it. These are never the same component.",
  },
  {
    label: "Auditable Decisions",
    body: "Every policy decision is persisted with a reason, a verdict, and a timestamp. No action happens without a traceable policy record. The audit trail is not optional — it is structural.",
  },
  {
    label: "Deterministic Proof",
    body: "The evaluation harness runs the real engines, not mocks. Results are reproducible from a fixed seed. The system proves its own governance on every CI run.",
  },
] as const;

export function SectionDGovernance() {
  const shouldReduceMotion = useReducedMotion();

  return (
    <CredibilityShell label="D / Governance" headline="The rule that does not bend.">
      <p className="mb-16 text-center font-mono text-lg tracking-wide text-[#f5f5f5]" aria-label={contract}>
        {shouldReduceMotion ? contract : contract.split(" ").map((word, index) => (
          <motion.span
            key={`${word}-${index}`}
            aria-hidden="true"
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            transition={{ duration: 0.35, delay: index * 0.08, ease: "easeOut" }}
            viewport={{ once: true, amount: 0.2 }}
            className="inline-block"
          >
            {word}{index < contract.split(" ").length - 1 ? "\u00a0" : ""}
          </motion.span>
        ))}
      </p>
      <div className="grid grid-cols-1 gap-8 md:grid-cols-3">
        {principles.map((principle) => (
          <div key={principle.label}>
            <h3 className="mb-3 font-mono text-xs tracking-widest text-[#f5f5f5] uppercase">{principle.label}</h3>
            <p className="text-sm leading-relaxed text-[#888888]">{principle.body}</p>
          </div>
        ))}
      </div>
      <p className="mx-auto mt-16 max-w-lg text-center text-sm text-[#444444]">
        Sentinel is not an AI that acts. It is an AI that recommends, governed by a system that decides.
      </p>
    </CredibilityShell>
  );
}
