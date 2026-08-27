package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.RecoveryOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecoveryOutcomeRepository extends JpaRepository<RecoveryOutcome, UUID> {
    List<RecoveryOutcome> findAllByIncidentIncidentId(UUID incidentId);

    List<RecoveryOutcome> findAllByRecoveryActionId(UUID recoveryActionId);
}
