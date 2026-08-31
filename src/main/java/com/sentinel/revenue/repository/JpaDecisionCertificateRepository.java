package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.DecisionCertificate;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaDecisionCertificateRepository implements DecisionCertificateRepository {
    private final EntityManager entityManager;

    public JpaDecisionCertificateRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override
    public DecisionCertificate append(DecisionCertificate certificate) {
        if (certificate.getId() != null) {
            throw new IllegalArgumentException("Decision certificates are immutable and append-only");
        }
        entityManager.persist(certificate);
        entityManager.flush();
        return certificate;
    }

    @Override
    public Optional<DecisionCertificate> findByDecisionId(UUID decisionId) {
        return entityManager.createQuery("select c from DecisionCertificate c where c.decisionId = :decisionId",
                        DecisionCertificate.class).setParameter("decisionId", decisionId).getResultStream().findFirst();
    }

    @Override
    public List<DecisionCertificate> findAllByIncidentId(UUID incidentId) {
        return entityManager.createQuery("""
                select c from DecisionCertificate c where c.incidentId = :incidentId
                order by c.createdAt asc, c.id asc
                """, DecisionCertificate.class).setParameter("incidentId", incidentId).getResultList();
    }
}
