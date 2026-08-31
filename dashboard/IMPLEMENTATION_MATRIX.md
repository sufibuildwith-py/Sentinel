# Sentinel Console implementation matrix

This matrix records the deployed contract used by the console. A blank or unavailable state is rendered when the API returns no evidence; the browser never manufactures financial truth.

| Capability | Backend contract | Console surface | Truth rule |
|---|---|---|---|
| Provider truth and recovery lifecycle | `GET /api/v1/revenue/incidents/{id}` | Recovery workbench | Acceptance stays distinct from confirmed recovery |
| Evidence Capsule and Agent Claims | `GET /api/v1/revenue/incidents/{id}/evidence-capsule` | Incident detail | Missing stages remain visible |
| Payment Health and systemic incidents | `GET /api/v1/revenue/control-tower` | Overview / Control Tower | Observed windows and baselines only |
| Financial attribution / Lost Revenue | `GET /api/v1/revenue/financial-attribution`, `GET /api/v1/revenue/lost-revenue` | Overview / Metrics / Control Tower | Unknown causal baselines remain unknown |
| Recovery Action Marketplace | `GET /api/v1/revenue/action-marketplace` | Intelligence | `NO_ACTION` is a first-class candidate |
| Counterfactual / timing / customer context | Incident-scoped V2 read endpoints | Intelligence / incident detail | Estimates are not authority or recovered money |
| Cost ledger / certificates | Incident-scoped V2 read endpoints | Intelligence / Audit / incident detail | Persisted values and hashes only |
| Policy, governor, kill switches, canaries | `GET /api/v1/revenue/control-tower` | Governance | Deterministic authority only |
| Model lifecycle, replay and shadow | `GET /api/v1/revenue/control-tower` | Governance | Shadow is zero-tool and visually labelled |
| Gemini runtime state | `GET /api/v1/diagnostics/llm` | Governance | Sanitized state only; never a key fragment |
| Recovery Olympics | `GET /api/v1/evaluation/recovery-olympics` | Evaluation | Controlled synthetic benchmark; small denominators labelled |
| Historical Razorpay validation | `GET /api/v1/evaluation/historical` | Evaluation | Public-source historical / derived replay, never merchant transactions |
| Failure Lab | Failure Lab scenario endpoints | Demo | Correct refusal is a successful safety outcome |

The UI uses independently implemented Tailwind, shadcn and Motion patterns. No third-party component source was copied into the repository.
