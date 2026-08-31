# Sentinel Recovery Olympics

`Recovery Olympics v1` is a deterministic **SYNTHETIC / CONTROLLED BENCHMARK**. It is not production merchant data and is separate from the Historical Razorpay Validation Corpus.

- Seed: `20260901`
- Frozen cases: `10,000` (`7,000` development, `2,000` held-out, `1,000` adversarial)
- Run: `GET /api/v1/evaluation/recovery-olympics`
- Comparison arms: no intervention, blind intervention, static rules, two explicitly labelled documented approximations, Sentinel baseline, and Sentinel V2
- Integrity: all arms receive identical cases, natural-recovery outcomes, already-paid states, risk signals, and latent treatment outcomes

The report exposes gross and natural recovery separately, derives incremental and net value, includes intervention cost and Wilson 95% intervals, and never hides refusals, `NO_ACTION`, unsafe executions, policy violations, or losses. Decision latencies are deterministic logical fixtures rather than wall-clock measurements. No benchmark arm can call Razorpay, contact a customer, mutate production state, or grant execution authority.
