package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.ProviderPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProviderPaymentRepository extends JpaRepository<ProviderPayment, UUID> {
    Optional<ProviderPayment> findByRazorpayPaymentId(String razorpayPaymentId);
}
