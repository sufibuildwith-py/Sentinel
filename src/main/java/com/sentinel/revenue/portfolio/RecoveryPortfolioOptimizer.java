package com.sentinel.revenue.portfolio;

import com.sentinel.revenue.opportunity.OpportunityAction;
import com.sentinel.revenue.opportunity.ProviderCapabilityRegistry;
import com.sentinel.revenue.opportunity.RecoveryRiskClass;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class RecoveryPortfolioOptimizer {
    private final ProviderCapabilityRegistry marketplace;

    public RecoveryPortfolioOptimizer(ProviderCapabilityRegistry marketplace) { this.marketplace = marketplace; }

    public PortfolioAllocation optimize(List<PortfolioCandidate> candidates, PortfolioConstraints constraints) {
        List<PortfolioCandidate> input = List.copyOf(candidates);
        Set<UUID> cases = new HashSet<>();
        input.forEach(candidate -> cases.add(candidate.incidentId()));
        long totalRisk = input.stream().mapToLong(PortfolioCandidate::amountMinor).sum();
        int policyBlocked = (int) input.stream().filter(candidate -> !candidate.policyAdmissible()).count();
        int governorBlocked = (int) input.stream().filter(candidate -> candidate.policyAdmissible()
                && !candidate.governorAdmissible()).count();
        int noAction = (int) input.stream().filter(candidate -> candidate.action() == OpportunityAction.NO_ACTION
                || candidate.expectedNetIncrementalValueMinor() == null
                || candidate.expectedNetIncrementalValueMinor().signum() <= 0).count();
        List<PortfolioCandidate> ranked = input.stream().filter(this::eligible)
                .sorted(Comparator.comparing(PortfolioCandidate::expectedNetIncrementalValueMinor).reversed()
                        .thenComparing(candidate -> candidate.incidentId().toString())
                        .thenComparing(candidate -> candidate.action().name())).toList();
        List<PortfolioCandidate> selected = new ArrayList<>();
        Set<UUID> selectedIncidents = new HashSet<>();
        long exposure = 0;
        int contacts = 0, reviews = 0, highRisk = 0;
        String binding = "NONE";
        for (PortfolioCandidate candidate : ranked) {
            if (selectedIncidents.contains(candidate.incidentId())) continue;
            if (exposure + candidate.amountMinor() > constraints.exposureBudgetMinor()) {
                binding = first(binding, "EXPOSURE_BUDGET"); continue;
            }
            if (candidate.customerContact() && contacts >= constraints.contactBudget()) {
                binding = first(binding, "CONTACT_BUDGET"); continue;
            }
            if (candidate.humanReview() && reviews >= constraints.humanReviewCapacity()) {
                binding = first(binding, "HUMAN_REVIEW_CAPACITY"); continue;
            }
            if (candidate.riskClass() == RecoveryRiskClass.HIGH
                    && highRisk >= constraints.maximumHighRiskActions()) {
                binding = first(binding, "HIGH_RISK_BUDGET"); continue;
            }
            selected.add(candidate); selectedIncidents.add(candidate.incidentId());
            exposure += candidate.amountMinor();
            if (candidate.customerContact()) contacts++;
            if (candidate.humanReview()) reviews++;
            if (candidate.riskClass() == RecoveryRiskClass.HIGH) highRisk++;
        }
        BigDecimal selectedValue = selected.stream().map(PortfolioCandidate::expectedNetIncrementalValueMinor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal eligibleValue = ranked.stream().map(PortfolioCandidate::expectedNetIncrementalValueMinor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PortfolioAllocation(cases.size(), totalRisk, selected, noAction, policyBlocked,
                governorBlocked, reviews, exposure, contacts, selectedValue,
                eligibleValue.subtract(selectedValue), binding, "ALLOCATION_PROPOSAL_ONLY");
    }

    private boolean eligible(PortfolioCandidate candidate) {
        if (!marketplace.supportedActions().contains(candidate.action())) return false;
        return candidate.action() != OpportunityAction.NO_ACTION && candidate.policyAdmissible()
                && candidate.governorAdmissible() && candidate.expectedNetIncrementalValueMinor() != null
                && candidate.expectedNetIncrementalValueMinor().signum() > 0;
    }
    private String first(String current, String value) { return "NONE".equals(current) ? value : current; }
}
