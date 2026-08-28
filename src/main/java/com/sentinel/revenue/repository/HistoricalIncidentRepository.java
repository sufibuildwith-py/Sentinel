package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.HistoricalIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HistoricalIncidentRepository extends JpaRepository<HistoricalIncident, UUID> {
    boolean existsByOriginalIncidentIncidentId(UUID incidentId);
}
