package com.sentinel.revenue.service;

import com.sentinel.revenue.api.EvidenceCapsuleView;
import com.sentinel.revenue.audit.AuditTrailService;
import com.sentinel.revenue.execution.RecoveryTruthResolver;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.WebhookEvent;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvidenceCapsuleServiceTest {
    @Test
    void capsuleExposesSafeWebhookMetadataButNeverRawPayloadOrSignature() {
        UUID incidentId = UUID.randomUUID();
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        IncidentFindingRepository findings = mock(IncidentFindingRepository.class);
        AgentClaimRepository claims = mock(AgentClaimRepository.class);
        WebhookEventRepository webhooks = mock(WebhookEventRepository.class);
        RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        RecoveryOutcomeRepository outcomes = mock(RecoveryOutcomeRepository.class);
        AuditTrailService audit = mock(AuditTrailService.class);
        RevenueIncident incident = mock(RevenueIncident.class);
        WebhookEvent event = new WebhookEvent("evt_safe", "payment.captured",
                "{\"customer_email\":\"secret@example.com\"}", "secret-signature",
                true, true, incidentId, Instant.parse("2026-08-31T10:00:00Z"),
                Instant.parse("2026-08-31T10:00:01Z"));

        when(incidents.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incident.getRootCause()).thenReturn("Provider outage");
        when(findings.findAllByIncidentIncidentId(incidentId)).thenReturn(List.of());
        when(claims.findAllByIncidentIncidentIdOrderByCreatedAtAsc(incidentId)).thenReturn(List.of());
        when(webhooks.findAllByIncidentIdOrderByReceivedAtAsc(incidentId)).thenReturn(List.of(event));
        when(actions.findFirstByIncidentIncidentIdOrderByCreatedAtDesc(incidentId)).thenReturn(Optional.empty());
        when(audit.trail(incidentId)).thenReturn(List.of());

        EvidenceCapsuleView capsule = new EvidenceCapsuleService(incidents, findings, claims,
                webhooks, actions, outcomes, audit, new RecoveryTruthResolver()).assemble(incidentId);

        assertThat(capsule.webhooks()).singleElement().satisfies(safe -> {
            assertThat(safe.eventId()).isEqualTo("evt_safe");
            assertThat(safe.verified()).isTrue();
        });
        assertThat(capsule.toString()).doesNotContain("secret@example.com", "secret-signature");
        assertThat(capsule.completeness().missingStages()).contains("FINAL_OUTCOME");
    }
}
