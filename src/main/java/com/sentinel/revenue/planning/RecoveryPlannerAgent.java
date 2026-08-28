package com.sentinel.revenue.planning;

import com.sentinel.core.agent.*;
import com.sentinel.revenue.investigation.CustomerContext;
import com.sentinel.revenue.investigation.CustomerContextTool;
import com.sentinel.revenue.investigation.HistoricalMemoryService;
import com.sentinel.revenue.investigation.SimilarHistoricalIncident;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.IncidentFindingRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RecoveryPlannerAgent implements SentinelAgent<RevenueIncident, RecoveryPlan> {
    private final IncidentFindingRepository findings;
    private final CustomerContextTool customerContext;
    private final HistoricalMemoryService memory;

    public RecoveryPlannerAgent(IncidentFindingRepository findings, CustomerContextTool customerContext,
                                HistoricalMemoryService memory) {
        this.findings = findings;
        this.customerContext = customerContext;
        this.memory = memory;
    }

    @Override
    public AgentResult<RecoveryPlan> execute(RevenueIncident incident, AgentContext context) {
        Instant started = Instant.now();
        IncidentFinding rootFinding = findings.findAllByIncidentIncidentId(incident.getIncidentId()).stream()
                .filter(finding -> finding.getSource() == FindingSource.ROOT_CAUSE_AGENT)
                .max(Comparator.comparing(IncidentFinding::getCreatedAt))
                .orElseThrow(() -> new IllegalStateException("A persisted root-cause finding is required"));
        if (rootFinding.getConfidence() == null) {
            throw new IllegalStateException("Root-cause confidence is required for planning");
        }
        double confidence = rootFinding.getConfidence().doubleValue();
        CustomerContext customers = customerContext.load(incident);
        List<SimilarHistoricalIncident> matches = memory.findSimilar(incident);
        StrategyPerformance best = strategyPerformance(matches).stream()
                .max(Comparator.comparingDouble(StrategyPerformance::recoveryRate)).orElse(null);

        RecoveryStrategy strategy = strategyFor(rootFinding.getSummary(), incident.getType(), customers);
        if (best != null && best.recoveryRate() >= 0.50) strategy = best.strategy();
        double estimatedRate = best == null ? confidence : Math.min(1.0, best.recoveryRate());
        long estimated = Math.round(incident.getAmountAtRiskMinor() * estimatedRate);
        RiskLevel risk = confidence < 0.60 || incident.getAmountAtRiskMinor() > 500_000
                ? RiskLevel.HIGH : incident.getAmountAtRiskMinor() > 100_000 ? RiskLevel.MEDIUM : RiskLevel.LOW;

        List<String> evidence = new ArrayList<>();
        evidence.add("Root-cause finding '%s' has persisted confidence %.4f."
                .formatted(rootFinding.getSummary(), confidence));
        evidence.addAll(customers.evidence());
        if (best == null) {
            evidence.add("No historical recovery-rate match exists; estimate uses root-cause confidence %.4f."
                    .formatted(confidence));
        } else {
            evidence.add("%d similar incidents using %s recovered %d/%d minor units (%.1f%%; mean similarity %.4f)."
                    .formatted(best.incidentCount(), best.strategy(), best.recoveredMinor(), best.atRiskMinor(),
                            best.recoveryRate() * 100, best.meanSimilarity()));
        }
        String reason = "%s proposed from persisted diagnosis, customer context, and %s."
                .formatted(strategy, best == null ? "diagnostic confidence" : "historical outcomes");
        RecoveryPlan plan = new RecoveryPlan(incident, strategy, reason,
                incident.getAffectedPayments().size(), incident.getAmountAtRiskMinor(),
                BigDecimal.valueOf(confidence).setScale(4, RoundingMode.HALF_UP), estimated, risk,
                Instant.now());
        List<Evidence> agentEvidence = evidence.stream().map(line -> new Evidence(
                "persisted-revenue-evidence", line, incident.getDetectedAt(), Map.of())).toList();
        return new AgentResult<>("RecoveryPlannerAgent", reason, new Confidence(confidence),
                agentEvidence, List.of(), started, Instant.now(), AgentStatus.SUCCEEDED, plan);
    }

    private RecoveryStrategy strategyFor(String rootCause, String incidentType, CustomerContext customers) {
        String signal = (rootCause + " " + incidentType).toUpperCase();
        if (signal.contains("UPI") || signal.contains("ISSUER")) return RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK;
        if (signal.contains("PROVIDER") || signal.contains("OUTAGE")) return RecoveryStrategy.WAIT_FOR_PROVIDER;
        if (customers.retryCount() > 0) return RecoveryStrategy.DEFERRED_RETRY;
        return RecoveryStrategy.RECOVERY_REMINDER;
    }

    private List<StrategyPerformance> strategyPerformance(List<SimilarHistoricalIncident> matches) {
        return matches.stream().filter(match -> match.recoveryRate() != null && match.recoveryRate() > 0)
                .collect(Collectors.groupingBy(SimilarHistoricalIncident::strategy))
                .entrySet().stream().map(entry -> {
                    long recovered = entry.getValue().stream()
                            .mapToLong(SimilarHistoricalIncident::recoveredAmountMinor).sum();
                    long atRisk = entry.getValue().stream().mapToLong(match ->
                            Math.round(match.recoveredAmountMinor() / match.recoveryRate())).sum();
                    double rate = atRisk == 0 ? 0.0 : (double) recovered / atRisk;
                    double similarity = entry.getValue().stream()
                            .mapToDouble(SimilarHistoricalIncident::similarity).average().orElse(0.0);
                    return new StrategyPerformance(entry.getKey(), entry.getValue().size(), recovered,
                            atRisk, rate, similarity);
                }).toList();
    }

    private record StrategyPerformance(RecoveryStrategy strategy, int incidentCount,
                                       long recoveredMinor, long atRiskMinor,
                                       double recoveryRate, double meanSimilarity) {}
}
