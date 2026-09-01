import { expect, test } from "@playwright/test";

const liveIncidentId = process.env.SENTINEL_LIVE_E2E_INCIDENT_ID;

test.describe("live recovery workbench", () => {
  test.skip(!liveIncidentId, "Requires a seeded live incident; fixture mode must not fabricate recovery transitions.");

  test("renders real stage transitions and append-only ledger events", async ({ page }) => {
    const recoveryPosts: string[] = [];
    page.on("request", (request) => {
      if (request.method() === "POST" && /\/(investigate|plan|execute)$/.test(request.url())) recoveryPosts.push(request.url());
    });
    await page.goto(`/incidents/${liveIncidentId}`);
    const ledgerRows = page.locator('[aria-live="polite"] li');
    const initialLedgerCount = await ledgerRows.count();
    const initialCompleted = await page.getByLabel(/: COMPLETE$/).count();

    const run = page.getByRole("button", { name: "RUN RECOVERY" });
    await expect(run).toBeEnabled();
    await run.click();
    await expect(page.getByText(/ACTIVE RECOVERY|AWAITING HUMAN REVIEW|AWAITING PROVIDER TRUTH|RECOVERY COMPLETE|BLOCKED BY POLICY|HELD BY GOVERNOR/).first()).toBeVisible({ timeout: 30_000 });

    await expect.poll(async () => ledgerRows.count(), { timeout: 30_000 }).toBeGreaterThan(initialLedgerCount);
    await expect.poll(async () => page.getByLabel(/: COMPLETE$/).count(), { timeout: 30_000 }).toBeGreaterThanOrEqual(initialCompleted + 2);
    await expect(page.getByText(/PROVIDER ACCEPTED · NOT RECOVERED YET|AWAITING HUMAN REVIEW|BLOCKED BY POLICY|HELD BY GOVERNOR|RECOVERY COMPLETE/).first()).toBeVisible();

    await page.reload();
    await expect(page.locator('[aria-live="polite"] li').first()).toBeVisible();
    await expect(page.getByRole("button", { name: "RUN RECOVERY" })).toHaveCount(0);
    expect(recoveryPosts.filter((url) => url.endsWith("/investigate"))).toHaveLength(0);
    expect(recoveryPosts.filter((url) => url.endsWith("/plan"))).toHaveLength(0);
    expect(recoveryPosts.filter((url) => url.endsWith("/execute"))).toHaveLength(1);
  });
});
