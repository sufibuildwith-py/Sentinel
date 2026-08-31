package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.RecoveryGovernorDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RecoveryGovernorDecisionRepository extends JpaRepository<RecoveryGovernorDecision, UUID> {
    List<RecoveryGovernorDecision> findAllByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
