package com.sentinel.revenue.investigation;

import com.sentinel.core.agent.*;
import com.sentinel.revenue.model.RevenueIncident;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class TriageAgent implements SentinelAgent<RevenueIncident, TriageResult> {
    @Override
    public AgentResult<TriageResult> execute(RevenueIncident incident, AgentContext context) {
        Instant started = Instant.now();
        String type = incident.getType().toUpperCase(Locale.ROOT);
        String category = type.contains("UPI") ? "PAYMENT_RAIL_DEGRADATION"
                : type.contains("PROVIDER") ? "PAYMENT_PROVIDER_OUTAGE" : "PAYMENT_FAILURE_CLUSTER";
        String strategy = type.contains("UPI") ? "Compare UPI issuer, error and retry patterns"
                : type.contains("PROVIDER") ? "Confirm provider-wide correlation and unaffected methods"
                : "Compare failure signature with rolling baseline";
        String evidenceLine = "Incident type %s, severity %s, %d payments and %d minor units at risk selected %s."
                .formatted(incident.getType(), incident.getSeverity(), incident.getAffectedPayments().size(),
                        incident.getAmountAtRiskMinor(), category);
        TriageResult output = new TriageResult(category, incident.getSeverity(), strategy,
                List.of("PatternAnalyzer", "CustomerContextTool", "HistoricalMemoryService"),
                List.of(evidenceLine), false);
        return new AgentResult<>("TriageAgent", strategy, new Confidence(1.0),
                List.of(new Evidence("incident", evidenceLine, incident.getDetectedAt(), Map.of(
                        "affectedPaymentCount", incident.getAffectedPayments().size(),
                        "amountAtRiskMinor", incident.getAmountAtRiskMinor()))), List.of(), started,
                Instant.now(), AgentStatus.SUCCEEDED, output);
    }
}
