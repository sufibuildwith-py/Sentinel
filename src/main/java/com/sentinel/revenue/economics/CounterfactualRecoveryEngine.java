package com.sentinel.revenue.economics;

import com.sentinel.revenue.model.HistoricalIncident;
import com.sentinel.revenue.model.RecoveryOutcomeStatus;
import com.sentinel.revenue.model.RecoveryStrategy;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.opportunity.CausalMaturity;
import com.sentinel.revenue.opportunity.OpportunityAction;
import com.sentinel.revenue.repository.HistoricalIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class CounterfactualRecoveryEngine {
    public static final String MODEL_VERSION = "empirical-counterfactual-v1";
    private final HistoricalIncidentRepository history;

    public CounterfactualRecoveryEngine(HistoricalIncidentRepository history) { this.history = history; }

    @Transactional(readOnly = true)
    public CounterfactualEstimate estimate(RevenueIncident incident, OpportunityAction action,
                                           CausalMaturity maturity) {
        if (maturity == CausalMaturity.M0) return unavailable(action, maturity);
        List<HistoricalIncident> all = history.findAll();
        List<HistoricalIncident> natural = comparable(all, RecoveryStrategy.NO_ACTION);
        RecoveryStrategy strategy = strategy(action);
        List<HistoricalIncident> treated = strategy == null ? List.of() : comparable(all, strategy);
        BigDecimal naturalRate = rate(natural);
        BigDecimal actionRate = action == OpportunityAction.NO_ACTION ? naturalRate : rate(treated);
        Instant start = all.stream().map(HistoricalIncident::getCreatedAt).min(Comparator.naturalOrder()).orElse(null);
        Instant end = all.stream().map(HistoricalIncident::getCreatedAt).max(Comparator.naturalOrder()).orElse(null);
        return new CounterfactualEstimate(action, maturity, naturalRate, actionRate,
                null, null, null, null, null,
                interval(natural), action == OpportunityAction.NO_ACTION ? interval(natural) : interval(treated),
                "OBSERVATIONAL_RESOLVED_FREQUENCY_NOT_CAUSAL", EconomicEvidenceQuality.OBSERVATIONAL,
                MODEL_VERSION, natural.size(), action == OpportunityAction.NO_ACTION ? natural.size() : treated.size(),
                start, end, true);
    }

    private CounterfactualEstimate unavailable(OpportunityAction action, CausalMaturity maturity) {
        return new CounterfactualEstimate(action, maturity, null, null, null, null, null, null,
                null, null, null, "NOT_ESTIMATED_INSUFFICIENT_DATA", EconomicEvidenceQuality.NOT_ESTIMATED,
                MODEL_VERSION, 0, 0, null, null, true);
    }

    private List<HistoricalIncident> comparable(List<HistoricalIncident> all, RecoveryStrategy strategy) {
        return all.stream().filter(item -> item.getRecoveryStrategy() == strategy).toList();
    }

    private BigDecimal rate(List<HistoricalIncident> sample) {
        if (sample.isEmpty()) return null;
        long successes = sample.stream().filter(this::recovered).count();
        return BigDecimal.valueOf(successes).divide(BigDecimal.valueOf(sample.size()), 4, RoundingMode.HALF_UP);
    }

    private ProbabilityInterval interval(List<HistoricalIncident> sample) {
        if (sample.isEmpty()) return null;
        long successes = sample.stream().filter(this::recovered).count();
        double n = sample.size();
        double p = successes / n;
        double z = 1.959963984540054;
        double denominator = 1 + z * z / n;
        double center = (p + z * z / (2 * n)) / denominator;
        double margin = z * Math.sqrt((p * (1 - p) + z * z / (4 * n)) / n) / denominator;
        return new ProbabilityInterval(decimal(Math.max(0, center - margin)),
                decimal(Math.min(1, center + margin)), "WILSON_95_PERCENT");
    }

    private BigDecimal decimal(double value) { return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP); }
    private boolean recovered(HistoricalIncident item) { return item.getOutcome() == RecoveryOutcomeStatus.RECOVERED; }

    private RecoveryStrategy strategy(OpportunityAction action) { return switch (action) {
        case CREATE_PAYMENT_LINK, REQUEST_ALTERNATE_METHOD -> RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK;
        case WAIT_FOR_DOWNTIME_RECOVERY -> RecoveryStrategy.WAIT_FOR_PROVIDER;
        case CUSTOMER_OUTREACH -> RecoveryStrategy.RECOVERY_REMINDER;
        case HUMAN_ESCALATION -> RecoveryStrategy.HUMAN_ESCALATION;
        case NO_ACTION -> RecoveryStrategy.NO_ACTION;
        case CREATE_NEW_ORDER -> null;
    }; }
}
