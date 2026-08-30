package com.sentinel.core.observability;

import com.sentinel.revenue.execution.RecoveryExecutionResponse;
import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.model.RecoveryStrategy;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.planning.RecoveryPlanningResult;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import com.sentinel.revenue.webhook.WebhookResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SentinelBusinessMetricsTest {
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
    private final SentinelBusinessMetrics metrics = new SentinelBusinessMetrics(meters, incidents);

    @Test
    void recordsIncidentPolicyExecutionWebhookAndDurationMetrics() throws Throwable {
        ProceedingJoinPoint detection = invocation(null);
        when(incidents.count()).thenReturn(4L, 6L);
        metrics.recordCreatedIncidents(detection);

        RecoveryPlanningResult planning = new RecoveryPlanningResult(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, PolicyDecision.AUTO,
                RecoveryActionStatus.AUTO_APPROVED, RevenueIncidentStatus.APPROVED, List.of(), "allowed");
        metrics.recordPolicyDecision(invocation(planning));

        RecoveryExecutionResponse execution = new RecoveryExecutionResponse(UUID.randomUUID(), UUID.randomUUID(),
                RecoveryActionStatus.EXECUTED, "plink_1", "reference", "https://example.invalid",
                "created", "TEST", false, "created");
        metrics.recordRecoveryExecution(invocation(execution));
        metrics.recordWebhook(invocation(new WebhookResult("evt_1", "DUPLICATE", true, "duplicate")));
        metrics.timeAgentInvestigation(invocation(new Object()));

        assertThat(count("sentinel.incidents.created")).isEqualTo(2.0);
        assertThat(count("sentinel.policy.approved")).isEqualTo(1.0);
        assertThat(count("sentinel.executions.attempted")).isEqualTo(1.0);
        assertThat(count("sentinel.executions.succeeded")).isEqualTo(1.0);
        assertThat(count("sentinel.webhooks.received")).isEqualTo(1.0);
        assertThat(count("sentinel.webhooks.duplicate")).isEqualTo(1.0);
        assertThat(meters.find("sentinel.agent.duration").timer()).isNotNull();
        assertThat(meters.find("sentinel.recovery.duration").timer()).isNotNull();
    }

    private ProceedingJoinPoint invocation(Object result) throws Throwable {
        ProceedingJoinPoint invocation = mock(ProceedingJoinPoint.class);
        when(invocation.proceed()).thenReturn(result);
        return invocation;
    }

    private double count(String name) {
        return meters.get(name).counter().count();
    }
}
