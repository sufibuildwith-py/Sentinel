package com.sentinel.revenue.audit;

import com.sentinel.revenue.model.AuditEvent;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.repository.AuditEventRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuditTrailServiceTest {
    @Test
    void reconstructsReadableChronologyUsingAuditTableAlone() {
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        AuditEventRepository audits = mock(AuditEventRepository.class);
        UUID id = UUID.randomUUID();
        RevenueIncident incident = new RevenueIncident("UPI_DEGRADATION", RevenueIncidentStatus.STOPPED,
                "HIGH", 50_000, Instant.now(), List.of("p1"), List.of("c1"), List.of(), null, null);
        ReflectionTestUtils.setField(incident, "incidentId", id);
        AuditEvent detected = event(incident, "DETECTOR", "INCIDENT_DETECTED",
                "UPI degradation detected", List.of(), "DETECTED");
        AuditEvent diagnosed = event(incident, "ROOT_CAUSE_AGENT", "AGENT_RESULT",
                "UPI issuer degradation confidence 0.91", List.of(), null);
        AuditEvent denied = event(incident, null, "POLICY_DECISION",
                "Mandatory duplicate-charge stop", List.of(
                        "FAIL NO_DUPLICATE_CHARGE_RISK: actual=true; required == false"), "DENY");
        when(incidents.existsById(id)).thenReturn(true);
        when(audits.findTrail(id)).thenReturn(List.of(detected, diagnosed, denied));

        List<AuditTrailEntry> trail = new AuditTrailService(incidents, audits).trail(id);

        assertThat(trail).extracting(AuditTrailEntry::stage)
                .containsExactly("DETECTOR", "ROOT_CAUSE_AGENT", "POLICY_DECISION");
        assertThat(trail.get(2).policyResult()).isEqualTo("DENY");
        assertThat(trail.get(2).ruleTrace()).singleElement()
                .asString().contains("NO_DUPLICATE_CHARGE_RISK", "actual=true");
        verify(incidents).existsById(id);
        verifyNoMoreInteractions(incidents);
    }

    private AuditEvent event(RevenueIncident incident, String agent, String action,
                             String decision, List<String> rules, String policyResult) {
        AuditEvent event = new AuditEvent(incident, Instant.now(), "SENTINEL", agent, action,
                List.of(decision), null, decision, rules, policyResult, null,
                null, null, decision);
        ReflectionTestUtils.setField(event, "eventId", UUID.randomUUID());
        return event;
    }
}
