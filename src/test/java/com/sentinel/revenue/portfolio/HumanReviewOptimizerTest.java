package com.sentinel.revenue.portfolio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HumanReviewOptimizerTest {
    @Test
    void ranksReviewCapacityByExplainableValuePerMinute() {
        HumanReviewCandidate low = candidate("1000", 10);
        HumanReviewCandidate high = candidate("5000", 10);
        assertThat(new HumanReviewOptimizer().rank(List.of(low, high), 1))
                .containsExactly(high);
    }

    private HumanReviewCandidate candidate(String value, int minutes) {
        return new HumanReviewCandidate(UUID.randomUUID(), 10_000, new BigDecimal(value),
                new BigDecimal("0.2"), "MEDIUM", Instant.parse("2026-09-01T00:00:00Z"), minutes,
                "AUTOMATION_POLICY_LIMIT", "HUMAN_ESCALATION", List.of("NO_ACTION"),
                List.of("policy threshold exceeded"));
    }
}
