package com.sentinel.revenue.investigation;

import com.sentinel.core.agent.*;
import com.sentinel.revenue.model.RevenueIncident;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PaymentAnalystAgent implements SentinelAgent<RevenueIncident, AnalystFindings> {
    private final PatternAnalyzer patterns;
    private final CustomerContextTool customers;

    public PaymentAnalystAgent(PatternAnalyzer patterns, CustomerContextTool customers) {
        this.patterns = patterns;
        this.customers = customers;
    }

    @Override
    public AgentResult<AnalystFindings> execute(RevenueIncident incident, AgentContext context) {
        Instant started = Instant.now();
        PatternAnalysis pattern = patterns.analyze(incident);
        CustomerContext customer = customers.load(incident);
        List<String> lines = new ArrayList<>(pattern.evidence());
        lines.addAll(customer.evidence());
        double confidence = pattern.dominantFailureShare();
        AnalystFindings output = new AnalystFindings(pattern, customer, lines, confidence);
        List<Evidence> evidence = lines.stream().map(line -> new Evidence(
                "computed-payment-statistics", line, incident.getDetectedAt(), Map.of())).toList();
        return new AgentResult<>("PaymentAnalystAgent", lines.get(0), new Confidence(confidence),
                evidence, List.of(), started, Instant.now(), AgentStatus.SUCCEEDED, output);
    }
}
