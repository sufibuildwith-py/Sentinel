package com.sentinel.revenue.service;

import com.sentinel.revenue.api.BatchIngestionSummary;
import com.sentinel.revenue.api.PaymentEventBatchRequest;
import com.sentinel.revenue.api.PaymentEventRequest;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.repository.PaymentEventRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventIngestionServiceTest {

    @Mock
    private PaymentEventRepository repository;

    @Mock
    private PaymentEventBatchListener batchListener;

    private PaymentEventIngestionService service;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        service = new PaymentEventIngestionService(repository, validator, List.of(batchListener));
    }

    @Test
    void skipsDatabaseAndInBatchDuplicates() {
        PaymentEventRequest newEvent = request("pay_new", 1);
        PaymentEventRequest duplicateInBatch = request("pay_new", 1);
        PaymentEventRequest existingEvent = request("pay_existing", 2);
        when(repository.findAllByPaymentIdIn(anyCollection())).thenReturn(List.of(
                entity(existingEvent)));

        BatchIngestionSummary summary = service.ingest(new PaymentEventBatchRequest(
                List.of(newEvent, duplicateInBatch, existingEvent)));

        assertThat(summary.count()).isEqualTo(1);
        assertThat(summary.duplicatesSkipped()).isEqualTo(2);
        assertThat(summary.validationErrors()).isEmpty();
        verify(repository).saveAllAndFlush(argThat(events -> {
            var iterator = events.iterator();
            return iterator.hasNext()
                    && iterator.next().getPaymentId().equals("pay_new")
                    && !iterator.hasNext();
        }));
        verify(batchListener).onEventsPersisted(argThat(events -> events.size() == 1
                && events.get(0).getPaymentId().equals("pay_new")));
    }

    @Test
    void persistsValidRowsAndReportsInvalidRows() {
        PaymentEventRequest valid = request("pay_valid", 1);
        PaymentEventRequest invalid = new PaymentEventRequest(
                "", "order_2", "customer_2", -1, "inr", "", null,
                "FAILED", null, null, null, 0, null, -1, null, Map.of());
        when(repository.findAllByPaymentIdIn(anyCollection())).thenReturn(List.of());

        BatchIngestionSummary summary = service.ingest(
                new PaymentEventBatchRequest(List.of(valid, invalid)));

        assertThat(summary.count()).isEqualTo(1);
        assertThat(summary.duplicatesSkipped()).isZero();
        assertThat(summary.validationErrors())
                .extracting(error -> error.index())
                .containsOnly(1);
        assertThat(summary.validationErrors())
                .extracting(error -> error.field())
                .contains("paymentId", "amountMinor", "currency", "method",
                        "timestamp", "attemptNumber", "previousFailureCount");
    }

    private PaymentEventRequest request(String paymentId, int attemptNumber) {
        return new PaymentEventRequest(
                paymentId, "order_1", "customer_1", 10_000, "INR", "UPI",
                "HDFC", "FAILED", "NETWORK_ERROR", "network error",
                Instant.parse("2026-01-15T09:00:00Z"), attemptNumber,
                "CARD", 1, null, Map.of("synthetic", true));
    }

    private PaymentEvent entity(PaymentEventRequest request) {
        return new PaymentEvent(
                request.paymentId(), request.orderId(), request.customerId(),
                request.amountMinor(), request.currency(), request.method(),
                request.issuerBank(), request.status(), request.errorCode(),
                request.errorDescription(), request.timestamp(), request.attemptNumber(),
                request.previousSuccessfulMethod(), request.previousFailureCount(),
                request.subscriptionId(), request.metadata());
    }
}
