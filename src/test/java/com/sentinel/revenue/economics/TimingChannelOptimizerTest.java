package com.sentinel.revenue.economics;

import com.sentinel.revenue.health.*;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.opportunity.CausalMaturity;
import com.sentinel.revenue.opportunity.OpportunityAction;
import com.sentinel.revenue.opportunity.ProviderCapabilityRegistry;
import com.sentinel.revenue.repository.CustomerContactPreferenceRepository;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TimingChannelOptimizerTest {
    @Test
    void activeProviderDowntimeDeterministicallyRecommendsWaitWithoutToolExecution() {
        PaymentHealthAnalyzer health = mock(PaymentHealthAnalyzer.class);
        CounterfactualRecoveryEngine counterfactuals = mock(CounterfactualRecoveryEngine.class);
        Instant now = Instant.parse("2026-08-31T06:00:00Z");
        when(health.analyze("merchant-1", now)).thenReturn(report(now, true));
        TimingChannelOptimizer optimizer = new TimingChannelOptimizer(health, counterfactuals,
                new ProviderCapabilityRegistry(), mock(CustomerContactPreferenceRepository.class),
                Clock.fixed(now, ZoneOffset.UTC));

        TimingChannelRecommendation result = optimizer.recommend(mock(RevenueIncident.class),
                "merchant-1", CausalMaturity.M1);

        assertThat(result.action()).isEqualTo(OpportunityAction.WAIT_FOR_DOWNTIME_RECOVERY);
        assertThat(result.authorityState()).isEqualTo("SHADOW_RECOMMENDATION_ONLY");
        assertThat(result.channel()).isEqualTo("NONE");
        verifyNoInteractions(counterfactuals);
    }

    private PaymentHealthReport report(Instant now, boolean downtime) {
        PaymentHealthWindow current = new PaymentHealthWindow(Duration.ofMinutes(15), 10, 2,
                10_000, 0.8, 0.1, Map.of("UPI", 0.8), Map.of(), Map.of(), Map.of(), Map.of());
        PaymentHealthWindow baseline = new PaymentHealthWindow(Duration.ofHours(24), 100, 10,
                100_000, 0.9, 0.01, Map.of("UPI", 0.9), Map.of(), Map.of(), Map.of(), Map.of());
        PaymentHealthSignal signal = new PaymentHealthSignal(PaymentHealthSignalType.PROVIDER_DOWNTIME_SIGNAL,
                downtime, downtime ? 1 : 0, 0, 1, "provider", List.of("test evidence"));
        return new PaymentHealthReport("merchant-1", now, Map.of("15m", current),
                Map.of("24h", baseline), List.of(signal));
    }
}
