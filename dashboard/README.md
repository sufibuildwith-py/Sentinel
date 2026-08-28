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
```

This runs ESLint, strict TypeScript checking, focused workflow tests, and the production build.

## Safety boundary

The browser receives operational summaries only. Razorpay credentials, webhook bodies, raw payment data, and customer identifiers remain server-side. Every financial surface is explicitly labelled **TEST MODE / SYNTHETIC EVALUATION**.
