# Contributing to Sentinel

Sentinel is feature-frozen for the Buildathon release. Open an issue before changing domain behavior, safety invariants, persistence schemas, evaluation oracles, or public APIs.

## Development workflow

1. Create a focused branch and keep secrets in untracked environment files.
2. Add a new Flyway migration instead of editing any migration that may have run.
3. Preserve integer minor units for money, deterministic policy authority, append-only audit, and idempotent financial effects.
4. Run `bash scripts/verify-no-secrets.sh`.
5. Run `mvn clean verify` with Docker available; integration tests use real PostgreSQL through Testcontainers.
6. Run `npm ci` and `npm run verify` in `dashboard/`.
7. For dashboard journeys, run `npx playwright install chromium` once and then `npm run test:e2e`.
8. Explain the scope, tests, safety impact, and documentation changes in the pull request.

Do not commit `target/`, `.m2/`, `node_modules/`, `.next/`, browser reports, `.env` files, credentials, PII, raw payment details, or webhook payloads. See [ARCHITECTURE.md](ARCHITECTURE.md) for system boundaries and [AGENTS.md](AGENTS.md) for repository-specific invariants.
