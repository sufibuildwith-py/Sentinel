export type TruthKind = "PROVIDER" | "PENDING" | "SIMULATION" | "HISTORICAL" | "SHADOW" | "OBSERVATIONAL" | "CONTROLLED" | "NEUTRAL";

export function truthKind(label: string): TruthKind {
  const value = label.toUpperCase().replaceAll("_", " ");
  if (value.includes("PROVIDER CONFIRMED") || value.includes("RAZORPAY TEST MODE")) return "PROVIDER";
  if (value.includes("AWAITING") || value.includes("UNRECONCILED")) return "PENDING";
  if (value.includes("SIMULATION") || value.includes("FAULT") || value.includes("SYNTHETIC")) return "SIMULATION";
  if (value.includes("HISTORICAL")) return "HISTORICAL";
  if (value.includes("SHADOW")) return "SHADOW";
  if (value.includes("OBSERVATIONAL")) return "OBSERVATIONAL";
  if (value.includes("CONTROLLED") || value.includes("HOLDOUT")) return "CONTROLLED";
  return "NEUTRAL";
}

export function safeExternalUrl(value: string): string | null {
  try {
    const parsed = new URL(value);
    return parsed.protocol === "https:" || parsed.protocol === "http:" ? parsed.toString() : null;
  } catch {
    return null;
  }
}

export function durationLabel(milliseconds?: number | null): string {
  if (milliseconds == null) return "Not measured";
  if (milliseconds < 60_000) return `${(milliseconds / 1000).toFixed(1)}s`;
  if (milliseconds < 3_600_000) return `${(milliseconds / 60_000).toFixed(1)}m`;
  if (milliseconds < 86_400_000) return `${(milliseconds / 3_600_000).toFixed(1)}h`;
  return `${(milliseconds / 86_400_000).toFixed(1)}d`;
}

export function paginate<T>(items: T[], page: number, pageSize: number): T[] {
  const safePage = Math.max(1, page);
  return items.slice((safePage - 1) * pageSize, safePage * pageSize);
}
