package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.DecisionCertificate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DecisionCertificateRepository {
    DecisionCertificate append(DecisionCertificate certificate);
    Optional<DecisionCertificate> findByDecisionId(UUID decisionId);
    List<DecisionCertificate> findAllByIncidentId(UUID incidentId);
}
