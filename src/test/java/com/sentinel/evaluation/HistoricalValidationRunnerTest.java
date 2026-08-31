package com.sentinel.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.revenue.model.RecoveryStrategy;
import com.sentinel.revenue.policy.MandatoryStopEvaluator;
import com.sentinel.revenue.policy.PolicyEngine;
import com.sentinel.revenue.policy.PolicyProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalValidationRunnerTest {
    private final HistoricalCorpusService corpus = new HistoricalCorpusService(new ObjectMapper());
    private final SensitiveDataScanner scanner = new SensitiveDataScanner();

    @Test
    void frozenCorpusHasFiveHundredUniquePublicProvenanceUnits() {
        var cases = corpus.frozenCases();
        assertThat(cases).hasSizeGreaterThanOrEqualTo(500);
        assertThat(cases).extracting(HistoricalValidationCase::canonicalSourceUrl).doesNotHaveDuplicates();
        assertThat(cases).extracting(item -> item.sourceRepository() + "#" + item.sourceId())
                .doesNotHaveDuplicates();
        assertThat(cases).allSatisfy(item -> {
            assertThat(item.sourceUrl()).startsWith("https://github.com/razorpay/");
            assertThat(item.sourceDate()).matches("\\d{4}-\\d{2}-\\d{2}");
            assertThat(item.paymentRail()).isNotBlank();
            assertThat(item.providerState()).isNotBlank();
            assertThat(item.derivedReplayIds()).hasSize(1);
        });
    }

    @Test
    void corpusContainsNoSensitiveValueAndManifestHashReproduces() {
        var cases = corpus.frozenCases();
        assertThat(corpus.manifestSha256()).matches("[0-9a-f]{64}");
        assertThat(corpus.manifestSha256()).isEqualTo(corpus.manifestSha256());
        assertThat(cases).allSatisfy(item -> assertThat(scanner.findings(
                item.sourceTitle() + " " + item.normalizedFailureReason() + " " + item.normalizationNotes()))
                .isEmpty());
    }

    @Test
    void historicalDerivedReplayIsDeterministicAndNeverClaimsUnverifiedMoney() {
        HistoricalValidationRunner runner = new HistoricalValidationRunner(corpus, policy());
        HistoricalValidationReport first = runner.evaluate();
        HistoricalValidationReport second = runner.evaluate();

        assertThat(first).isEqualTo(second);
        assertThat(first.acceptedPublicSourceCases()).isGreaterThanOrEqualTo(500);
        assertThat(first.derivedReplayCount()).isEqualTo(first.acceptedPublicSourceCases());
        assertThat(first.unsafeExecutions()).isZero();
        assertThat(first.duplicateFinancialEffects()).isZero();
        assertThat(first.unverifiedRecoveryClaims()).isZero();
        assertThat(first.failed()).isZero();
        assertThat(first.cases()).allMatch(HistoricalValidationReport.CaseResult::safeRefusal)
                .allMatch(HistoricalValidationReport.CaseResult::traceComplete)
                .allMatch(item -> item.evidenceLabel().equals("HISTORICAL-SOURCE-DERIVED REPLAY"));
    }

    private PolicyEngine policy() {
        PolicyProperties properties = new PolicyProperties(0.85, 100_000, 3, 3, 0.70,
                Duration.ofMinutes(30), Set.of(RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK,
                RecoveryStrategy.DEFERRED_RETRY, RecoveryStrategy.HUMAN_ESCALATION),
                Set.of("PAID", "CAPTURED", "REFUNDED"));
        return new PolicyEngine(properties, new MandatoryStopEvaluator(properties));
    }
}
