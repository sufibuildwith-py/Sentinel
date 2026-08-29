"use client";

import Link from "next/link";
import { motion, useReducedMotion } from "motion/react";
import { SceneWrapper } from "@/components/pitch/shared/SceneWrapper";

export function Scene6Closing() {
  const shouldReduceMotion = useReducedMotion();

  const entrance = (delay: number) => ({
    initial: shouldReduceMotion ? false as const : { opacity: 0, y: 10 },
    whileInView: shouldReduceMotion ? undefined : { opacity: 1, y: 0 },
    transition: { duration: 0.45, delay: shouldReduceMotion ? 0 : delay, ease: "easeOut" as const },
    viewport: { once: true },
  });

  return (
    <SceneWrapper id="scene-6">
      <div className="text-center">
        <motion.p {...entrance(0)} className="text-2xl font-light text-[#888888]">Don&apos;t just detect lost revenue.</motion.p>
        <motion.h2 {...entrance(0.8)} className="mt-5 font-mono text-4xl font-bold tracking-tight text-white sm:text-5xl">Recover it.</motion.h2>
        <motion.p {...entrance(1.4)} className="mt-10 font-mono text-xs tracking-[0.4em] text-[#444444] uppercase">Sentinel</motion.p>
        <motion.div {...entrance(1.8)} className="mt-10 flex flex-col items-stretch justify-center gap-3 sm:flex-row">
          <Link href="/console" className="bg-[#2563eb] px-8 py-3 font-mono text-sm tracking-widest text-white transition-colors duration-200 ease-out hover:bg-[#1d4ed8] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#2563eb]">
            Launch Sentinel →
          </Link>
          <a href="#scene-3" className="border border-white/20 bg-transparent px-8 py-3 font-mono text-sm tracking-widest text-[#888888] transition-[border-color,color] duration-200 ease-out hover:border-white/40 hover:text-[#f5f5f5] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white/60">
            Explore Architecture
          </a>
        </motion.div>
      </div>
    </SceneWrapper>
  );
}
