import { describe, expect, it } from "vitest";
import { durationLabel, paginate, safeExternalUrl, truthKind } from "./truth";

describe("truth presentation", () => {
  it("keeps financial truth classes visually distinct", () => {
    expect(truthKind("PROVIDER CONFIRMED")).toBe("PROVIDER");
    expect(truthKind("AWAITING_RECONCILIATION")).toBe("PENDING");
    expect(truthKind("SYNTHETIC BENCHMARK")).toBe("SIMULATION");
    expect(truthKind("HISTORICAL DERIVED REPLAY")).toBe("HISTORICAL");
    expect(truthKind("SHADOW ONLY")).toBe("SHADOW");
  });

  it("does not clamp or hide anomalous durations", () => {
    expect(durationLabel(6_560_181_581)).toBe("75.9d");
    expect(durationLabel(null)).toBe("Not measured");
  });

  it("rejects unsafe provenance protocols", () => {
    expect(safeExternalUrl("javascript:alert(1)")).toBeNull();
    expect(safeExternalUrl("https://github.com/razorpay/example")).toContain("https://github.com/");
  });

  it("makes the complete historical corpus browseable", () => {
    const cases = Array.from({ length: 500 }, (_, index) => index + 1);
    expect(paginate(cases, 20, 25)).toEqual(Array.from({ length: 25 }, (_, index) => 476 + index));
  });
});
