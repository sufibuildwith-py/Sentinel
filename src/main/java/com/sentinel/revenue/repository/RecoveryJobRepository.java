package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.RecoveryJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface RecoveryJobRepository extends JpaRepository<RecoveryJob, UUID> {
    List<RecoveryJob> findByStatusAndNextAttemptAtBefore(String status, Instant now);
    Optional<RecoveryJob> findByIncidentId(UUID incidentId);
    Optional<RecoveryJob> findFirstByIncidentIdAndStrategyAndStatusInOrderByCreatedAtDesc(
            UUID incidentId, String strategy, Collection<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from RecoveryJob job where job.id = :jobId")
    Optional<RecoveryJob> findForUpdateById(@Param("jobId") UUID jobId);
}
