# Sentinel architecture

Sentinel separates model-assisted investigation from deterministic financial authority. PostgreSQL is the system of record; every consequential decision is explainable through persisted evidence and the append-only audit trail.

```mermaid
flowchart TD
    Events[Payment events] --> Ingest[Validated idempotent ingestion]
    Ingest --> Stats[Statistics engine]
    Stats --> Detect[Detection rules and failure clustering]
    Detect --> Incident[(Revenue incident)]
    Stats --> Health[Payment Health Radar]
    Health --> Systemic[(Systemic incident evidence)]
    Systemic --> Incident
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
    Auto --> Governor{Safety governor<br/>blast radius + kill switches}
    Governor -->|Allowed| Execute
    Governor -->|Denied / canary held| Deny
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
    Incident --> Replay[Immutable replay snapshot]
    Replay --> Shadow[Policy/model shadow comparison]
    Shadow -. zero-tool evidence .-> Audit
    Shadow -. no execution path .-> Deny
    Health --> Tower[Merchant Control Tower]
    Metrics --> Tower
    Governor --> Tower
    Shadow --> Tower
    Audit --> Capsule[Evidence Capsule]
    Capsule --> Tower
    Evaluation[Existing deterministic evaluation harness] --> FailureLab[Failure Lab]
    FailureLab -. fault / synthetic / shadow labels .-> Tower
    Economics[Counterfactual economics<br/>cost ledger + portfolio] --> Certificate[(Decision Certificate)]
    Policy --> Certificate
    Governor --> Certificate
    Certificate --> Tower
    Olympics[10K Recovery Olympics<br/>synthetic / controlled] --> Evaluation
    Historical[500 public-source Razorpay cases<br/>provenance-linked] --> HistoricalReplay[Source-derived safety replay]
    HistoricalReplay --> Evaluation
    HistoricalReplay -. no provider/customer tool path .-> Deny
```

## Runtime boundaries

- **Spring Boot / Java 17:** orchestration, rules, external gateways, evaluation, and APIs.
- **PostgreSQL / Flyway:** durable state, uniqueness guards, migrations, audit, and outcome reconciliation.
- **Next.js / TypeScript:** an operational read-and-command surface; no provider secrets or raw webhook bodies enter the browser.
- **Gemini:** structured diagnosis support behind `LlmClient` and `EmbeddingClient`; investigation has a bounded deterministic fallback.
- **Razorpay Test Mode:** the only execution target; policy or persisted human approval must grant permission first.
- **Replay/shadow boundary:** immutable, version-attributed evaluation with no provider, execution, webhook mutation, or communication adapter dependencies.
- **Control Tower / Failure Lab:** sanitized operational read models and truth-labelled safety evidence; neither surface can turn simulation or shadow output into recovered revenue.
- **V2 economics:** integer-minor-unit costs, evidence-classed counterfactuals, constrained portfolio ranking, and immutable version-attributed Decision Certificates remain proposals/evidence until deterministic authority permits execution.
- **Recovery Olympics / Historical Validation:** controlled synthetic economics and public-source historical safety are distinct proof systems and are never blended into one marketing number.

See [SETUP.md](SETUP.md) for the one-command runtime and [evaluation/README.md](evaluation/README.md) for the reproducible proof contract.
