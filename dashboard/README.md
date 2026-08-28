# Sentinel Revenue Intelligence Dashboard

The dashboard is the Test Mode operational surface for Sentinel's complete revenue-recovery flow: detect, investigate, plan, policy, approval, execution, signed webhook outcome, reconciled metrics, and immutable audit.

## Setup

```powershell
npm install
Copy-Item .env.example .env.local
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). The Sentinel backend must be available at the URL configured by `NEXT_PUBLIC_SENTINEL_API_URL` (default `http://localhost:8080`). Do not place Razorpay keys or webhook secrets in any `NEXT_PUBLIC_*` variable.

For visual review without the backend, set `NEXT_PUBLIC_USE_FIXTURES=true`. Keep it `false` for the real five-minute demonstration.

## Verify

```powershell
npm run verify
npx playwright install chromium
npm run test:e2e
```

`verify` runs ESLint, strict TypeScript checking, focused workflow/evaluation tests, and the production build. Playwright separately exercises the Evaluation Lab at desktop and mobile widths, checks the keyboard/dialog path, runs axe WCAG A/AA analysis, and captures full-page visual evidence in `test-results/`.

## Evaluation Lab

Open [http://localhost:3000/evaluation](http://localhost:3000/evaluation) for the Phase 9 proof surface. It reads `GET /api/v1/evaluation/report`, can regenerate the fixed-seed report with `POST /api/v1/evaluation/run`, and exposes canonical JSON/Markdown downloads. With fixtures enabled, it displays the same 464-scenario report shape without contacting the backend.

The scorecard deliberately exposes the numerator/denominator, confusion matrix, sample counts, deterministic failure evidence, zero-tolerance safety gates and limitations. Logical latency is not presented as a production benchmark.

## Safety boundary

The browser receives operational summaries only. Razorpay credentials, webhook bodies, raw payment data, and customer identifiers remain server-side. Every financial surface is explicitly labelled **TEST MODE / SYNTHETIC EVALUATION**.
