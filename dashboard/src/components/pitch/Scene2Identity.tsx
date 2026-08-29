"use client";

import { motion, useReducedMotion } from "motion/react";
import { SceneWrapper } from "@/components/pitch/shared/SceneWrapper";

const philosophy = ["AI proposes.", "Evidence supports.", "Policy decides.", "Tools execute.", "Outcomes teach."];

export function Scene2Identity() {
  const shouldReduceMotion = useReducedMotion();

  return (
    <SceneWrapper id="scene-2" className="relative overflow-hidden">
      {!shouldReduceMotion && (
        <motion.div
          aria-hidden="true"
          initial={{ scale: 1, opacity: 1 }}
          whileInView={{ scale: 800, opacity: 0 }}
          transition={{ duration: 1.2, ease: "easeOut" }}
          viewport={{ once: true, amount: 0.7 }}
          className="absolute inset-0 m-auto size-px rounded-full bg-blue-600/40"
        />
      )}

      <div className="relative z-10 text-center">
        <motion.div
          initial={shouldReduceMotion ? false : { opacity: 0, y: 10 }}
          whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.4, ease: "easeOut" }}
          viewport={{ once: true }}
        >
          <h1 className="font-mono text-4xl font-bold tracking-[0.4em] text-white uppercase sm:text-5xl">Sentinel</h1>
          <p className="mt-4 text-sm tracking-wider text-[#888888]">Governed AI Revenue Recovery</p>
        </motion.div>

        <div className="mt-14 space-y-4">
          {philosophy.map((line, index) => (
            <motion.p
              key={line}
              initial={shouldReduceMotion ? false : { opacity: 0, y: 8, color: "#444444" }}
              whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0, color: "#888888" }}
              transition={{ duration: 0.4, delay: 1 + index * 0.2, ease: "easeOut" }}
              viewport={{ once: true }}
              className="font-mono text-xs tracking-widest uppercase"
            >
              {line}
            </motion.p>
          ))}
        </div>
      </div>
    </SceneWrapper>
  );
}
