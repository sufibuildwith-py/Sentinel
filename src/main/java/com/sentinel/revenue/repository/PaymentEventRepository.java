package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {
    boolean existsByPaymentIdAndAttemptNumber(String paymentId, int attemptNumber);

    List<PaymentEvent> findAllByPaymentIdIn(Collection<String> paymentIds);

    List<PaymentEvent> findAllByTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
            Instant startInclusive, Instant endExclusive);
}
