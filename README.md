# Sentinel

**A domain-extensible agentic incident-intelligence platform.**

Sentinel detects operational problems, investigates their causes, proposes bounded interventions, executes approved actions through external tools, observes outcomes, and learns from previous incidents.

Its first complete domain is **Sentinel Revenue Intelligence**, a Razorpay Test Mode revenue-recovery system built around one principle:

> **AI proposes. Evidence supports. Policy decides. Tools execute. Outcomes teach.**

This README is the ten-phase development plan for evolving the existing Java/Spring Boot RAG prototype into a Buildathon-ready operational AI system.

## Product objective

The finished revenue-recovery workflow must demonstrate the complete loop:

```text
Payment events
      ↓
Detect revenue anomaly
      ↓
Cluster failures into an incident
      ↓
Investigate with agents, tools, and historical memory
      ↓
Diagnose root cause with evidence and confidence
      ↓
Propose a recovery strategy
      ↓
Apply deterministic policy gates
      ↓
Auto-execute, request approval, or deny
      ↓
Execute through Razorpay Test Mode
      ↓
Observe webhook outcome
      ↓
Measure recovered revenue and retain the audit trail
```

The project must remain domain-extensible. Sentinel Core owns reasoning, orchestration, memory, policy, tools, audit, and evaluation. The Revenue Recovery domain owns payments, failure detection, recovery strategies, financial guardrails, and Razorpay integration.

## Current baseline

The repository already contains:

- Java 17 and Spring Boot 3.3
- `POST /investigate`
- Gemini text generation
- Gemini embeddings
- Runbook ingestion
- In-memory cosine-similarity retrieval
- A basic retrieval-augmented diagnosis pipeline
- Five sample engineering incident runbooks

After Phase 1, the preserved investigation flow is:

```text
HTTP controller
    → InvestigationService
    → EmbeddingClient
    → RunbookMemory
    → LlmClient structured generation
    → Diagnosis
```

This baseline must continue working while the platform is expanded. The revenue workflow will be added around an extracted Sentinel Core rather than replacing the existing project with a Razorpay-only application.

## Target architecture

```text
                            SENTINEL CORE
                ┌─────────────────────────────────┐
                │ Agents · Orchestration · Memory │
                │ Policy · Tools · Audit · Evals  │
                └───────────────┬─────────────────┘
                                │
                     Revenue Recovery domain
                ┌───────────────┴─────────────────┐
                │ Events · Detection · Incidents  │
                │ Analysis · Planning · Recovery  │
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

The Java backend remains the system of record. PostgreSQL is the only required infrastructure service. The dashboard is a separate Next.js and TypeScript application.

## Status: Phase 9 of 10 complete

| Phase | Focus | Status | Outcome |
|---|---|---|---|
| 1 | Sentinel Core and project hygiene | Complete | Extensible core without breaking `/investigate` |
| 2 | Revenue domain and persistence | Complete | Normalized payment and incident data in PostgreSQL |
| 3 | Detection and incident clustering | Complete | Explainable revenue incidents created without an LLM |
| 4 | Agentic investigation and memory | Complete | Evidence-backed root cause with validated structured output |
| 5 | Recovery planning, policy, and audit | Complete | Governed AUTO/HUMAN/DENY decisions |
| 6 | Razorpay Test Mode execution | Complete | Idempotent Test Mode Payment Link execution with reconciliation |
| 7 | Outcome loop and revenue measurement | Complete | Signed idempotent outcomes and reconciled Test Mode metrics |
| 8 | Operational dashboard | Complete | Complete workflow visible without relying on Postman |
| 9 | Evaluation, testing, and resilience | Complete | Reproducible quality, safety, and failure evidence |
| 10 | Integration, release, and submission | Next | Reproducible Buildathon-ready release and demo |

---

## Phase 1 — Sentinel Core and project hygiene

**Goal:** Refactor the prototype into an extensible core while preserving existing behaviour.

### Work

- Keep Java 17, Maven, and Spring Boot; do not rewrite the backend in another language.
- Remove generated Maven `target/` output from version control and add appropriate ignore rules.
- Introduce core packages for agents, orchestration, memory, policy, audit, LLM clients, tools, and evaluation.
- Extract orchestration out of `InvestigateController` into an application service.
- Refactor `GeminiService` behind an `LlmClient` interface with validated structured generation.
- Refactor `EmbeddingService` behind an `EmbeddingClient` interface.
- Add a generic `SentinelAgent<I, O>` abstraction and a common structured `AgentResult`.
- Define shared evidence, confidence, status, and error models.
- Add request validation, consistent API errors, upstream timeouts, and safe configuration.
- Add initial unit and controller tests while preserving `POST /investigate`.

### Deliverable

An extensible Sentinel Core with the original runbook-based investigation flow still working.

### Exit criteria

- The existing investigation endpoint passes a regression test.
- External model calls are behind interfaces and can be mocked.
- Agents return structured results rather than unvalidated free-form text.
- The project builds and tests without generated artifacts being tracked.

**Current state:** Complete. The regression, validation, orchestration, similarity, structured-output, and agent-boundary tests run without live Gemini calls.

---

## Phase 2 — Revenue domain and persistence

**Goal:** Establish the normalized data model and durable state needed by the recovery workflow.

### Work

- Add Spring Data JPA, PostgreSQL, Flyway, Bean Validation, and Spring Boot Test.
- Create the normalized `PaymentEvent` model for both synthetic and Razorpay-derived data.
- Create `RevenueIncident` with amount at risk, affected payments/customers, evidence, findings, plan, actions, and outcome.
- Implement the incident state machine:

```text
DETECTED → INVESTIGATING → DIAGNOSED → PLANNING → POLICY_REVIEW
         → APPROVED/HUMAN_REVIEW → EXECUTING → MONITORING
         → RECOVERED/FAILED/STOPPED
```

- Create initial tables for payment events, incidents, findings, plans, actions, outcomes, audit events, processed webhooks, and historical incidents.
- Add batch ingestion through `POST /api/v1/revenue/events/batch`.
- Create a repeatable labelled dataset with 200–500 synthetic payment events.
- Include UPI degradation, provider outage, normal failures, insufficient funds, abandonment, mixed degradation, already-paid, high-value, duplicate, and API-failure scenarios.
- Store money in the smallest currency unit and use safe numeric types.

### Deliverable

A batch of normalized payment events can be persisted and converted into durable revenue-domain state.

### Exit criteria

- Migrations create a clean database from scratch.
- Batch ingestion is validated and idempotent.
- State-machine transitions reject invalid movement.
- Synthetic datasets contain ground-truth labels and no real customer data.

### Phase 2 implementation notes

- PostgreSQL is the only persistence backend. Hibernate validates the schema;
  Flyway owns schema creation through `V1__create_revenue_core_tables.sql` and
  `V2__create_recovery_and_audit_tables.sql`.
- All monetary values use `BIGINT` minor units (`amountMinor`,
  `amountAtRiskMinor`, and related recovery fields). Floating-point money is not
  used.
- `POST /api/v1/revenue/events/batch` accepts an `events` array. It validates
  each event independently, persists valid rows, and returns `count`,
  `duplicatesSkipped`, and `validationErrors`. The database and service both
  enforce the `(paymentId, attemptNumber)` idempotency key.
- `SyntheticPaymentDatasetGenerator` uses DataFaker with the fixed seed
  `20260827` to create 300 non-PII, ground-truth-labelled events. It covers all
  ten scenarios in this phase and intentionally contains 15 duplicate rows.
- The endpoint integration test posts that dataset twice: the first request is
  expected to persist 285 unique events, and the second is expected to skip all
  300 rows.

To migrate an empty PostgreSQL database, set `SENTINEL_DB_URL`,
`SENTINEL_DB_USERNAME`, and `SENTINEL_DB_PASSWORD`, then pass them to Flyway:

```powershell
mvn flyway:migrate `
  "-Dflyway.url=$env:SENTINEL_DB_URL" `
  "-Dflyway.user=$env:SENTINEL_DB_USERNAME" `
  "-Dflyway.password=$env:SENTINEL_DB_PASSWORD"
```

`mvn clean verify` requires a running Docker-compatible container runtime. The
repository and endpoint integration tests always use PostgreSQL 16 through
Testcontainers; they do not substitute H2.

---

## Phase 3 — Detection and incident clustering

**Goal:** Detect explainable revenue risk deterministically, without depending on an LLM.

### Work

- Compute overall, payment-method, and issuer/bank success rates.
- Track failure-code distributions, retry frequency, value concentration, abandonment, and baseline deviation.
- Make detection thresholds configurable: minimum volume, success-rate drop, baseline deviation, and amount at risk.
- Cluster related failures by method, issuer, error code, merchant, time window, and failure signature.
- Calculate affected transaction count, customer count, and total amount at risk.
- Prevent random individual failures from creating noisy incidents.
- Record exactly which rules and evidence caused an incident to be created.
- Add demo injection endpoints for a clean reset and a repeatable UPI-outage scenario.

### Deliverable

Uploading a degraded payment batch automatically creates a clustered `RevenueIncident` with quantified revenue at risk.

### Exit criteria

- Labelled anomaly scenarios are detected consistently.
- Normal random failures do not exceed the agreed false-positive threshold.
- Each incident explains the triggering rules and contributing events.
- Detection works when Gemini is unavailable.

### Phase 3 detection defaults

All settings are externalized under `sentinel.detection` in
`application.yml` and can be overridden with normal Spring Boot configuration:

| Property | Default | Meaning |
| --- | ---: | --- |
| `evaluation-window` | `60m` | Cohort used to calculate cluster statistics |
| `cluster-window` | `60m` | Maximum span of one matching failure cluster |
| `baseline-window` | `24h` | Historical lookback preceding the cluster |
| `baseline-bucket` | `15m` | Bucket size for rolling success-rate samples |
| `minimum-volume` | `10` | Minimum failed events in the cluster |
| `minimum-success-rate-drop` | `0.20` | Minimum absolute drop from baseline |
| `minimum-baseline-deviation` | `2.0` | Minimum drop in baseline standard deviations |
| `minimum-amount-at-risk-minor` | `100000` | Minimum failed value in currency minor units |
| `default-baseline-success-rate` | `0.95` | Cold-start baseline when history is empty |
| `minimum-baseline-standard-deviation` | `0.05` | Stable denominator for flat/cold baselines |
| `success-statuses` | `CAPTURED`, `AUTHORIZED` | Statuses counted as successful |
| `merchant-metadata-key` | `merchantId` | Normalized-event metadata key used for clustering |

An incident is created only when all four detection rules pass. Every rule
records `PASS` or `FAIL`, its actual value, comparison operator, threshold, and
unit. Failures are clustered by method, issuer/bank, error code, merchant, and
time window before rules are evaluated.

Statistics use these explicit definitions:

- Retry frequency is the share of events with `attemptNumber > 1` or a positive
  `previousFailureCount`.
- Value concentration is the share of total value represented by the largest
  10 percent of events in the cohort.
- Abandonment rate is the share of events with status `ABANDONED`.
- Baseline deviation is `(rolling baseline success rate - current success rate)
  / baseline standard deviation`. Apache Commons Math calculates the rolling
  mean and standard deviation.

The repeatable live-demo flow is:

```text
POST /api/v1/demo/reset
POST /api/v1/demo/inject/upi-outage
```

The injection endpoint generates and ingests 30 fixed-seed normalized events,
runs the same statistics, clustering, and rule services as the production batch
endpoint, and returns the persisted incident summary. It does not call an LLM or
return a canned detection response.

---

## Phase 4 — Agentic investigation and memory

**Goal:** Produce a structured root-cause assessment grounded in computed evidence and historical incidents.

### Work

- Implement a Triage Agent that selects the relevant tools and investigation path.
- Implement a Payment Analyst that examines method, issuer, error, retry, and time patterns.
- Implement deterministic Pattern Analyzer and Customer Context tools instead of turning every calculation into an agent.
- Extend runbook retrieval into historical incident memory containing evidence, root cause, strategy, outcome, recovered amount, and success rate.
- Implement a Root Cause Agent that combines detection evidence, analyst findings, context, and memory.
- Require validated structured output containing root cause, confidence, evidence, and alternative hypotheses.
- Mask customer identifiers and exclude phone numbers, full email addresses, credentials, and raw payment details from prompts.
- Fall back to deterministic findings when the LLM is unavailable; never authorize recovery from an incomplete investigation.

### Deliverable

An incident can move from `DETECTED` to `DIAGNOSED` with evidence, confidence, alternatives, and an auditable agent timeline.

### Exit criteria

- Every diagnosis references visible evidence.
- Invalid or incomplete LLM output is rejected safely.
- The LLM receives only the minimum masked context required.
- Agents cannot execute financial actions.
- Root-cause accuracy is measurable against labelled scenarios.

### Phase 4 implementation notes

- `POST /api/v1/revenue/incidents/{id}/investigate` runs the real investigation
  pipeline and permits only `DETECTED → INVESTIGATING → DIAGNOSED`. It does not
  create plans, policy decisions, recovery actions, or execution side effects.
- `TriageAgent` deterministically selects the payment investigation path.
  `PaymentAnalystAgent` invokes the plain `PatternAnalyzer` and
  `CustomerContextTool` services; their percentages, counts, rolling-baseline
  deviation, retry totals, and historical recovery rate are computed from
  persisted records.
- `HistoricalMemoryService` embeds stored `HistoricalIncident` content and uses
  cosine similarity. Recovery rate is derived as
  `recoveredAmountMinor / evidenceSummary.amountAtRiskMinor`; it is reported as
  unavailable when that real denominator is absent. An empty history returns no
  matches without calling the embedding provider or raising an error.
- `RootCauseAgent` requests the strict incident-report JSON shape from Gemini,
  validates it with Jakarta Bean Validation, and retries invalid/incomplete
  output once. Resilience4j applies a circuit breaker and a hard time limit. Two
  failed attempts produce a deterministic, explicitly LLM-unavailable diagnosis
  whose reduced confidence is 60 percent of the observed dominant failure
  signature, capped at `0.60`.
- `PromptContextBuilder` exposes counts and masked aliases such as
  `customer_0001`, never raw customer/payment IDs. It removes email addresses,
  phone numbers, UPI IDs, card-like values, and credential assignments before a
  prompt reaches `LlmClient`.
- `PAYMENT_ANALYST` and `ROOT_CAUSE_AGENT` findings are persisted with their
  evidence and confidence, preserving the visible investigation timeline.

Phase 4 settings are externalized under `sentinel.investigation`:

| Property | Default | Meaning |
| --- | ---: | --- |
| `timeout` | `5s` | Hard limit for one structured LLM attempt |
| `memory-top-k` | `5` | Maximum similar historical incidents returned |
| `memory-minimum-similarity` | `0.35` | Minimum cosine similarity for a memory match |
| `circuit-breaker-failure-rate` | `50` | Failure percentage that opens the breaker |
| `circuit-breaker-minimum-calls` | `2` | Calls required before failure rate is evaluated |
| `circuit-breaker-window-size` | `4` | Recent calls retained by the count-based window |
| `circuit-breaker-open-duration` | `30s` | Delay before a provider health probe is allowed |

---

## Phase 5 — Recovery planning, policy, and audit

**Goal:** Convert a diagnosis into a bounded proposal and deterministic decision.

### Work

- Implement a Recovery Planner Agent using root cause, confidence, customer context, historical strategy performance, and available tools.
- Support the initial strategy set:

```text
ALTERNATIVE_PAYMENT_LINK
DEFERRED_RETRY
RECOVERY_REMINDER
WAIT_FOR_PROVIDER
HUMAN_ESCALATION
NO_ACTION
```

- Treat planner output as a proposal only; it never grants itself execution permission.
- Build a deterministic Policy Engine that returns `AUTO`, `HUMAN`, or `DENY`.
- Gate decisions by confidence, amount, payment status, active recovery, retry count, customer limit, strategy allowlist, and duplicate-charge risk.
- Add mandatory stop rules for recovered payments, expired actions, maximum attempts, unacceptable risk, and possible duplicate charges.
- Build the human approval and rejection workflow for high-value or uncertain actions.
- Create an append-only audit event for every agent result, transition, proposal, rule evaluation, approval, action, and outcome.
- Make each policy result explain which rules passed or failed.

### Deliverable

A diagnosed incident produces a recovery proposal and an explainable AUTO/HUMAN/DENY policy result.

### Exit criteria

- No action can bypass the Policy Engine.
- Low-confidence and high-value cases require human review.
- Already-paid and duplicate-risk cases are stopped.
- Replaying the same request cannot create a second active recovery.
- The full decision history can be reconstructed from the audit log.

### Phase 5 implementation notes

- `POST /api/v1/revenue/incidents/{id}/plan` invokes
  `RecoveryPlannerAgent`, persists its proposal, evaluates mandatory stops,
  evaluates the Easy Rules allow gates, persists every rule result, and only
  then creates a guarded `RecoveryAction`. Planning never starts execution.
- Mandatory stops are a dedicated first pass. Already recovered/paid payments,
  expired proposals, exhausted attempts, unacceptable risk, and duplicate-charge
  signals return `DENY` without evaluating the permissive rules.
- Easy Rules `RuleListener` captures each permissive rule's PASS/FAIL result,
  actual value, comparison, threshold, and explanation. Confidence or amount
  failures produce `HUMAN`; unsafe payment state, an existing active recovery,
  or a non-allowlisted strategy produces `DENY`; all rules passing produces
  `AUTO`.
- Flyway migration `V3__enforce_recovery_and_audit_guards.sql` adds a PostgreSQL
  partial unique index over active action statuses. Concurrent inserts therefore
  cannot create two active recoveries for the same incident. It also installs a
  database trigger that rejects updates and deletes against `audit_events`.
- `AuditEventRepository` exposes only `append` and chronological `findTrail`
  operations—no general save, update, or delete surface. Detection, agent
  results, state changes, proposals, every policy rule, decisions, actions, and
  human decisions are represented as immutable events.
- Human decisions require a non-blank persisted actor and reason through:
  `POST /api/v1/revenue/actions/{actionId}/approve` and
  `POST /api/v1/revenue/actions/{actionId}/reject`.
- `GET /api/v1/revenue/incidents/{id}/audit-trail` returns the decision history
  as a chronological human-readable story, including the complete policy trace.

Policy settings are externalized under `sentinel.policy`:

| Property | Default | Meaning |
| --- | ---: | --- |
| `auto-confidence-threshold` | `0.85` | Minimum confidence for autonomous approval |
| `maximum-auto-amount-minor` | `100000` | Maximum value eligible for autonomous approval |
| `maximum-attempts` | `3` | Mandatory stop boundary for attempts |
| `per-customer-action-limit` | `2` | Maximum prior actions before human review |
| `maximum-risk-score` | `0.70` | Mandatory risk-score ceiling |
| `action-ttl` | `30m` | Proposal validity window |
| `allowed-strategies` | Four bounded strategies | Strategies eligible to proceed |
| `paid-or-refunded-statuses` | `CAPTURED`, `AUTHORIZED`, `PAID`, `REFUNDED` | Payment states that cannot be recovered again |

---

## Phase 6 — Razorpay Test Mode execution

**Goal:** Execute one complete real recovery strategy safely through Razorpay Test Mode.

### Work

- Implement a narrow `RazorpayClient` for payment lookup, Payment Link creation/fetch/cancellation, and supported notifications.
- Configure `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, and `RAZORPAY_WEBHOOK_SECRET` through environment variables.
- Validate planned API requests and webhook details against current official Razorpay documentation during implementation.
- Implement **Alternative Payment Link** as the first fully functional strategy.
- Allow the plan to avoid a degraded payment method when supported by the integration.
- Persist the mapping between recovery actions, affected payments, and external Test Mode resources.
- Make execution idempotent and verify that the original payment has not already succeeded.
- Add bounded retries, timeout handling, and a circuit breaker.
- Move failed external calls to a controlled state such as `RETRY_PENDING`; never hammer the provider.
- Keep all execution in Test Mode—no real-money automation.

### Deliverable

An approved low-risk action creates a real Razorpay Test Mode Payment Link exactly once.

### Exit criteria

- Credentials never enter source control, logs, prompts, or API responses.
- Autonomous execution occurs only after a persisted `AUTO` policy decision.
- Human-gated actions execute only after recorded approval.
- Razorpay timeouts and errors leave the action in a safe recoverable state.
- Repeated execution requests do not create duplicate links.

### Phase 6 implementation notes

- `POST /api/v1/revenue/incidents/{incidentId}/execute` executes only an
  `ALTERNATIVE_PAYMENT_LINK` action with persisted `AUTO_APPROVED` permission
  or a HUMAN action that has reached `APPROVED` with `approvedAt` recorded.
- One action targets one deterministic failed payment, one masked customer,
  one currency, and that payment's exact minor-unit amount. Other incident
  payments are recorded as outside the Phase 6 action scope.
- The deterministic provider reference is `sntl_` plus the action UUID without
  hyphens (37 characters). A PostgreSQL row lock serializes execution requests,
  the reference is unique, and repeated requests return the stored resource.
- Sentinel checks local payment state first. IDs beginning with `pay_` are also
  fetched from Razorpay before link creation; authorized, captured, paid, or
  refunded payments are stopped to avoid duplicate-charge risk.
- Every create is preceded by a reference lookup. A timeout or ambiguous 5xx is
  never blindly retried: Sentinel looks up the same reference and either
  recovers the link or records `EXECUTION_UNCERTAIN`/`RETRY_PENDING`.
- Safe reads retry bounded 429/temporary failures with exponential backoff and
  jitter. Ordinary 4xx failures are non-retryable, response bodies are not
  persisted, and the circuit breaker produces a sanitized retry-pending result.
- Payment Links use `accept_partial=false`, bounded expiry, masked notes,
  notifications off by default, and checkout options with UPI disabled while
  cards and netbanking remain enabled for the UPI-outage recovery path.
- Cancellation and notification adapter operations are available at
  `POST /api/v1/revenue/actions/{actionId}/cancel` and
  `POST /api/v1/revenue/actions/{actionId}/notify/{sms|email}`. Notifications
  remain disabled unless explicitly enabled.
- Execution claimed, payment verification, provider request, reconciliation,
  success, uncertainty/retry, failure, and cancellation are append-only audit
  events and appear in the existing incident audit-trail endpoint.

Razorpay execution is disabled by default and rejects any `rzp_live_` key:

| Environment variable | Default | Meaning |
| --- | --- | --- |
| `RAZORPAY_ENABLED` | `false` | Enables the Test Mode adapter |
| `RAZORPAY_KEY_ID` | empty | Must begin with `rzp_test_` when enabled |
| `RAZORPAY_KEY_SECRET` | empty | Read only by the provider adapter |
| `RAZORPAY_BASE_URL` | `https://api.razorpay.com` | Override only for isolated tests |

The remaining timeouts, expiry, retry, notification, and circuit-breaker
settings are under `sentinel.razorpay.*` in `application.yml`. Razorpay Test
Mode currently limits an account to 30 Payment Links, so cancel old demo links
or use a fresh Test Mode account when the provider reports that limit.

#### 60–90 second Test Mode demo

```powershell
$env:RAZORPAY_ENABLED="true"
$env:RAZORPAY_KEY_ID="rzp_test_your_key"
$env:RAZORPAY_KEY_SECRET="your_test_secret"
mvn spring-boot:run

$demo = Invoke-RestMethod -Method Post http://localhost:8080/api/v1/demo/inject/upi-outage
$id = $demo.incidents[0].incidentId
Invoke-RestMethod -Method Post "http://localhost:8080/api/v1/revenue/incidents/$id/investigate"
$plan = Invoke-RestMethod -Method Post "http://localhost:8080/api/v1/revenue/incidents/$id/plan"
if ($plan.policyDecision -eq "HUMAN") {
  Invoke-RestMethod -Method Post -ContentType application/json -Body '{"actor":"demo-reviewer","reason":"Verified Test Mode demo"}' "http://localhost:8080/api/v1/revenue/actions/$($plan.actionId)/approve"
}
$first = Invoke-RestMethod -Method Post "http://localhost:8080/api/v1/revenue/incidents/$id/execute"
$second = Invoke-RestMethod -Method Post "http://localhost:8080/api/v1/revenue/incidents/$id/execute"
$first; $second; Invoke-RestMethod "http://localhost:8080/api/v1/revenue/incidents/$id/audit-trail"
```

Both execution responses must show the same provider ID/reference/short URL,
and `mode` must be `TEST`. The second response reports `existing=true`.

The optional live Test Mode smoke test is skipped by default. It creates one
₹1.00 link only when explicitly enabled:

```powershell
$env:RAZORPAY_SMOKE="true"
$env:RAZORPAY_KEY_ID="rzp_test_your_key"
$env:RAZORPAY_KEY_SECRET="your_test_secret"
mvn -Dtest=RazorpayTestModeSmokeTest test
```

Phase 6 does not process payment outcomes; webhook verification and revenue
measurement remain Phase 7.

---

## Phase 7 — Outcome loop and revenue measurement

**Goal:** Observe recovery outcomes, close incidents, and measure results honestly.

### Work

- Add `POST /api/v1/webhooks/razorpay` while retaining the raw request body.
- Validate webhook signatures using the configured secret and the required HMAC procedure.
- Persist external event IDs before processing so duplicate webhooks are safely acknowledged and ignored.
- Support Payment Link paid, partially paid, and cancelled lifecycle outcomes required by the demo.
- Update action, incident, and outcome state transactionally.
- Cancel recovery when the original payment succeeds elsewhere.
- Compute revenue at risk, attempted recovery, recovered revenue, recovery rate, and strategy performance.
- Label all financial results as **Recovered Revenue — Test Mode / Synthetic Evaluation**.
- Feed completed incident outcomes into historical memory for later strategy analysis.

### Deliverable

A completed Test Mode payment updates the action, closes or advances the incident, increases recovered revenue, and records every step.

### Exit criteria

- A valid webhook changes state and revenue exactly once.
- Invalid signatures are rejected and audited.
- Duplicate events return safely without duplicate financial updates.
- Recovered-revenue totals reconcile with individual outcomes.
- The closed loop works: detect → diagnose → plan → guard → act → observe → measure.

### Phase 7 implementation notes

- `POST /api/v1/webhooks/razorpay` receives the exact request bytes. Sentinel
  verifies `X-Razorpay-Signature` with HMAC-SHA256 and constant-time comparison
  before parsing JSON. `X-Razorpay-Event-Id` is mandatory after signature
  validation.
- Subscribe the Test Mode webhook to `payment_link.paid`,
  `payment_link.partially_paid`, and `payment_link.cancelled` only. Configure
  the endpoint secret as `RAZORPAY_WEBHOOK_SECRET`; it is never returned,
  logged, audited, persisted, or passed to an LLM.
- Razorpay delivery is treated as at-least-once and potentially out of order.
  The event-ID database constraint and locked action row make duplicate and
  concurrent delivery idempotent. Paid is terminal, cumulative partial amounts
  only increase, cancellation cannot reverse paid, and a later verified paid
  event can supersede an earlier cancellation.
- Events match only `RecoveryAction.externalResourceId` (the Razorpay Payment
  Link ID). Customer details, descriptions, phones, emails, and reference text
  are never used as identity.
- Sentinel stores the event ID/type, link ID, SHA-256 payload digest, timestamps,
  disposition, and a minimized non-customer payload projection. Invalid
  signatures are represented in a separate append-only security table using
  only the request digest, timestamp, header presence, and safe reason.
- The normal webhook path never fetches Razorpay. It updates the current outcome
  projection transactionally and responds immediately; provider reconciliation
  remains an explicit Phase 6 operation.

`GET /api/v1/revenue/metrics` returns:

| Metric | Definition |
| --- | --- |
| Revenue at risk | Sum of persisted incident `amountAtRiskMinor` |
| Attempted recovery | Exact action amount after a Payment Link was created |
| Recovered revenue | Latest verified cumulative `amount_paid` per outcome |
| Recovery rate | Recovered revenue divided by attempted recovery, zero-safe |
| Strategy performance | Attempted and recovered amounts grouped by the persisted plan strategy |

Every response is labelled **Recovered Revenue — Test Mode / Synthetic
Evaluation** and carries `mode: TEST`. Metrics read current outcome projections,
not webhook-event rows, so partial and duplicate events cannot inflate totals.

#### Manual signed-fixture test

```powershell
$secret = $env:RAZORPAY_WEBHOOK_SECRET
$body = '{"event":"payment_link.paid","payload":{"payment_link":{"entity":{"id":"plink_REPLACE","amount":12345,"amount_paid":12345,"currency":"INR","status":"paid"}}}}'
$hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($secret))
$signature = [Convert]::ToHexString($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($body))).ToLowerInvariant()
Invoke-RestMethod -Method Post -ContentType application/json -Body $body `
  -Headers @{'X-Razorpay-Signature'=$signature;'X-Razorpay-Event-Id'='manual_event_001'} `
  http://localhost:8080/api/v1/webhooks/razorpay
Invoke-RestMethod http://localhost:8080/api/v1/revenue/metrics
```

Replace the link ID and exact amount with the resource returned by Phase 6.
Sending the same event ID again returns `DUPLICATE` with no metric change.

For a real Test Mode flow, expose only the webhook endpoint over HTTPS, register
that URL in the Razorpay Test Mode dashboard, and use the same dashboard secret
as `RAZORPAY_WEBHOOK_SECRET`. Razorpay recommends a zrok tunnel for local demos
when common tunnel services are unavailable. Never expose a local development
database or management port through the tunnel.

Known limitations: Phase 7 handles the three Payment Link lifecycle events
needed for the buildathon, stores no full customer payload, performs no dashboard
updates, and does not implement generic payment/order webhook families.

---

## Phase 8 — Operational dashboard

**Goal:** Make the complete operational story understandable and controllable from one interface.

### Work

- Build a Next.js and TypeScript dashboard.
- Add overview cards for revenue at risk, recovered revenue, recovery rate, and active incidents.
- Add recovery trends, failure distribution, incident feed, strategy performance, and agent status.
- Build an incident detail page with amounts, affected transactions/customers, state, and pipeline progress.
- Display agent findings, confidence, and supporting evidence—not hidden chain-of-thought.
- Display the proposed strategy, recoverable amount, predicted outcome, and policy rules.
- Add an approval queue with approve/reject controls and the reason human review is required.
- Add action results, webhook outcomes, and the immutable audit timeline.
- Support the demo reset and UPI-outage injection flow from the UI.

### Deliverable

The primary five-minute story can be demonstrated from the dashboard without Postman.

### Exit criteria

- A user can inject a scenario, inspect the incident, approve a gated action, and see the outcome.
- Every important decision is visible with evidence and status.
- Financial metrics clearly state Test Mode/synthetic scope.
- Loading, empty, failure, and partial-result states are usable.

---

## Phase 9 — Evaluation, testing, and resilience

**Goal:** Prove that Sentinel is effective, policy-compliant, and safe under failure.

### Work

- Run all 200–500 labelled payment events through a reproducible evaluation harness.
- Measure detection precision/recall, root-cause accuracy, recovery rate, policy compliance, escalation rate, false interventions, duplicate actions, and latency.
- Report recovered revenue and recovery performance by strategy.
- Add unit tests for calculations, clustering, state transitions, policy rules, signature validation, idempotency, and metrics.
- Add integration tests for database migrations, APIs, agent orchestration, execution, and webhook processing.
- Test LLM outage, Razorpay outage, invalid structured output, low confidence, already-paid customers, duplicate actions, and duplicate webhooks.
- Add safe structured logs, correlation IDs, health checks, and stage-level latency/cost observations.
- Verify PII minimization and prompt-injection boundaries for runbooks and external evidence.
- Add CI that builds the backend/dashboard and runs deterministic tests without live credentials.

### Deliverable

An evaluation report and automated test suite demonstrating system quality, safety, and graceful degradation.

### Exit criteria

- Evaluation results are generated from a clean run and displayed in the dashboard/report.
- Policy compliance is 100% for the labelled evaluation scenarios.
- Duplicate recovery actions remain zero.
- LLM failure prevents autonomous recovery and returns deterministic diagnostics.
- Razorpay failure is bounded and does not cause repeated uncontrolled calls.
- All required backend and frontend checks pass in CI.

---

## Phase 10 — Integration, release, and submission

**Goal:** Freeze features and deliver a reproducible, polished Buildathon release.

### Work

- Integrate the complete path from dataset load through measured recovery.
- Fix defects only; do not introduce risky experimental features during release hardening.
- Containerize or otherwise document repeatable PostgreSQL, backend, and dashboard startup.
- Add configuration examples with placeholder credentials.
- Publish architecture, API, evaluation, privacy, operations, and demo documentation.
- Create the five-minute demo script and stable synthetic scenario.
- Capture screenshots and a backup demo recording.
- Clean the public repository, verify licensing, and scan for secrets and generated files.
- Test setup, migrations, seed data, failure paths, reset, and the demo from a clean environment.
- Record known limitations and a post-Buildathon roadmap for additional Sentinel domain packs.

### Deliverable

A versioned, documented Sentinel Revenue Intelligence release that can be set up and demonstrated reliably.

### Exit criteria

From a clean environment, the team can:

1. Start PostgreSQL, the Sentinel backend, and the dashboard.
2. Load the labelled payment dataset.
3. Detect a revenue anomaly and create a clustered incident.
4. Run agentic investigation and produce an evidence-backed root cause.
5. Generate a recovery proposal and deterministic policy result.
6. Auto-execute a safe action or request human approval.
7. Create the Razorpay Test Mode recovery resource.
8. Receive and validate the outcome webhook.
9. Update recovered revenue exactly once.
10. Display evaluation metrics and the complete audit trail.

## Planned API surface

```text
POST /investigate                                  # existing engineering flow

POST /api/v1/revenue/events/batch                 # ingest evaluation/payment events
GET  /api/v1/revenue/incidents                    # list incidents
GET  /api/v1/revenue/incidents/{id}               # incident details
POST /api/v1/revenue/incidents/{id}/investigate   # run agent investigation
POST /api/v1/revenue/incidents/{id}/plan          # propose recovery
POST /api/v1/revenue/incidents/{id}/execute       # execute an approved action
POST /api/v1/revenue/actions/{id}/approve         # human approval
POST /api/v1/revenue/actions/{id}/reject          # human rejection
GET  /api/v1/revenue/metrics                      # evaluation/recovery metrics
GET  /api/v1/revenue/incidents/{id}/audit-trail   # immutable incident timeline
POST /api/v1/webhooks/razorpay                    # signed outcome events

POST /api/v1/demo/reset                           # reset synthetic demo state
POST /api/v1/demo/inject/upi-outage               # repeatable demo incident
```

## Priority rules

### P0 — Required for submission

- Payment-event batch
- Deterministic revenue detection
- Agentic investigation and evidence-backed root cause
- Recovery planning
- Deterministic Policy Engine
- Razorpay Test Mode action
- Webhook-driven outcome
- Immutable audit trail
- Recovered-revenue metric
- Usable dashboard

### P1 — Strong differentiators

- Historical incident memory
- Strategy success statistics
- Human approval queue
- Incident clustering
- Graceful API fallback
- Evaluation harness
- Live agent/evidence timeline

### P2 — Stretch only after P0 is stable

- pgvector
- Server-sent event streaming
- Subscription recovery
- Automatic retry scheduling
- Multiple merchants
- Advanced anomaly models
- Model routing
- MCP integration
- Voice recovery

P2 work must never endanger the complete P0 recovery loop.

## Deliberate non-goals

For the Buildathon release, do not add:

- A large collection of superficial agents
- Kubernetes or unnecessary microservices
- Kafka without a demonstrated need
- A custom ML anomaly model before deterministic rules work
- Multiple LLM providers
- A mobile application
- A full authentication platform, CRM, or billing system
- Real-money automation
- A giant RAG corpus
- Fake autonomous behaviour

The system should be deep, bounded, explainable, and demonstrably useful.

## Core success metrics

- Total transactions and value evaluated
- Revenue at risk
- Recovery attempted
- **Recovered Revenue — Test Mode / Synthetic Evaluation**
- Recovery rate overall and by strategy
- Detection precision and recall
- Root-cause accuracy
- Policy compliance and human-escalation rates
- False-intervention rate
- Duplicate recovery actions
- Duplicate webhooks safely ignored
- Mean decision and recovery latency

## Suggested eight-day mapping

| Buildathon day | Phases | Demonstrable result |
|---|---|---|
| Day 1 | Phase 1 | Extensible Sentinel Core; existing investigation still works |
| Day 2 | Phases 2–3 | Batch upload creates a quantified revenue incident |
| Day 3 | Phase 4 | Agents produce root cause, evidence, and confidence |
| Day 4 | Phase 5 | Recovery proposal receives AUTO/HUMAN/DENY decision |
| Day 5 | Phases 6–7 | Test Mode link → payment → webhook → recovered revenue |
| Day 6 | Phase 8 | Complete operational flow visible in the dashboard |
| Day 7 | Phase 9 | Evaluation results, automated tests, and failure demos |
| Day 8 | Phase 10 | Feature freeze, clean setup, documentation, and pitch |

## Post-Buildathon direction

Once Revenue Recovery is stable, Sentinel Core can support additional domain packs:

```text
Sentinel Core
├── Revenue Intelligence
│   └── Razorpay Revenue Recovery
├── Engineering Intelligence
│   ├── GitHub change analysis
│   ├── Log analysis
│   └── Runbook retrieval
├── Infrastructure Intelligence
├── Security Incident Intelligence
└── Custom Domain SDK
```

## Phase 8 dashboard

The operational dashboard lives in `dashboard/` as a separate Next.js and TypeScript application. It never receives Razorpay credentials, webhook bodies, customer identifiers, or raw payment data. The Spring Boot backend remains the source of truth for incident state, policy decisions, execution, idempotency, and recovered-revenue measurement.

### Run locally

1. Start PostgreSQL and the Sentinel backend as described above. The backend listens on `http://localhost:8080` by default.
2. Copy `dashboard/.env.example` to `dashboard/.env.local`. Keep `NEXT_PUBLIC_USE_FIXTURES=false` for the real demonstration.
3. From `dashboard/`, run `npm install` and then `npm run dev`.
4. Open `http://localhost:3000`.

Only `NEXT_PUBLIC_SENTINEL_API_URL` belongs in the browser configuration. Razorpay keys and the webhook secret stay in backend environment variables.

### Five-minute dashboard demo

1. Open **Demo controls**, reset the synthetic state, and inject the UPI outage. The fixed labelled batch goes through real ingestion and rule-based detection.
2. Open the created incident. Run **Investigation**, then inspect computed evidence and the root-cause confidence.
3. Build the recovery plan. Read the individual mandatory-stop and allow-rule results in **Policy trace**.
4. If the result is **HUMAN**, open **Approval queue**, provide an actor identity and reason, and approve or reject. For **AUTO**, continue directly.
5. Execute the approved action once. Sentinel creates or safely recovers one Razorpay Test Mode Payment Link; replaying the request does not create another active action.
6. Pay the Test Mode link. After Razorpay sends the signed webhook, use **Refresh**. While an incident is monitoring an outcome, the detail view polls modestly every eight seconds and labels that behavior explicitly.
7. Open **Metrics & audit**. Confirm that recovered revenue advances exactly once and that detection, diagnosis, policy rule trace, approval, execution, webhook verification, and duplicate safety form one chronological story.

Every financial surface is labelled **TEST MODE / SYNTHETIC EVALUATION**. The dashboard uses controlled refresh and narrow mutation invalidation; it does not claim to be a streaming interface.

### Dashboard verification

```bash
cd dashboard
npm run verify
```

This runs linting, strict TypeScript checking, focused workflow tests, and the production Next.js build. The primary desktop and narrow mobile layouts are designed for keyboard and touch use; `Cmd/Ctrl+K` opens navigation, `Escape` closes dialogs, focus states are visible, and reduced-motion preferences are honored globally.

### Dashboard references

- [Next.js documentation](https://nextjs.org/docs)
- [shadcn/ui Sidebar](https://ui.shadcn.com/docs/components/base/sidebar) and [Chart](https://ui.shadcn.com/docs/components/base/chart)
- [TanStack Query](https://tanstack.com/query/latest/docs/framework/react/overview)
- [Motion for React](https://motion.dev/docs/react) and [reduced-motion accessibility](https://motion.dev/docs/react-accessibility)
- [Lucide React](https://lucide.dev/guide/react)
- [Radix accessibility guidance](https://www.radix-ui.com/primitives/docs/overview/accessibility)

## Engineering references

Phase 1 follows primary documentation and representative open-source implementations:

- [Spring Boot type-safe external configuration and validation](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [Spring Framework REST error handling](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Google Gemini structured output](https://ai.google.dev/gemini-api/docs/structured-output)
- [Java 17 HTTP client timeouts](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.Builder.html)
- [Google Gen AI Java SDK examples](https://github.com/googleapis/java-genai)
- [Spring PetClinic reference application](https://github.com/spring-projects/spring-petclinic)
- [Razorpay Payment Links API](https://razorpay.com/docs/api/payments/payment-links/)
- [Create Standard Payment Link](https://razorpay.com/docs/api/payments/payment-links/create-standard/)
- [Fetch Payment Links by reference ID](https://razorpay.com/docs/api/payments/payment-links/fetch-all-standard/)
- [Customise Payment Link payment methods](https://razorpay.com/docs/api/payments/payment-links/customise-payment-methods/)
- [Razorpay API errors and rate limiting](https://razorpay.com/docs/api/understand/)
- [Official Razorpay Java SDK reference](https://github.com/razorpay/razorpay-java)
- [Resilience4j](https://github.com/resilience4j/resilience4j)
- [WireMock](https://wiremock.org/docs/spring-boot/)
- [Testcontainers PostgreSQL](https://java.testcontainers.org/modules/databases/postgres/)
- [Razorpay Test webhook validation](https://razorpay.com/docs/webhooks/validate-test/)
- [Razorpay webhook best practices](https://razorpay.com/docs/webhooks/best-practices/)
- [Razorpay Payment Link webhook payloads](https://razorpay.com/docs/webhooks/payment-links/)
- [Razorpay Payment Link states](https://razorpay.com/docs/payments/payment-links/states/)
- [zrok local tunnel](https://zrok.io/)

## License

MIT
