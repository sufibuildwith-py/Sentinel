package com.sentinel.revenue.economics;

import com.sentinel.revenue.health.PaymentHealthAnalyzer;
import com.sentinel.revenue.health.PaymentHealthReport;
import com.sentinel.revenue.health.PaymentHealthSignalType;
import com.sentinel.revenue.model.CustomerContactPreference;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.opportunity.CausalMaturity;
import com.sentinel.revenue.opportunity.OpportunityAction;
import com.sentinel.revenue.opportunity.ProviderCapabilityRegistry;
import com.sentinel.revenue.repository.CustomerContactPreferenceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TimingChannelOptimizer {
    private final PaymentHealthAnalyzer health;
    private final CounterfactualRecoveryEngine counterfactuals;
    private final ProviderCapabilityRegistry marketplace;
    private final CustomerContactPreferenceRepository preferences;
    private final Clock clock;

    public TimingChannelOptimizer(PaymentHealthAnalyzer health, CounterfactualRecoveryEngine counterfactuals,
                                  ProviderCapabilityRegistry marketplace,
                                  CustomerContactPreferenceRepository preferences, Clock clock) {
        this.health = health; this.counterfactuals = counterfactuals; this.marketplace = marketplace;
        this.preferences = preferences; this.clock = clock;
    }

    public TimingChannelRecommendation recommend(RevenueIncident incident, String merchantId,
                                                 CausalMaturity maturity) {
        Instant now = clock.instant();
        PaymentHealthReport report = health.analyze(merchantId, now);
        boolean downtime = report.signals().stream().anyMatch(signal -> signal.active()
                && signal.type() == PaymentHealthSignalType.PROVIDER_DOWNTIME_SIGNAL);
        List<String> explanation = new ArrayList<>();
        explanation.add("Provider health evaluated at " + report.evaluatedAt());
        if (downtime) {
            explanation.add("Active provider downtime suppresses immediate recovery execution.");
            return new TimingChannelRecommendation(OpportunityAction.WAIT_FOR_DOWNTIME_RECOVERY,
                    now.plus(Duration.ofMinutes(30)), "NONE", "ACTIVE_DEGRADATION_RECHECK_REQUIRED",
                    "NOT_APPLICABLE", "SHADOW_RECOMMENDATION_ONLY", EconomicEvidenceQuality.DETERMINISTIC,
                    "PROVIDER_DOWNTIME_GUARD", explanation);
        }

        List<CounterfactualEstimate> estimates = marketplace.supportedActions().stream()
                .map(action -> counterfactuals.estimate(incident, action, maturity)).toList();
        CounterfactualEstimate selected = estimates.stream()
                .filter(item -> item.actionRecoveryProbability() != null)
                .max(Comparator.comparing(CounterfactualEstimate::actionRecoveryProbability)
                        .thenComparing(item -> item.action().name()))
                .orElseGet(() -> estimates.stream().filter(item -> item.action() == OpportunityAction.NO_ACTION)
                        .findFirst().orElseThrow());
        String channel = "NONE";
        String eligibility = "NOT_APPLICABLE";
        if (selected.action() == OpportunityAction.CUSTOMER_OUTREACH) {
            CustomerContactPreference preference = incident.getAffectedCustomers().stream().findFirst()
                    .flatMap(preferences::findById).orElse(null);
            boolean allowed = preference != null && preference.isConsentGranted()
                    && !preference.isDoNotContact() && !preference.isOptedOut();
            if (!allowed) {
                selected = estimates.stream().filter(item -> item.action() == OpportunityAction.NO_ACTION)
                        .findFirst().orElseThrow();
                eligibility = "BLOCKED_BY_CONTACT_PREFERENCE";
                explanation.add("Customer outreach removed by deterministic consent/DNC/opt-out checks.");
            } else {
                channel = "SIMULATED_COMMUNICATION";
                eligibility = "CONSENT_ELIGIBLE_SIMULATION_ONLY";
            }
        }
        BigDecimal current = BigDecimal.valueOf(report.current().get("15m").successRate());
        BigDecimal baseline = BigDecimal.valueOf(report.baseline().get("24h").successRate());
        String window = current.compareTo(baseline) >= 0 ? "PROVIDER_RECOVERY_WINDOW_OPEN" : "PROVIDER_BELOW_BASELINE";
        explanation.add("Current success rate=" + current + ", 24h baseline=" + baseline);
        explanation.add("Ranking evidence=" + selected.evidenceQuality() + "; no execution authority granted.");
        return new TimingChannelRecommendation(selected.action(), now, channel, window, eligibility,
                "SHADOW_RECOMMENDATION_ONLY", selected.evidenceQuality(), selected.method(), explanation);
    }
}
