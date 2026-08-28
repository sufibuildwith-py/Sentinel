package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryActionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Collection;
import java.util.UUID;
import java.util.Optional;

public interface RecoveryActionRepository extends JpaRepository<RecoveryAction, UUID> {
    List<RecoveryAction> findAllByIncidentIncidentId(UUID incidentId);

    @Query("select a from RecoveryAction a where a.recoveryPlan.id = :recoveryPlanId")
    List<RecoveryAction> findAllByRecoveryPlanId(@Param("recoveryPlanId") UUID recoveryPlanId);

    List<RecoveryAction> findAllByIncidentIncidentIdAndStatusIn(
            UUID incidentId, Collection<RecoveryActionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from RecoveryAction a where a.incident.incidentId = :incidentId")
    Optional<RecoveryAction> findForExecutionByIncidentId(UUID incidentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from RecoveryAction a where a.externalResourceId = :providerLinkId")
    Optional<RecoveryAction> findForWebhookByExternalResourceId(String providerLinkId);

    Optional<RecoveryAction> findByExternalResourceId(String providerLinkId);
}
