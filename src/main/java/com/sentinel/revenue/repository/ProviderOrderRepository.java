package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.ProviderOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProviderOrderRepository extends JpaRepository<ProviderOrder, UUID> {
    Optional<ProviderOrder> findByIdempotencyKey(String key);
    Optional<ProviderOrder> findByRazorpayOrderId(String razorpayOrderId);
}
