"use client";

import { AnimatePresence, motion, useInView, useReducedMotion } from "motion/react";
import { useRef, useState } from "react";
import { MonoLabel } from "@/components/pitch/shared/MonoLabel";
import { SceneWrapper } from "@/components/pitch/shared/SceneWrapper";

const executionSteps = ["Policy Engine", "Execution Layer", "Razorpay Test Mode"];

export function Scene5Execution() {
  const shouldReduceMotion = useReducedMotion();
  const moneyRef = useRef<HTMLDivElement>(null);
  const moneyInView = useInView(moneyRef, { once: true, amount: 0.8 });
  const [recovered, setRecovered] = useState(false);
  const showRecovered = Boolean(shouldReduceMotion) || recovered;

  return (
    <SceneWrapper id="scene-5">
      <div className="flex w-full max-w-lg flex-col items-center text-center">
        <MonoLabel className="text-[#444444]">Execution and reconciliation</MonoLabel>

        <div className="mt-12 flex flex-col items-center">
          {executionSteps.map((step, index) => (
            <div key={step} className="flex flex-col items-center">
              <motion.div
                initial={shouldReduceMotion ? false : { opacity: 0, y: 8 }}
                whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
                transition={{ duration: 0.35, delay: index * 0.4, ease: "easeOut" }}
                viewport={{ once: true }}
                className="border border-white/[0.06] bg-[#0f0f0f] px-8 py-3 font-mono text-xs tracking-widest text-[#888888] uppercase"
              >
                {step}
              </motion.div>
              {index < executionSteps.length - 1 && (
                <motion.div
                  aria-hidden="true"
                  initial={shouldReduceMotion ? false : { scaleY: 0 }}
                  whileInView={shouldReduceMotion ? undefined : { scaleY: 1 }}
                  transition={{ duration: 0.35, delay: index * 0.4 + 0.25, ease: "easeOut" }}
                  viewport={{ once: true }}
                  className="h-8 w-px origin-top bg-white/10"
                />
              )}
            </div>
          ))}
        </div>

        <motion.div
          initial={shouldReduceMotion ? false : { opacity: 0, y: 8 }}
          whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
          transition={{ duration: 0.35, delay: 1.3, ease: "easeOut" }}
          viewport={{ once: true }}
          className="mt-8 flex items-center gap-2 font-mono text-sm tracking-widest text-[#22c55e]"
        >
          <span aria-hidden="true">●</span> EXECUTED
        </motion.div>

        <div className="mt-7 space-y-2 font-mono text-xs text-[#888888]">
          <motion.p
            initial={shouldReduceMotion ? false : { opacity: 0, y: 6 }}
            whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: 1.7, ease: "easeOut" }}
            viewport={{ once: true }}
          >
            payment.captured
          </motion.p>
          <motion.p
            initial={shouldReduceMotion ? false : { opacity: 0, y: 6 }}
            whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: 2, ease: "easeOut" }}
            viewport={{ once: true }}
          >
            signature verified <span className="text-[#22c55e]">✓</span>
          </motion.p>
        </div>

        <motion.div
          ref={moneyRef}
          initial={shouldReduceMotion ? false : { opacity: 0, y: 10 }}
          whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 2.35, ease: "easeOut" }}
          viewport={{ once: true }}
          className="mt-14 font-mono text-3xl font-semibold sm:text-4xl"
          aria-live="polite"
        >
          <span className="text-[#f5f5f5]">₹4,299 </span>
          <span className="relative inline-grid">
            <AnimatePresence initial={false} mode="sync">
              <motion.span
                key={showRecovered ? "recovered" : "risk"}
                initial={shouldReduceMotion ? false : { opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={shouldReduceMotion ? undefined : { opacity: 0 }}
                transition={{ duration: 0.35, ease: "easeOut" }}
                className={`col-start-1 row-start-1 ${showRecovered ? "text-[#22c55e]" : "text-[#ef4444]"}`}
              >
                {showRecovered ? "RECOVERED" : "AT RISK"}
              </motion.span>
            </AnimatePresence>
          </span>
        </motion.div>

        {moneyInView && !shouldReduceMotion && !recovered && (
          <motion.span
            aria-hidden="true"
            initial={{ opacity: 0 }}
            animate={{ opacity: 0 }}
            transition={{ duration: 0.01, delay: 3.15, ease: "easeOut" }}
            onAnimationComplete={() => setRecovered(true)}
            className="sr-only"
          />
        )}
      </div>
    </SceneWrapper>
  );
}
