package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.RecoveryPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecoveryPlanRepository extends JpaRepository<RecoveryPlan, UUID> {
    List<RecoveryPlan> findAllByIncidentIncidentId(UUID incidentId);
}
