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
