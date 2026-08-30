package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.PaymentDowntime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentDowntimeRepository extends JpaRepository<PaymentDowntime, UUID> {
}
