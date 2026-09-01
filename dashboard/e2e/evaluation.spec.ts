import { expect, test } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test.beforeEach(async ({ page }) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
});

test("renders inspectable evaluation proof without serious accessibility violations", async ({ page }, testInfo) => {
  await page.goto("/evaluation");
  await expect(page.getByRole("heading", { name: "Sentinel Evaluation Lab" })).toBeVisible();
  await expect(page.getByText("All gates pass")).toBeVisible();
  await expect(page.getByText("Policy compliance", { exact: true }).first()).toBeVisible();

  await page.getByRole("button", { name: "Definitions" }).click();
  await expect(page.getByRole("dialog", { name: "Metric definitions and evidence" })).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog", { name: "Metric definitions and evidence" })).toBeHidden();

  await page.getByPlaceholder("Filter category, policy, outcome").fill("invalid signature");
  await page.getByText("INVALID SIGNATURE", { exact: true }).click();
  await expect(page.getByText("Selected evidence")).toBeVisible();
  await expect(page.getByText("INVALID SIGNATURE REJECTED", { exact: true }).last()).toBeVisible();

  const accessibility = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21aa"]).analyze();
  const serious = accessibility.violations.filter((item) => item.impact === "serious" || item.impact === "critical");
  expect(serious).toEqual([]);

  const snapshotPath = testInfo.outputPath(`evaluation-${testInfo.project.name}.png`);
  await page.screenshot({ path: snapshotPath, fullPage: true, animations: "disabled" });
  await testInfo.attach("evaluation-lab", { path: snapshotPath, contentType: "image/png" });
});

test("failure laboratory remains explicit and bounded", async ({ page }) => {
  await page.goto("/evaluation");
  await page.getByRole("tab", { name: "Failure laboratory" }).click();
  await expect(page.getByText("LLM timeout/outage/invalid output")).toBeVisible();
  await expect(page.getByText("Razorpay 400/401/429/5xx/timeout")).toBeVisible();
  await expect(page.getByText("Duplicate and out-of-order webhook")).toBeVisible();
  await expect(page.getByText("PostgreSQL contention / concurrent duplicate execution")).toBeVisible();
  await expect(page.getByText("Already-paid conflicting state")).toBeVisible();
  await expect(page.getByText("BOUNDED", { exact: true })).toHaveCount(7);
});

test("V2 proof routes preserve truth labels and do not invent fixture results", async ({ page }) => {
  await page.goto("/evaluation/recovery-olympics");
  await expect(page.getByRole("heading", { name: "10,000-case controlled economics benchmark" })).toBeVisible();
  await expect(page.getByText("SYNTHETIC / CONTROLLED BENCHMARK", { exact: true })).toBeVisible();
  await expect(page.getByText("Benchmark unavailable in fixture mode")).toBeVisible();

  await page.goto("/evaluation/historical");
  await expect(page.getByRole("heading", { name: "Razorpay public-source case corpus" })).toBeVisible();
  await expect(page.getByText("PUBLIC-SOURCE HISTORICAL VALIDATION", { exact: true })).toBeVisible();
  await expect(page.getByText("Historical corpus unavailable in fixture mode")).toBeVisible();
  await expect(page.getByText("500", { exact: true })).toHaveCount(0);
});

test("operator workspaces remain honest and responsive in fixture mode", async ({ page }) => {
  for (const [path, heading] of [
    ["/recovery", "Recovery operations board"],
    ["/intelligence", "Recovery economics, before authority"],
    ["/governance", "Authority remains deterministic"],
    ["/audit", "Immutable decision evidence"],
  ] as const) {
    await page.goto(path);
    await expect(page.getByRole("heading", { name: heading })).toBeVisible();
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
    expect(overflow).toBe(false);
  }
  await page.goto("/intelligence");
  await expect(page.getByText("Marketplace unavailable in fixture mode")).toBeVisible();
  await expect(page.getByText("No values were inferred", { exact: false })).toBeVisible();
});

test("operations board and case portfolio remain truth-labelled and selectable", async ({ page }, testInfo) => {
  await page.goto("/console");
  await expect(page.getByRole("heading", { name: "Revenue recovery operations" })).toBeVisible();
  await expect(page.getByText("Provider confirmed", { exact: true }).first()).toBeVisible();
  await expect(page.getByText("Awaiting provider truth", { exact: true }).first()).toBeVisible();
  const consoleShot = testInfo.outputPath(`operations-console-${testInfo.project.name}.png`);
  await page.screenshot({ path: consoleShot, fullPage: true, animations: "disabled" });

  await page.goto("/recovery");
  await expect(page.getByRole("heading", { name: "Recovery operations board" })).toBeVisible();
  await expect(page.getByLabel("Governed recovery graph")).toBeVisible();
  await expect(page.getByText("Provider confirmed recovered", { exact: true })).toBeVisible();
  await expect(page.getByText("Signed payment_link.paid webhook verified and applied exactly once.")).toBeVisible();
  await page.getByRole("button", { name: /PROVIDER OUTAGE/ }).click();
  await expect(page.getByText("Incident 5C35F128", { exact: false })).toBeVisible();
  await expect(page.getByText("Current control: Awaiting human approval", { exact: false })).toBeVisible();
  await expect(page.getByText("Awaiting human review", { exact: true })).toBeVisible();
  await expect(page.getByText("Provider accepted · not recovered yet", { exact: true })).toHaveCount(0);
  await expect(page.getByText("Signed payment_link.paid webhook verified and applied exactly once.")).toHaveCount(0);
  const recoveryShot = testInfo.outputPath(`recovery-board-${testInfo.project.name}.png`);
  await page.screenshot({ path: recoveryShot, fullPage: true, animations: "disabled" });
  await page.getByRole("button", { name: /UPI DEGRADATION/ }).click();
  await expect(page.getByText("Provider confirmed recovered", { exact: true })).toBeVisible();
  await expect(page.getByText("Signed payment_link.paid webhook verified and applied exactly once.")).toBeVisible();
  const recoveredShot = testInfo.outputPath(`recovery-confirmed-${testInfo.project.name}.png`);
  await page.screenshot({ path: recoveredShot, fullPage: true, animations: "disabled" });

  await page.goto("/incidents/7b47c4ee-5c22-43c4-8f0c-831168683bbe");
  await expect(page.getByRole("heading", { name: "UPI DEGRADATION" })).toBeVisible();
  await expect(page.getByText("₹723.00 RECOVERED")).toBeVisible();

  await page.goto("/incidents");
  await expect(page.getByRole("heading", { name: "Recovery case portfolio" })).toBeVisible();
  await page.getByRole("tab", { name: "Historical Razorpay" }).click();
  await expect(page.getByText("HISTORICAL PUBLIC SOURCE", { exact: true }).first()).toBeVisible();
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
  expect(overflow).toBe(false);
  const casesShot = testInfo.outputPath(`case-portfolio-${testInfo.project.name}.png`);
  await page.screenshot({ path: casesShot, fullPage: true, animations: "disabled" });
});
