package com.sentinel.revenue.metrics;

import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class RevenueMetricsService {
    public static final String LABEL = "Recovered Revenue — Test Mode / Synthetic Evaluation";
    private final RevenueIncidentRepository incidents;
    private final RecoveryActionRepository actions;
    private final RecoveryOutcomeRepository outcomes;
    private final RecoveryPlanRepository plans;
    public RevenueMetricsService(RevenueIncidentRepository incidents, RecoveryActionRepository actions,
                                 RecoveryOutcomeRepository outcomes, RecoveryPlanRepository plans) {
        this.incidents = incidents; this.actions = actions; this.outcomes = outcomes; this.plans = plans;
    }

    @Transactional(readOnly = true)
    public RevenueMetrics metrics() {
        long atRisk = incidents.findAll().stream().mapToLong(RevenueIncident::getAmountAtRiskMinor).sum();
        List<RecoveryAction> createdLinks = actions.findAll().stream()
                .filter(action -> action.getExternalResourceId() != null).toList();
        long attempted = createdLinks.stream().mapToLong(RecoveryAction::getAmountMinor).sum();
        List<RecoveryOutcome> currentOutcomes = outcomes.findAll();
        long recovered = currentOutcomes.stream().mapToLong(RecoveryOutcome::getRecoveredAmountMinor).sum();

        Map<UUID, RecoveryAction> actionById = new HashMap<>();
        createdLinks.forEach(action -> actionById.put(action.getId(), action));
        Map<RecoveryStrategy, long[]> grouped = new EnumMap<>(RecoveryStrategy.class);
        for (RecoveryAction action : createdLinks) {
            RecoveryStrategy strategy = plans.findById(action.getRecoveryPlanId()).orElseThrow().getStrategy();
            grouped.computeIfAbsent(strategy, ignored -> new long[2])[0] += action.getAmountMinor();
        }
        for (RecoveryOutcome outcome : currentOutcomes) {
            RecoveryAction action = actionById.get(outcome.getRecoveryActionId());
            if (action == null) continue;
            RecoveryStrategy strategy = plans.findById(action.getRecoveryPlanId()).orElseThrow().getStrategy();
            grouped.computeIfAbsent(strategy, ignored -> new long[2])[1] += outcome.getRecoveredAmountMinor();
        }
        List<StrategyPerformance> performance = grouped.entrySet().stream()
                .map(entry -> new StrategyPerformance(entry.getKey(), entry.getValue()[0], entry.getValue()[1],
                        rate(entry.getValue()[1], entry.getValue()[0])))
                .sorted(Comparator.comparing(metric -> metric.strategy().name())).toList();
        return new RevenueMetrics(LABEL, "TEST", atRisk, attempted, recovered,
                rate(recovered, attempted), performance);
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) return BigDecimal.ZERO.setScale(4);
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }
}
