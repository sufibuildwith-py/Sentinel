package com.sentinel.revenue.portfolio;

import com.sentinel.revenue.opportunity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecoveryPortfolioOptimizerTest {
    @Test
    void allocatesHighestValueWithinExposureAndNeverWidensPolicyAuthority() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID denied = UUID.randomUUID();
        List<PortfolioCandidate> candidates = List.of(
                candidate(first, 60_000, "9000", true, true),
                candidate(second, 50_000, "8000", true, true),
                candidate(denied, 10_000, "99999", false, true));

        PortfolioAllocation result = new RecoveryPortfolioOptimizer(new ProviderCapabilityRegistry())
                .optimize(candidates, new PortfolioConstraints(60_000, 10, 10, 0));

        assertThat(result.selected()).singleElement().extracting(PortfolioCandidate::incidentId).isEqualTo(first);
        assertThat(result.casesPolicyBlocked()).isEqualTo(1);
        assertThat(result.bindingConstraint()).isEqualTo("EXPOSURE_BUDGET");
        assertThat(result.authorityState()).isEqualTo("ALLOCATION_PROPOSAL_ONLY");
    }

    @Test
    void unknownEconomicsBecomesNoActionInsteadOfAestheticEstimate() {
        PortfolioCandidate unknown = new PortfolioCandidate(UUID.randomUUID(), OpportunityAction.CREATE_PAYMENT_LINK,
                50_000, null, RecoveryRiskClass.LOW, false, false, true, true, "NOT_ESTIMATED");
        PortfolioAllocation result = new RecoveryPortfolioOptimizer(new ProviderCapabilityRegistry())
                .optimize(List.of(unknown), new PortfolioConstraints(100_000, 1, 1, 0));
        assertThat(result.selected()).isEmpty();
        assertThat(result.casesNoAction()).isEqualTo(1);
    }

    private PortfolioCandidate candidate(UUID id, long amount, String value, boolean policy, boolean governor) {
        return new PortfolioCandidate(id, OpportunityAction.CREATE_PAYMENT_LINK, amount,
                new BigDecimal(value), RecoveryRiskClass.LOW, false, false, policy, governor,
                "CONTROLLED_HOLDOUT");
    }
}
