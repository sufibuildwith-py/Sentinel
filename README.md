# Sentinel

**A domain-extensible agentic incident-intelligence platform, built in Java + Spring Boot.**

Sentinel detects operational problems, investigates their causes with evidence and historical memory, proposes a bounded intervention, passes it through deterministic safety policies, executes approved actions through external tools, observes the outcome, and learns from what happened.

> **AI proposes. Evidence supports. Policy decides. Tools execute. Outcomes teach.**

Its first complete domain is **Sentinel Revenue Intelligence** — an autonomous-but-governed payment-recovery system built for the **Razorpay AI Revenue Recovery** track, integrating Razorpay Test Mode end to end: detection → investigation → recovery → policy gate → execution → webhook outcome → measured recovered revenue.

Sentinel started as a single-agent RAG incident copilot ([original prototype](https://github.com/sufibuildwith-py/Sentinel)) and is being rebuilt into a proper multi-agent, policy-governed platform without throwing that foundation away — the original `/investigate` endpoint still works.

---

## Why this exists

Most "AI revenue recovery" demos are a chatbot that reads a failed-payment description and suggests something plausible. That's not what Razorpay is asking for, and it's not what makes a durable system.

Sentinel treats revenue loss the way an SRE treats an incident: something abnormal happened, it needs evidence before a diagnosis, the diagnosis needs a bounded response, and the response needs a stopping condition. Nothing here executes a financial action because a language model felt confident about it — every action passes through a deterministic policy engine first, and every step is written to an audit trail.

## Status: Phase 4 of 10 complete

| Phase | Focus | Status | Outcome |
|---|---|---|---|
| 1 | Sentinel Core & project hygiene | ✅ Complete | Extensible core; original `/investigate` flow untouched |
| 2 | Revenue domain & persistence | ✅ Complete | Normalized payment/incident data in PostgreSQL, Flyway-migrated |
| 3 | Detection & incident clustering | ✅ Complete | Explainable revenue incidents, zero LLM calls |
| 4 | Agentic investigation & memory | ✅ Complete | Evidence-backed root cause, validated structured output, historical memory |
| 5 | Recovery planning, policy & audit | ⏳ Next | Governed AUTO / HUMAN / DENY decisions |
| 6 | Razorpay Test Mode execution | Planned | Real Payment Link recovery actions |
| 7 | Outcome loop & revenue measurement | Planned | Webhook-driven outcomes, recovered-revenue metrics |
| 8 | Operational dashboard | Planned | Full workflow visible without Postman |
| 9 | Evaluation & reliability | Planned | Measured quality, safe failure behaviour |
| 10 | Integration, release & submission | Planned | Reproducible, demo-ready release |

## The loop, as built so far

```text
Payment events (batch ingestion, idempotent)
        │
        ▼
Deterministic detection — success-rate drop, baseline deviation,
failure-code concentration — no LLM in this step
        │
        ▼
Explainable clustering — related failures grouped by method,
issuer, error code, time window → a quantified RevenueIncident
        │
        ▼
Agentic investigation
  Triage Agent → Payment Analyst (+ Pattern Analyzer,
  Customer Context tools) → historical memory retrieval
  → Root Cause Agent → structured, validated diagnosis
        │
        ▼
DIAGNOSED incident: root cause, confidence, evidence,
alternative hypotheses — nothing here has spent a rupee
```

Phases 5–7 close the loop: a recovery proposal, a policy gate that returns AUTO / HUMAN / DENY, execution through Razorpay Test Mode, and a webhook-driven outcome that updates a recovered-revenue metric.

## Example: what an incident looks like today

**Detection + clustering (Phase 3)** — fed a batch with a UPI-issuer degradation:

```json
{
  "incidentId": "RR-1042",
  "type": "UPI_DEGRADATION",
  "severity": "CRITICAL",
  "amountAtRiskMinor": 8460000,
  "affectedPayments": 38,
  "affectedCustomers": 31,
  "findings": [
    {
      "source": "DETECTOR",
      "rule": "SUCCESS_RATE_DROP",
      "baseline": "96.3%",
      "observed": "54.8%",
      "delta": "-41.5pp",
      "result": "PASS"
    }
  ]
}
```

**Agentic investigation (Phase 4)** — the same incident, once the Root Cause Agent runs:

```json
{
  "rootCause": "UPI issuer degradation at Bank X",
  "confidence": 0.91,
  "evidence": [
    "73% of failures are UPI",
    "61% involve Bank X",
    "failure onset 09:42, no corresponding rise in card failures"
  ],
  "alternativeHypotheses": [],
  "similarHistoricalIncidents": 14,
  "historicalStrategySuccessRate": {
    "strategy": "ALTERNATIVE_PAYMENT_LINK",
    "attempts": 47,
    "recovered": 34,
    "rate": "72.3%"
  }
}
```

That last block is the part worth noticing: the recommendation isn't "the model believes this is a good idea," it's "this action worked on 72% of similar past incidents." Evidence over vibes.

## Architecture

```text
                            SENTINEL CORE
                ┌─────────────────────────────────┐
                │ Agents · Orchestration · Memory  │
                │ Policy · Tools · Audit · Evals   │
                └───────────────┬───────────────────┘
                                │
                     Revenue Recovery domain
                ┌───────────────┴─────────────────┐
                │ Events · Detection · Incidents   │
                │ Analysis · Planning · Recovery   │
                └───────────────┬─────────────────┘
                                │
                  Deterministic policy engine
                     ┌──────────┼──────────┐
                     ↓          ↓          ↓
                   AUTO       HUMAN       DENY
                     └──────────┬──────────┘
                                ↓
                     Razorpay Test Mode tools
                                ↓
                   Webhooks · Outcomes · Metrics
```

The core knows what an incident is. The Revenue Recovery domain knows what payments, customers, failures, and Razorpay APIs are. That separation is deliberate — it's what lets Sentinel grow additional domain packs (engineering ops, infra, security) after this one, instead of being a single-purpose Razorpay app.

## Tech stack

- **Java 17 + Spring Boot 3** — REST API, layered architecture, dependency injection
- **PostgreSQL + Spring Data JPA + Flyway** — system of record, versioned schema
- **Google Gemini** — generation (structured, schema-validated output only) and embeddings
- **Apache Commons Math** — rolling-baseline statistics for detection
- **Easy Rules** — explainable, traceable detection rule evaluation
- **Resilience4j** — circuit-breaking around the LLM and (soon) Razorpay calls, so external outages degrade gracefully instead of cascading
- **Testcontainers** — every persistence/integration test runs against real PostgreSQL, never H2
- **DataFaker** — deterministic, seeded synthetic payment datasets for evaluation

## Project structure

```text
Sentinel/
├── src/main/java/com/sentinel/
│   ├── core/                 # agent abstraction, orchestration, memory,
│   │                         # policy, audit, LLM/embedding client interfaces
│   └── revenue/
│       ├── model/            # PaymentEvent, RevenueIncident, IncidentFinding,
│       │                     # RecoveryPlan/Action/Outcome, AuditEvent, ...
│       ├── detection/        # StatisticsEngine, DetectionRuleEngine
│       ├── service/          # FailureClusteringService, state machine,
│       │                     # ingestion, historical memory
│       ├── agent/            # Triage, Payment Analyst, Root Cause agents
│       ├── api/              # REST controllers
│       └── dataset/          # synthetic evaluation dataset generator
├── src/main/resources/db/migration/   # Flyway migrations
├── runbooks/                 # original + revenue-domain runbook corpus
└── docs/                     # architecture, API, evaluation notes
```

## Running it locally

**1. Start PostgreSQL** (Docker):
```bash
docker run --name sentinel-pg -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
```

**2. Set environment variables**
```bash
export GEMINI_API_KEY="your_key_here"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/postgres"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="postgres"
```

**3. Run migrations and start the app**
```bash
mvn flyway:migrate
mvn spring-boot:run
```

**4. Load the synthetic dataset and watch detection fire**
```bash
curl -X POST http://localhost:8080/api/v1/revenue/events/batch \
  -H "Content-Type: application/json" \
  -d @datasets/payment-batch-labelled.json
```

**5. See a live-injected demo incident**
```bash
curl -X POST http://localhost:8080/api/v1/demo/inject/upi-outage
```

## Roadmap

- [x] Phase 1 — Sentinel Core extracted, original investigation flow preserved
- [x] Phase 2 — Revenue domain modeled and persisted in PostgreSQL
- [x] Phase 3 — Deterministic, explainable detection and clustering
- [x] Phase 4 — Agentic investigation grounded in evidence and historical memory
- [ ] Phase 5 — Recovery planning + deterministic policy engine (AUTO/HUMAN/DENY)
- [ ] Phase 6 — Razorpay Test Mode execution (Payment Links)
- [ ] Phase 7 — Webhook outcome loop, recovered-revenue metric
- [ ] Phase 8 — Next.js operational dashboard
- [ ] Phase 9 — Evaluation harness, resilience testing
- [ ] Phase 10 — Submission hardening and demo release

## What Sentinel deliberately is not

No fifteen-agent swarm, no Kubernetes, no custom ML anomaly model before the rule-based one earns it, no real-money automation, no giant RAG corpus. Deep rather than bloated — every component here exists because a specific requirement needed it.

## License

MIT
