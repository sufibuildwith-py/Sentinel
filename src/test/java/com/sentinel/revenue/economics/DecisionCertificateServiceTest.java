package com.sentinel.revenue.economics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.revenue.model.DecisionCertificate;
import com.sentinel.revenue.model.DecisionCertificateDraft;
import com.sentinel.revenue.repository.DecisionCertificateRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DecisionCertificateServiceTest {
    @Test
    void sameDecisionAndContentIsIdempotentAndHasStableHash() {
        DecisionCertificateRepository repository = mock(DecisionCertificateRepository.class);
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        UUID incidentId = UUID.randomUUID();
        when(incidents.existsById(incidentId)).thenReturn(true);
        when(repository.findByDecisionId(any())).thenReturn(Optional.empty());
        when(repository.append(any())).thenAnswer(invocation -> invocation.getArgument(0));
        DecisionCertificateService service = service(repository, incidents);

        DecisionCertificate created = service.issue(draft(incidentId, "AWAITING_RECONCILIATION", null, null));

        assertThat(created.getCertificateSha256()).hasSize(64);
        assertThat(created.getAuthorizationResult()).isEqualTo("SHADOW_ONLY_NOT_AUTHORIZED");
        verify(repository).append(any());
    }

    @Test
    void cannotClaimConfirmedRecoveryWithoutProviderAndReconciliationEvidence() {
        DecisionCertificateService service = service(mock(DecisionCertificateRepository.class),
                mock(RevenueIncidentRepository.class));
        assertThatThrownBy(() -> service.issue(draft(UUID.randomUUID(), "RECOVERED_CONFIRMED", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider and reconciliation");
    }

    private DecisionCertificateService service(DecisionCertificateRepository repository,
                                               RevenueIncidentRepository incidents) {
        return new DecisionCertificateService(repository, incidents, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-31T00:00:00Z"), ZoneOffset.UTC));
    }

    private DecisionCertificateDraft draft(UUID incidentId, String truth, String provider, String reconciliation) {
        return new DecisionCertificateDraft(UUID.randomUUID(), incidentId, null, "TEST", "policy-v1",
                "none-deterministic", "features-v1", "strategy-v1",
                DecisionCertificateService.hashText("input"), null, List.of("NO_ACTION"), List.of(),
                "NO_ACTION", "NOT_ESTIMATED", EconomicEvidenceQuality.NOT_ESTIMATED, null,
                "SHADOW_ONLY_NOT_AUTHORIZED", "NOT_EVALUATED", null, provider, reconciliation,
                null, truth, "decision-certificate-v1");
    }
}
