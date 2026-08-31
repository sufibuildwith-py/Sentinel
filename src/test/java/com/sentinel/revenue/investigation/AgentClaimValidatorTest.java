package com.sentinel.revenue.investigation;

import com.sentinel.revenue.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentClaimValidatorTest {
    private final AgentClaimValidator validator = new AgentClaimValidator();
    private final Instant now = Instant.parse("2026-08-31T10:00:00Z");

    @Test
    void rejectsUnknownEvidenceReference() {
        UUID unknown = UUID.randomUUID();
        ClaimValidationResult result = validator.validate(claim(ClaimType.DIAGNOSIS,
                        List.of(unknown), new BigDecimal("0.8000"), null),
                new ClaimValidationContext(List.of(), null, null, now));

        assertThat(result.status()).isEqualTo(ClaimValidationStatus.REJECTED);
        assertThat(result.errors()).contains("UNKNOWN_EVIDENCE_REFERENCE:" + unknown);
    }

    @Test
    void staleEvidenceDowngradesConfidence() {
        UUID id = UUID.randomUUID();
        IncidentFinding finding = finding(id, now.minusSeconds(1));
        ClaimValidationResult result = validator.validate(claim(ClaimType.ANALYSIS,
                        List.of(id), new BigDecimal("0.8000"), null),
                new ClaimValidationContext(List.of(finding), null, null, now));

        assertThat(result.status()).isEqualTo(ClaimValidationStatus.DOWNGRADED);
        assertThat(result.effectiveConfidence()).isEqualByComparingTo("0.4000");
    }

    @Test
    void rejectsRecoveryAssertionWithoutProviderReconciliation() {
        ClaimValidationResult result = validator.validate(claim(ClaimType.RECOVERY_ASSERTION,
                        List.of(), BigDecimal.ONE, null),
                new ClaimValidationContext(List.of(), null, null, now));

        assertThat(result.status()).isEqualTo(ClaimValidationStatus.REJECTED);
        assertThat(result.errors()).contains("RECOVERY_CLAIM_WITHOUT_RECONCILIATION");
    }

    @Test
    void acceptsGroundedDiagnosisAndSupportedAction() {
        UUID id = UUID.randomUUID();
        IncidentFinding finding = finding(id, now.plusSeconds(300));
        ClaimValidationResult result = validator.validate(claim(ClaimType.RECOVERY_PROPOSAL,
                        List.of(id), new BigDecimal("0.9100"),
                        RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK.name()),
                new ClaimValidationContext(List.of(finding), null, null, now));

        assertThat(result.status()).isEqualTo(ClaimValidationStatus.VALID);
        assertThat(result.errors()).isEmpty();
        assertThat(result.effectiveConfidence()).isEqualByComparingTo("0.9100");
    }

    private ConsequentialAgentClaim claim(ClaimType type, List<UUID> refs,
                                           BigDecimal confidence, String action) {
        return new ConsequentialAgentClaim(UUID.randomUUID(), type, "grounded claim",
                confidence, refs, List.of(), action, now);
    }

    private IncidentFinding finding(UUID id, Instant validUntil) {
        IncidentFinding finding = mock(IncidentFinding.class);
        when(finding.getId()).thenReturn(id);
        when(finding.getValidUntil()).thenReturn(validUntil);
        return finding;
    }
}
