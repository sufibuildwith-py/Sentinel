package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.RecoveryAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecoveryActionRepository extends JpaRepository<RecoveryAction, UUID> {
    List<RecoveryAction> findAllByIncidentIncidentId(UUID incidentId);

    List<RecoveryAction> findAllByRecoveryPlanId(UUID recoveryPlanId);
}
