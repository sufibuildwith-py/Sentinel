package com.sentinel.core.agent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentResultTest {

    @Test
    void copiesStructuredCollectionsAtTheBoundary() {
        List<Evidence> evidence = new ArrayList<>();
        evidence.add(new Evidence("detector", "Success rate dropped", Instant.now(), null));
        Instant started = Instant.parse("2026-08-27T10:00:00Z");

        AgentResult<String> result = new AgentResult<>(
                "triage",
                "Payment degradation detected",
                new Confidence(0.9),
                evidence,
                List.of(new Recommendation("investigate", "The deviation is material")),
                started,
                started.plusSeconds(1),
                AgentStatus.SUCCEEDED,
                "UPI_DEGRADATION"
        );
        evidence.clear();

        assertThat(result.evidence()).hasSize(1);
        assertThat(result.output()).isEqualTo("UPI_DEGRADATION");
        assertThatThrownBy(() -> result.evidence().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsInvalidConfidence() {
        assertThatThrownBy(() -> new Confidence(1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
