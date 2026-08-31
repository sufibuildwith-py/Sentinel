package com.sentinel.revenue.controltower;

import com.sentinel.revenue.governor.*;
import com.sentinel.revenue.health.PaymentHealthAnalyzer;
import com.sentinel.revenue.health.PaymentHealthReport;
import com.sentinel.revenue.metrics.*;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ControlTowerServiceTest {
    @Test
    void aggregatesOnlySanitizedOperationalStateWithExplicitTruthLabels() {
        Instant now = Instant.parse("2026-09-01T00:00:00Z");
        PaymentHealthAnalyzer health = mock(PaymentHealthAnalyzer.class);
        FinancialAttributionService financial = mock(FinancialAttributionService.class);
        SystemicRecoveryIncidentRepository systemic = mock(SystemicRecoveryIncidentRepository.class);
        RecoveryOpportunityLogRepository opportunities = mock(RecoveryOpportunityLogRepository.class);
        KillSwitchService switches = mock(KillSwitchService.class);
        RecoveryBatchRepository batches = mock(RecoveryBatchRepository.class);
        RegisteredModelRepository models = mock(RegisteredModelRepository.class);
        PolicyReplaySnapshotRepository snapshots = mock(PolicyReplaySnapshotRepository.class);
        ShadowDecisionDifferenceRepository differences = mock(ShadowDecisionDifferenceRepository.class);
        PromiseToPayRepository promises = mock(PromiseToPayRepository.class);
        RecoverySafetyProperties safety = new RecoverySafetyProperties(10_000, 10, 2_000,
                20, 5, 3, 2, .25, 4_000, 2, 1);
        PaymentHealthReport report = new PaymentHealthReport("ALL", now, Map.of(), Map.of(), List.of());
        FinancialAttribution attribution = new FinancialAttribution("TEST", 0, 0, 0, 0,
                "NOT_ESTIMATED", 0, 0, 0, 0, 0, 0, "NOT_CONFIGURED", 0,
                new FinancialAttribution.OperationalTimings(metric(), metric(), metric(), metric()));
        when(health.analyze("ALL", now)).thenReturn(report);
        when(financial.attribution()).thenReturn(attribution);
        when(systemic.findAll()).thenReturn(List.of());
        when(opportunities.findAll()).thenReturn(List.of());
        when(switches.states()).thenReturn(Map.of(KillSwitch.ALL_AUTONOMOUS_EXECUTION, false));
        when(batches.findAll()).thenReturn(List.of());
        when(models.findAll()).thenReturn(List.of());
        when(snapshots.count()).thenReturn(3L);
        when(differences.findAll()).thenReturn(List.of());
        when(promises.findAll()).thenReturn(List.of());

        ControlTowerView view = new ControlTowerService(health, financial, systemic, opportunities,
                switches, safety, batches, models, snapshots, differences, promises,
                Clock.fixed(now, ZoneOffset.UTC)).view();

        assertThat(view.generatedAt()).isEqualTo(now);
        assertThat(view.truthLabels()).contains("SHADOW ONLY", "PROVIDER CONFIRMED", "AWAITING RECONCILIATION");
        assertThat(view.replayAndShadow().snapshotCount()).isEqualTo(3);
        assertThat(ControlTowerView.class.getRecordComponents()).extracting(component -> component.getName())
                .doesNotContain("customerRef", "rawPayload", "webhookPayload");
    }

    private FinancialAttribution.TimingMetric metric() {
        return new FinancialAttribution.TimingMetric(null, 0, "not measured");
    }
}
