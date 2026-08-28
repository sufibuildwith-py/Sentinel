package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.RecoveryOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface RecoveryOutcomeRepository extends JpaRepository<RecoveryOutcome, UUID> {
    List<RecoveryOutcome> findAllByIncidentIncidentId(UUID incidentId);

    @Query("select outcome from RecoveryOutcome outcome where outcome.recoveryAction.id = :recoveryActionId")
    List<RecoveryOutcome> findAllByRecoveryActionId(@Param("recoveryActionId") UUID recoveryActionId);

    @Query("select outcome from RecoveryOutcome outcome where outcome.recoveryAction.id = :recoveryActionId")
    Optional<RecoveryOutcome> findByRecoveryActionId(@Param("recoveryActionId") UUID recoveryActionId);
}
