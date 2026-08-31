"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MotionConfig } from "motion/react";
import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from "react";
import { TooltipProvider } from "@/components/ui/tooltip";
import { Toaster } from "@/components/ui/sonner";

export interface StatusEvent { title: string; detail: string; }
const StatusContext = createContext<{ event: StatusEvent | null; emit: (event: StatusEvent) => void }>({ event: null, emit: () => undefined });
export const useStatusIsland = () => useContext(StatusContext);

export function Providers({ children }: { children: ReactNode }) {
  const [client] = useState(() => new QueryClient({ defaultOptions: { queries: { staleTime: 15_000, retry: 1, refetchOnWindowFocus: false } } }));
  const [event, setEvent] = useState<StatusEvent | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const emit = useCallback((next: StatusEvent) => { setEvent(next); if (timer.current) clearTimeout(timer.current); timer.current = setTimeout(() => setEvent(null), 4200); }, []);
  const value = useMemo(() => ({ event, emit }), [event, emit]);

  return <QueryClientProvider client={client}><MotionConfig reducedMotion="user" transition={{ type: "spring", duration: .2, bounce: .12 }}><StatusContext.Provider value={value}><TooltipProvider>{children}<Toaster theme="light" position="bottom-right" /></TooltipProvider></StatusContext.Provider></MotionConfig></QueryClientProvider>;
}
