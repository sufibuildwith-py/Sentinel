"use client";

import { motion, useReducedMotion } from "motion/react";
import type { ReactNode } from "react";

interface SceneWrapperProps {
  children: ReactNode;
  className?: string;
  id?: string;
}

export function SceneWrapper({ children, className = "", id }: SceneWrapperProps) {
  const shouldReduceMotion = useReducedMotion();

  return (
    <motion.section
      id={id}
      initial={shouldReduceMotion ? false : { opacity: 0, y: 24 }}
      whileInView={shouldReduceMotion ? undefined : { opacity: 1, y: 0 }}
      transition={{ duration: 0.6, ease: "easeOut" }}
      viewport={{ once: true, amount: 0.25 }}
      className={`flex min-h-screen flex-col items-center justify-center px-6 py-24 ${className}`}
    >
      {children}
    </motion.section>
  );
}
