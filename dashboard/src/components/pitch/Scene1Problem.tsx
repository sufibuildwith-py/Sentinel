"use client";

import { animate, motion, useInView, useMotionValue, useReducedMotion, useTransform } from "motion/react";
import { useEffect, useRef } from "react";
import { MonoLabel } from "@/components/pitch/shared/MonoLabel";
import { SceneWrapper } from "@/components/pitch/shared/SceneWrapper";

const payments = ["₹2,499", "₹899", "₹12,400", "₹4,299", "₹1,799"];
const totalAtRisk = 21_796;

const amountList = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.12 } },
};

const amountItem = {
  hidden: { opacity: 0, y: 12 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.45, ease: "easeOut" as const } },
};

export function Scene1Problem() {
  const shouldReduceMotion = useReducedMotion();
  const counterRef = useRef<HTMLDivElement>(null);
  const counterInView = useInView(counterRef, { once: true, amount: 0.7 });
  const counter = useMotionValue(0);
  const formattedCounter = useTransform(counter, (value) => `₹${Math.round(value).toLocaleString("en-IN")}`);

  useEffect(() => {
    if (!counterInView) return;
    if (shouldReduceMotion) {
      counter.set(totalAtRisk);
      return;
    }
    const controls = animate(counter, totalAtRisk, { duration: 1.8, delay: 1.45, ease: "easeOut" });
    return () => controls.stop();
  }, [counter, counterInView, shouldReduceMotion]);

  return (
    <SceneWrapper id="scene-1" className="relative">
      <div className="w-full max-w-lg text-center">
        <MonoLabel className="text-[#444444]">Failure sequence</MonoLabel>
        <motion.ul
          variants={amountList}
          initial={shouldReduceMotion ? false : "hidden"}
          whileInView={shouldReduceMotion ? undefined : "visible"}
          viewport={{ once: true, amount: 0.7 }}
          className="mx-auto mt-10 grid max-w-sm gap-3"
          aria-label="Failed payment values"
        >
          {payments.map((amount, index) => (
            <motion.li key={amount} variants={amountItem} className="grid grid-cols-[1fr_auto] items-center border-b border-white/[0.06] px-4 py-3 text-left">
              <span className="font-mono text-2xl text-[#f5f5f5]">{amount}</span>
              <motion.span
                initial={shouldReduceMotion ? false : { opacity: 0, x: -6 }}
                whileInView={shouldReduceMotion ? undefined : { opacity: 1, x: 0 }}
                transition={{ duration: 0.3, delay: 0.8 + index * 0.08, ease: "easeOut" }}
                viewport={{ once: true }}
                className="font-mono text-xs tracking-widest text-[#ef4444]"
              >
                FAILED
              </motion.span>
            </motion.li>
          ))}
        </motion.ul>

        <div ref={counterRef} className="mt-14">
          <MonoLabel className="text-[#444444]">Revenue at risk</MonoLabel>
          <motion.div className="mt-4 font-mono text-4xl font-semibold text-[#ef4444]" aria-label="Twenty-one thousand seven hundred ninety-six rupees at risk">
            {formattedCounter}
          </motion.div>
        </div>

        <div className="mt-14 space-y-3">
          <motion.p
            initial={shouldReduceMotion ? false : { opacity: 0, y: 8 }}
            whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 3.35, ease: "easeOut" }}
            viewport={{ once: true }}
            className="text-sm text-[#888888]"
          >
            Payments fail in milliseconds.
          </motion.p>
          <motion.p
            initial={shouldReduceMotion ? false : { opacity: 0, y: 8 }}
            whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 3.95, ease: "easeOut" }}
            viewport={{ once: true }}
            className="text-sm text-[#f5f5f5]"
          >
            Revenue disappears much more quietly.
          </motion.p>
        </div>
      </div>
    </SceneWrapper>
  );
}
