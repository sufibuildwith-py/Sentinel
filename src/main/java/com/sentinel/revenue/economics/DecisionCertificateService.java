package com.sentinel.revenue.economics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sentinel.revenue.model.DecisionCertificate;
import com.sentinel.revenue.model.DecisionCertificateDraft;
import com.sentinel.revenue.repository.DecisionCertificateRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class DecisionCertificateService {
    private final DecisionCertificateRepository certificates;
    private final RevenueIncidentRepository incidents;
    private final ObjectMapper canonicalJson;
    private final Clock clock;

    public DecisionCertificateService(DecisionCertificateRepository certificates,
                                      RevenueIncidentRepository incidents, ObjectMapper objectMapper,
                                      Clock clock) {
        this.certificates = certificates; this.incidents = incidents; this.clock = clock;
        this.canonicalJson = objectMapper.copy()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Transactional
    public DecisionCertificate issue(DecisionCertificateDraft draft) {
        validate(draft);
        if (!incidents.existsById(draft.incidentId())) {
            throw new IllegalArgumentException("Revenue incident not found: " + draft.incidentId());
        }
        String hash = sha256(draft);
        return certificates.findByDecisionId(draft.decisionId()).map(existing -> {
            if (!existing.getCertificateSha256().equals(hash)) {
                throw new IllegalStateException("Decision certificate already exists with different content");
            }
            return existing;
        }).orElseGet(() -> certificates.append(new DecisionCertificate(draft, hash, clock.instant())));
    }

    @Transactional(readOnly = true)
    public List<DecisionCertificate> forIncident(UUID incidentId) {
        return certificates.findAllByIncidentId(incidentId);
    }

    private void validate(DecisionCertificateDraft draft) {
        if (draft == null || draft.decisionId() == null || draft.incidentId() == null
                || blank(draft.decisionType()) || blank(draft.policyVersion()) || blank(draft.modelVersion())
                || blank(draft.featureSchemaVersion()) || blank(draft.strategyVersion())
                || !isHash(draft.inputSnapshotHash()) || blank(draft.selectedAction())
                || blank(draft.counterfactualMethod()) || draft.evidenceQuality() == null
                || blank(draft.authorizationResult()) || blank(draft.exposureDecision())
                || blank(draft.finalTruthState()) || blank(draft.certificateVersion())) {
            throw new IllegalArgumentException("Decision certificate is incomplete");
        }
        if ("RECOVERED_CONFIRMED".equals(draft.finalTruthState())
                && (blank(draft.providerReference()) || blank(draft.reconciliationReference()))) {
            throw new IllegalArgumentException("Confirmed recovery requires provider and reconciliation references");
        }
    }

    private String sha256(DecisionCertificateDraft draft) {
        try {
            byte[] bytes = canonicalJson.writeValueAsBytes(draft);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to create decision certificate hash", exception);
        }
    }

    public static String hashText(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private boolean isHash(String value) { return value != null && value.matches("[0-9a-f]{64}"); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
