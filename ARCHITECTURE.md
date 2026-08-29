# Sentinel architecture

Sentinel separates model-assisted investigation from deterministic financial authority. PostgreSQL is the system of record; every consequential decision is explainable through persisted evidence and the append-only audit trail.

```mermaid
flowchart TD
    Events[Payment events] --> Ingest[Validated idempotent ingestion]
    Ingest --> Stats[Statistics engine]
    Stats --> Detect[Detection rules and failure clustering]
    Detect --> Incident[(Revenue incident)]
    Incident --> Triage[Triage agent]
    Triage --> Tools[Pattern and customer-context tools]
    Tools --> Root[Root-cause agent]
    Memory[(Historical incident memory)] --> Root
    Root --> Plan[Recovery planner proposal]
    Memory --> Plan
    Plan --> Stop{Mandatory stop rules}
    Stop -->|Stop rule fired| Deny[DENY]
    Stop -->|No stop rule| Policy{Deterministic policy rules}
    Policy --> Auto[AUTO]
    Policy --> Human[HUMAN approval]
    Policy --> Deny
    Human -->|Approved with actor and reason| Execute[Replay-safe Razorpay Test Mode execution]
    Human -->|Rejected| Deny
    Auto --> Execute
    Execute --> Link[Payment Link]
    Link --> Webhook[Raw-body HMAC webhook verification]
    Webhook --> Ledger[(Idempotency and outcome ledger)]
    Ledger --> Metrics[Reconciled recovered-revenue metrics]
    Detect -. evidence .-> Audit[(Append-only audit trail)]
    Root -. finding .-> Audit
    Plan -. proposal .-> Audit
    Stop -. rule trace .-> Audit
    Policy -. rule trace .-> Audit
    Human -. decision .-> Audit
    Execute -. action .-> Audit
    Webhook -. verified outcome .-> Audit
```

## Runtime boundaries

- **Spring Boot / Java 17:** orchestration, rules, external gateways, evaluation, and APIs.
- **PostgreSQL / Flyway:** durable state, uniqueness guards, migrations, audit, and outcome reconciliation.
- **Next.js / TypeScript:** an operational read-and-command surface; no provider secrets or raw webhook bodies enter the browser.
- **Gemini:** structured diagnosis support behind `LlmClient` and `EmbeddingClient`; investigation has a bounded deterministic fallback.
- **Razorpay Test Mode:** the only execution target; policy or persisted human approval must grant permission first.

See [SETUP.md](SETUP.md) for the one-command runtime and [evaluation/README.md](evaluation/README.md) for the reproducible proof contract.
