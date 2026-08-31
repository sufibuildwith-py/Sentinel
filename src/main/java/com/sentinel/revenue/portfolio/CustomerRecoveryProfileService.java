package com.sentinel.revenue.portfolio;

import com.sentinel.revenue.communication.PromiseStatus;
import com.sentinel.revenue.economics.EconomicEvidenceQuality;
import com.sentinel.revenue.model.CustomerInteraction;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.model.PromiseToPay;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.repository.CustomerInteractionRepository;
import com.sentinel.revenue.repository.PaymentEventRepository;
import com.sentinel.revenue.repository.PromiseToPayRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CustomerRecoveryProfileService {
    public static final String FEATURE_SCHEMA_VERSION = "customer-recovery-profile-v1";
    private final RevenueIncidentRepository incidents;
    private final PaymentEventRepository payments;
    private final CustomerInteractionRepository interactions;
    private final PromiseToPayRepository promises;

    public CustomerRecoveryProfileService(RevenueIncidentRepository incidents, PaymentEventRepository payments,
                                          CustomerInteractionRepository interactions,
                                          PromiseToPayRepository promises) {
        this.incidents = incidents; this.payments = payments; this.interactions = interactions;
        this.promises = promises;
    }

    @Transactional(readOnly = true)
    public CustomerRecoveryProfile forIncident(UUID incidentId) {
        RevenueIncident incident = incidents.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Revenue incident not found: " + incidentId));
        List<PaymentEvent> paymentHistory = incident.getAffectedCustomers().stream()
                .flatMap(customer -> payments.findAllByCustomerIdOrderByTimestampAsc(customer).stream()).toList();
        List<CustomerInteraction> contactHistory = incident.getAffectedCustomers().stream()
                .flatMap(customer -> interactions.findAllByCustomerRefOrderByCreatedAtAsc(customer).stream()).toList();
        List<PromiseToPay> promiseHistory = incident.getAffectedCustomers().stream()
                .flatMap(customer -> promises.findAllByCustomerRef(customer).stream()).toList();
        List<PaymentEvent> successes = paymentHistory.stream().filter(this::successful).toList();
        String preferredRail = successes.stream().map(PaymentEvent::getMethod)
                .filter(Objects::nonNull).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream().max(Map.Entry.<String, Long>comparingByValue()
                        .thenComparing(Map.Entry::getKey)).map(Map.Entry::getKey).orElse("UNKNOWN");
        Map<String, Long> failureClasses = paymentHistory.stream().filter(event -> !successful(event))
                .map(event -> normalized(event.getErrorCode())).collect(Collectors.groupingBy(
                        Function.identity(), TreeMap::new, Collectors.counting()));
        Map<Integer, Long> hours = successes.stream().collect(Collectors.groupingBy(
                event -> event.getTimestamp().atZone(ZoneOffset.UTC).getHour(), TreeMap::new, Collectors.counting()));
        int kept = (int) promiseHistory.stream().filter(promise -> promise.getStatus() == PromiseStatus.KEPT).count();
        BigDecimal reliability = promiseHistory.isEmpty() ? null : BigDecimal.valueOf(kept)
                .divide(BigDecimal.valueOf(promiseHistory.size()), 4, RoundingMode.HALF_UP);
        EconomicEvidenceQuality quality = paymentHistory.isEmpty() && promiseHistory.isEmpty()
                ? EconomicEvidenceQuality.NOT_ESTIMATED : EconomicEvidenceQuality.OBSERVATIONAL;
        return new CustomerRecoveryProfile(incidentId, incident.getAffectedCustomers().size(), paymentHistory.size(),
                successes.size(), preferredRail, failureClasses, hours, contactHistory.size(),
                (int) contactHistory.stream().filter(item -> item.getDeliveryMode().startsWith("SIMULATED")).count(),
                promiseHistory.size(), kept, reliability, quality, FEATURE_SCHEMA_VERSION, definitions());
    }

    private List<CustomerRecoveryProfile.FeatureDefinition> definitions() {
        return List.of(
                new CustomerRecoveryProfile.FeatureDefinition("preferredPaymentRail", "payment_events",
                        "Explain supported rail preference", "merchant-configured payment retention",
                        "ranking only; never authorization"),
                new CustomerRecoveryProfile.FeatureDefinition("successfulUtcHours", "payment_events",
                        "Describe historical success timing", "merchant-configured payment retention",
                        "timing recommendation only"),
                new CustomerRecoveryProfile.FeatureDefinition("contactFrequency", "customer_interactions",
                        "Enforce fatigue constraints", "communication policy retention",
                        "deterministic contact policy"),
                new CustomerRecoveryProfile.FeatureDefinition("promiseReliability", "promises_to_pay",
                        "Describe provider-confirmed promise outcomes", "promise lifecycle retention",
                        "human review and ranking only"));
    }

    private boolean successful(PaymentEvent event) {
        return "SUCCESS".equalsIgnoreCase(event.getStatus()) || "CAPTURED".equalsIgnoreCase(event.getStatus())
                || "PAID".equalsIgnoreCase(event.getStatus());
    }
    private String normalized(String value) { return value == null || value.isBlank() ? "UNKNOWN" : value; }
}
