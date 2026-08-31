import { describe, expect, it } from "vitest";
import { SentinelApiError } from "./api";
import { mutationErrorMessage } from "./api-errors";

describe("semantic mutation errors", () => {
  it("explains state conflicts and duplicate prevention", () => {
    expect(mutationErrorMessage(new SentinelApiError("conflict", 409, "request-1", "STATE_CONFLICT"), "execute"))
      .toContain("No duplicate provider action was sent");
  });

  it("keeps reset state truthful on server failure", () => {
    expect(mutationErrorMessage(new SentinelApiError("internal", 500), "reset"))
      .toContain("Existing state is unchanged");
  });

  it("distinguishes connectivity failure", () => {
    expect(mutationErrorMessage(new TypeError("Failed to fetch"))).toContain("reach the API");
  });
});
