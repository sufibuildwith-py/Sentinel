package com.sentinel.revenue.failurelab;

import com.sentinel.evaluation.EvaluationReport;
import com.sentinel.evaluation.EvaluationReportService;
import com.sentinel.revenue.execution.RazorpayGateway;
import com.sentinel.revenue.repository.ShadowDecisionDifferenceRepository;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FailureLabServiceTest {
    private final EvaluationReportService reports = mock(EvaluationReportService.class);
    private final ShadowDecisionDifferenceRepository differences = mock(ShadowDecisionDifferenceRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC);
    private final FailureLabService lab = new FailureLabService(reports, differences, clock);

    @Test
    void reusesExistingEvaluationHarnessAndTreatsCorrectRefusalAsSuccess() {
        EvaluationReport report = mock(EvaluationReport.class);
        EvaluationReport.FailureInjection injection = new EvaluationReport.FailureInjection(
                "Already-paid conflicting state", 4, "Mandatory stop pass denies execution", true,
                "ALREADY_PAID scenarios remain DENY");
        EvaluationReport.SafetyGate gate = new EvaluationReport.SafetyGate(
                "unsafe autonomous execution", 0, "= 0", true, "No unsafe execution");
        when(report.failureInjectionMatrix()).thenReturn(List.of(injection));
        when(report.scenarios()).thenReturn(List.of());
        when(report.safetyGates()).thenReturn(List.of(gate));
        when(reports.report()).thenReturn(report);

        FailureLabResult result = lab.run("policy-deny");

        assertThat(result.safetyDemonstrationPassed()).isTrue();
        assertThat(result.scenario().expectedSafetyOutcome()).isEqualTo("DENY");
        assertThat(result.evidence()).anyMatch(line -> line.contains("Mandatory stop"));
        verify(reports).report();
    }

    @Test
    void realProviderScenarioNeverSynthesizesAnOutcome() {
        FailureLabResult result = lab.run("accepted-not-recovered");

        assertThat(result.status()).isEqualTo("REQUIRES_REAL_PROVIDER_EVENT");
        assertThat(result.safetyDemonstrationPassed()).isFalse();
        assertThat(result.evidence()).contains("REAL RAZORPAY TEST MODE", "AWAITING RECONCILIATION");
        verifyNoInteractions(reports, differences);
    }

    @Test
    void failureLabHasNoProviderOrCustomerCommunicationToolPath() {
        assertThat(List.of(FailureLabService.class.getDeclaredFields()))
                .extracting(field -> field.getType().getName())
                .noneMatch(name -> name.contains("Razorpay") || name.contains("CommunicationAdapter")
                        || name.contains("RecoveryExecutionService") || name.contains("RecoveryWorker"));
        assertThat(RazorpayGateway.class).isNotNull();
    }
}
