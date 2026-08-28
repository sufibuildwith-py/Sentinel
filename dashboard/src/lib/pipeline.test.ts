import { describe, expect, it } from "vitest";
import { nextActionFor, progressFor } from "./pipeline";
import { money, shortId } from "./api";

describe("incident command-center state", () => {
  it("maps every workflow state to a bounded pipeline step", () => {
    expect(progressFor("DETECTED")).toBe(0);
    expect(progressFor("DIAGNOSED")).toBe(1);
    expect(progressFor("HUMAN_REVIEW")).toBe(3);
    expect(progressFor("MONITORING")).toBe(5);
    expect(progressFor("RECOVERED")).toBe(5);
  });

  it("only offers valid next actions", () => {
    expect(nextActionFor("DETECTED")).toBe("investigate");
    expect(nextActionFor("DIAGNOSED")).toBe("plan");
    expect(nextActionFor("APPROVED")).toBe("execute");
    expect(nextActionFor("HUMAN_REVIEW")).toBeNull();
    expect(nextActionFor("MONITORING")).toBeNull();
  });
});

describe("safe financial presentation", () => {
  it("formats integer minor units without floating-point storage", () => {
    expect(money(72300)).toMatch(/₹723\.00/);
    expect(shortId("7b47c4ee-5c22-43c4")).toBe("7B47C4EE");
  });
});
