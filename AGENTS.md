# Sentinel contributor guide

## Repository map

- `src/main/java/com/sentinel/core`: reusable agent, LLM, policy, audit, and observability boundaries.
- `src/main/java/com/sentinel/revenue`: the Revenue Intelligence domain and Razorpay Test Mode integration.
- `src/main/java/com/sentinel/evaluation`: deterministic Phase 9 dataset, harness, report, and readiness health check.
- `src/main/resources/db/migration`: append-only Flyway migrations. Never edit a migration that may have run; add the next version.
- `src/test`: unit, contract, and real-PostgreSQL Testcontainers tests.
- `dashboard`: independent Next.js/TypeScript operations dashboard. Follow `dashboard/AGENTS.md` too.
- `evaluation`: committed evaluation schema and reproducibility notes.

## Required checks

- Backend: `mvn clean verify` with Docker available so Testcontainers runs; do not substitute H2.
- Dashboard: from `dashboard`, run `npm ci` then `npm run verify`.
- Browser journey: install Chromium once with `npx playwright install chromium`, then run `npm run test:e2e`.

## Safety invariants

- Money is integer minor units (`bigint`/`long`), never floating point.
- Razorpay credentials, webhook raw bodies, customer PII, and payment details never reach the browser, logs, prompts, or evaluation artifacts.
- Model output can propose or explain; only deterministic policy and persisted human approval can grant execution permission.
- Webhook state and revenue metrics are monotonic and idempotent. Duplicate delivery must produce no duplicate financial effect.
- Phase 9 defaults to deterministic fixtures and requires no live credentials. Keep optional Test Mode smoke checks explicitly credential-gated.
- Do not commit `target/`, `node_modules/`, `.next/`, reports containing sensitive data, or local environment files.

