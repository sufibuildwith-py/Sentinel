import type { ReactNode } from "react";

interface MonoLabelProps {
  children: ReactNode;
  className?: string;
}

export function MonoLabel({ children, className = "" }: MonoLabelProps) {
  return <span className={`font-mono text-xs tracking-widest uppercase ${className}`}>{children}</span>;
}
