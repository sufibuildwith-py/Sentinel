package com.sentinel.revenue.experiment;

import com.sentinel.revenue.opportunity.OpportunityAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegretAnalysisServiceTest {
    @Test
    void regretConsidersOnlyAlreadyApprovedAlternativesAndCannotOverrideLiveAuthority() {
        RegretAnalysis result = new RegretAnalysisService().analyze(UUID.randomUUID(),
                OpportunityAction.NO_ACTION, new BigDecimal("100"), List.of(
                        new RegretCandidate(OpportunityAction.CREATE_PAYMENT_LINK, new BigDecimal("500"),
                                true, "CONTROLLED_HOLDOUT"),
                        new RegretCandidate(OpportunityAction.CREATE_NEW_ORDER, new BigDecimal("9999"),
                                false, "SIMULATED")), "policy-v1", "model-v1");
        assertThat(result.bestApprovedAlternative()).isEqualTo(OpportunityAction.CREATE_PAYMENT_LINK);
        assertThat(result.estimatedRegretMinor()).isEqualByComparingTo("400");
        assertThat(result.authorityState()).isEqualTo("LEARNING_SIGNAL_ONLY_NO_LIVE_OVERRIDE");
    }
}
