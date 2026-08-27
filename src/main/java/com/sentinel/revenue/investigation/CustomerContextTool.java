package com.sentinel.revenue.investigation;

import com.sentinel.revenue.model.HistoricalIncident;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.repository.HistoricalIncidentRepository;
import com.sentinel.revenue.repository.PaymentEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CustomerContextTool {
    private final PaymentEventRepository payments;
    private final HistoricalIncidentRepository historicalIncidents;

    public CustomerContextTool(PaymentEventRepository payments,
                               HistoricalIncidentRepository historicalIncidents) {
        this.payments = payments;
        this.historicalIncidents = historicalIncidents;
    }

    @Transactional(readOnly = true)
    public CustomerContext load(RevenueIncident incident) {
        List<PaymentEvent> events = payments.findAllByPaymentIdIn(incident.getAffectedPayments());
        int retryCount = events.stream().mapToInt(event -> Math.max(event.getAttemptNumber() - 1,
                event.getPreviousFailureCount())).sum();
        Map<String, Long> previousMethods = events.stream()
                .map(PaymentEvent::getPreviousSuccessfulMethod).filter(Objects::nonNull)
                .filter(method -> !method.isBlank())
                .collect(Collectors.groupingBy(String::toUpperCase, Collectors.counting()));
        List<HistoricalIncident> history = historicalIncidents.findAll();
        long recovered = history.stream().mapToLong(HistoricalIncident::getRecoveredAmountMinor).sum();
        long atRisk = history.stream().mapToLong(CustomerContextTool::amountAtRisk).sum();
        Double recoveryRate = atRisk == 0 ? null : (double) recovered / atRisk;

        List<String> evidence = new ArrayList<>();
        evidence.add("%d affected customers generated %d prior/repeated attempts."
                .formatted(incident.getAffectedCustomers().size(), retryCount));
        evidence.add(previousMethods.isEmpty()
                ? "No prior successful payment method is recorded for this cohort."
                : "Prior successful methods from stored payment events: " + previousMethods + ".");
        evidence.add(recoveryRate == null
                ? "Historical recovery rate is unavailable because no completed history has an amount-at-risk denominator."
                : "Historical recovery is %s (%d/%d minor units) across %d stored incidents."
                .formatted(percent(recoveryRate), recovered, atRisk, history.size()));
        return new CustomerContext(incident.getAffectedCustomers().size(), retryCount,
                previousMethods, history.size(), recoveryRate, evidence);
    }

    static long amountAtRisk(HistoricalIncident incident) {
        Object value = incident.getEvidenceSummary().get("amountAtRiskMinor");
        if (value instanceof Number number) return Math.max(0, number.longValue());
        if (value instanceof String text) {
            try { return Math.max(0, Long.parseLong(text)); } catch (NumberFormatException ignored) { return 0; }
        }
        return 0;
    }
    private String percent(double value) { return BigDecimal.valueOf(value * 100).setScale(1, RoundingMode.HALF_UP).toPlainString() + "%"; }
}
