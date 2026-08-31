package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.SystemicRecoveryIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SystemicRecoveryIncidentRepository extends JpaRepository<SystemicRecoveryIncident, UUID> {
    List<SystemicRecoveryIncident> findAllByMerchantIdOrderByCreatedAtDesc(String merchantId);
}
