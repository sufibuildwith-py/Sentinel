package com.sentinel.revenue.planning;

import com.sentinel.core.agent.AgentContext;
import com.sentinel.revenue.investigation.*;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.IncidentFindingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RecoveryPlannerAgentTest {
    @Test
    void usesComputedHistoricalStrategyRecoveryRateInProposal() {
        IncidentFindingRepository findings = mock(IncidentFindingRepository.class);
        CustomerContextTool customers = mock(CustomerContextTool.class);
        HistoricalMemoryService memory = mock(HistoricalMemoryService.class);
        RevenueIncident incident = new RevenueIncident("UPI_DEGRADATION", RevenueIncidentStatus.DIAGNOSED,
                "MEDIUM", 100_000, Instant.now(), List.of("p1"), List.of("c1"), List.of(), null, null);
        ReflectionTestUtils.setField(incident, "incidentId", UUID.randomUUID());
        IncidentFinding root = new IncidentFinding(incident, FindingSource.ROOT_CAUSE_AGENT,
                "UPI issuer degradation", new BigDecimal("0.9100"), List.of("UPI concentration"), Instant.now());
        when(findings.findAllByIncidentIncidentId(incident.getIncidentId())).thenReturn(List.of(root));
        when(customers.load(incident)).thenReturn(new CustomerContext(1, 0, Map.of(), 2,
                0.723, List.of("Historical recovery is 72.3%.")));
        when(memory.findSimilar(incident)).thenReturn(List.of(
                new SimilarHistoricalIncident(UUID.randomUUID(), "issuer timeout",
                        RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, RecoveryOutcomeStatus.RECOVERED,
                        36_150, 0.723, 0.91),
                new SimilarHistoricalIncident(UUID.randomUUID(), "issuer unavailable",
                        RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, RecoveryOutcomeStatus.RECOVERED,
                        36_150, 0.723, 0.88)));
        Instant now = Instant.now();

        RecoveryPlan plan = new RecoveryPlannerAgent(findings, customers, memory)
                .execute(incident, new AgentContext("plan", now, now.plusSeconds(30), Map.of()))
                .output();

        assertThat(plan.getStrategy()).isEqualTo(RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK);
        assertThat(plan.getEstimatedRecoveryMinor()).isEqualTo(72_300);
        assertThat(plan.getConfidence()).isEqualByComparingTo("0.9100");
        assertThat(plan.getReason()).contains("historical outcomes");
        assertThat(plan.getRiskLevel()).isEqualTo(RiskLevel.LOW);
    }
}
