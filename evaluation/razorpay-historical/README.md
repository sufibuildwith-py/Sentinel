# Razorpay Historical Validation Corpus

This corpus is **PUBLIC-SOURCE HISTORICAL VALIDATION**. It contains normalized metadata from unique public issues in repositories owned by the official Razorpay GitHub organization. It is not private merchant data, and the deterministic replays are not original transactions.

The importer retains canonical URLs, public issue identifiers, dates, bounded normalized facts, and SHA-256 source-content hashes. It does not republish issue bodies or copy payment identifiers, credentials, or customer data. Competitor and economic claims are outside this corpus; it scores safety, reconciliation, policy disposition, trace completeness, and deterministic replay.

Run `tools/Collect-OfficialGitHubCases.ps1` to rebuild the frozen input from public GitHub metadata. The collector is bounded to eight 100-result pages, respects public API rate limits, rejects empty/thin/unrelated records, deduplicates canonical provenance, and refuses to emit fewer than the requested accepted target. The generated manifest is loaded by the existing Sentinel evaluation module and exposed at `GET /api/v1/evaluation/historical`.

The API must display the actual accepted count and manifest hash. Never show the `500+` claim if validation or collection yields fewer than 500 accepted unique sources.
