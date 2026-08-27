package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.RevenueIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RevenueIncidentRepository extends JpaRepository<RevenueIncident, UUID> {
}
