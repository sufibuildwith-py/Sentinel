# Sentinel Evaluation Lab

Phase 9 is a proof harness, not a production benchmark. It evaluates Sentinel with a deterministic, labelled, Test Mode/synthetic dataset and makes every numerator, denominator, expected decision, actual decision, and failure response inspectable.

## Reproducibility contract

- Seed: `20260901` (`sentinel.evaluation.seed`)
- Report version: `phase9-v1`
- Dataset: 29 categories × 16 scenarios = 464 cases
- Schema: [`schema/sentinel-evaluation-scenario.schema.json`](schema/sentinel-evaluation-scenario.schema.json)
- Stable timestamp: derived from the report version and seed, not wall-clock time
- Stable output: canonical property ordering plus SHA-256 persisted with each evaluation run

Running the same version and seed must produce byte-identical JSON. A drift gate fails when it does not.

## Coverage

The generator includes normal and noisy controls; UPI degradation and provider outage; insufficient funds, abandonment, already-paid, high-value, and duplicate-risk cases; HUMAN approvals and stop rules; duplicate, out-of-order, partial, and cancelled webhooks; wrong amount/currency/link and invalid signatures; LLM timeout, outage, malformed/schema-invalid output; Razorpay 400/401/429/timeout/5xx and ambiguous-create recovery; and prompt-injection/PII boundaries.

Each category has the same sample count. This deliberately prevents a large happy-path category from hiding a safety regression. It does not estimate real-world prevalence.

## Generate evidence

```powershell
mvn -Dtest=EvaluationHarnessIntegrationTest test
```

The test starts real PostgreSQL through Testcontainers, executes all 464 cases, asserts every safety gate, verifies repeatability, persists the run, and writes:

- `target/evaluation-reports/sentinel-evaluation-report.json`
- `target/evaluation-reports/sentinel-evaluation-report.md`

The JSON and Markdown downloads are also exposed at `/api/v1/evaluation/report.json` and `/api/v1/evaluation/report.md`. The dashboard renders the same API report at `/evaluation`.

## Metric definitions

- Detection precision = `TP / (TP + FP)`
- Detection recall = `TP / (TP + FN)`
- Root-cause category accuracy = matching root-cause labels / labelled detected incidents
- Policy compliance = matching deterministic policy decisions / labelled incidents
- False intervention rate = unsafe financial mutations / all scenarios
- Escalation rate = HUMAN decisions / labelled incidents
- Verified recovery rate = verified signed paid outcomes / attempted recoveries

Logical latency percentiles are deterministic regression evidence, not a production performance claim. Recovered revenue is explicitly Razorpay Test Mode / Synthetic Evaluation and is derived only from verified simulated outcomes.

