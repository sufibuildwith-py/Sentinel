package com.sentinel.revenue.service;

import com.sentinel.revenue.api.BatchIngestionSummary;
import com.sentinel.revenue.api.BatchValidationError;
import com.sentinel.revenue.api.PaymentEventBatchRequest;
import com.sentinel.revenue.api.PaymentEventRequest;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.repository.PaymentEventRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PaymentEventIngestionService {

    private final PaymentEventRepository paymentEventRepository;
    private final Validator validator;
    private final List<PaymentEventBatchListener> batchListeners;

    public PaymentEventIngestionService(PaymentEventRepository paymentEventRepository,
                                        Validator validator,
                                        List<PaymentEventBatchListener> batchListeners) {
        this.paymentEventRepository = paymentEventRepository;
        this.validator = validator;
        this.batchListeners = List.copyOf(batchListeners);
    }

    @Transactional
    public BatchIngestionSummary ingest(PaymentEventBatchRequest batch) {
        List<BatchValidationError> validationErrors = new ArrayList<>();
        List<IndexedRequest> validRequests = validate(batch.events(), validationErrors);

        Set<PaymentKey> knownKeys = loadExistingKeys(validRequests);
        Set<PaymentKey> requestKeys = new HashSet<>();
        List<PaymentEvent> toPersist = new ArrayList<>();
        int duplicatesSkipped = 0;

        for (IndexedRequest indexed : validRequests) {
            PaymentEventRequest request = indexed.request();
            PaymentKey key = new PaymentKey(request.paymentId(), request.attemptNumber());
            if (knownKeys.contains(key) || !requestKeys.add(key)) {
                duplicatesSkipped++;
                continue;
            }
            toPersist.add(toEntity(request));
        }

        paymentEventRepository.saveAllAndFlush(toPersist);
        if (!toPersist.isEmpty()) {
            List<PaymentEvent> persisted = List.copyOf(toPersist);
            batchListeners.forEach(listener -> listener.onEventsPersisted(persisted));
        }
        return new BatchIngestionSummary(toPersist.size(), duplicatesSkipped, validationErrors);
    }

    private List<IndexedRequest> validate(List<PaymentEventRequest> requests,
                                          List<BatchValidationError> errors) {
        List<IndexedRequest> valid = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            int eventIndex = index;
            PaymentEventRequest request = requests.get(index);
            if (request == null) {
                errors.add(new BatchValidationError(index, null, "event", "must not be null"));
                continue;
            }

            Set<ConstraintViolation<PaymentEventRequest>> violations = validator.validate(request);
            if (violations.isEmpty()) {
                valid.add(new IndexedRequest(index, request));
                continue;
            }
            violations.stream()
                    .sorted((left, right) -> left.getPropertyPath().toString()
                            .compareTo(right.getPropertyPath().toString()))
                    .map(violation -> toError(eventIndex, request.paymentId(), violation))
                    .forEach(errors::add);
        }
        return valid;
    }

    private Set<PaymentKey> loadExistingKeys(Collection<IndexedRequest> requests) {
        Set<String> paymentIds = new LinkedHashSet<>();
        requests.forEach(indexed -> paymentIds.add(indexed.request().paymentId()));
        if (paymentIds.isEmpty()) {
            return Set.of();
        }

        Set<PaymentKey> keys = new HashSet<>();
        paymentEventRepository.findAllByPaymentIdIn(paymentIds)
                .forEach(event -> keys.add(
                        new PaymentKey(event.getPaymentId(), event.getAttemptNumber())));
        return keys;
    }

    private BatchValidationError toError(int index, String paymentId,
                                         ConstraintViolation<PaymentEventRequest> violation) {
        return new BatchValidationError(index, paymentId,
                violation.getPropertyPath().toString(), violation.getMessage());
    }

    private PaymentEvent toEntity(PaymentEventRequest request) {
        return new PaymentEvent(
                request.paymentId(),
                request.orderId(),
                request.customerId(),
                request.amountMinor(),
                request.currency(),
                request.method(),
                request.issuerBank(),
                request.status(),
                request.errorCode(),
                request.errorDescription(),
                request.timestamp(),
                request.attemptNumber(),
                request.previousSuccessfulMethod(),
                request.previousFailureCount(),
                request.subscriptionId(),
                request.metadata());
    }

    private record IndexedRequest(int index, PaymentEventRequest request) {
    }

    private record PaymentKey(String paymentId, int attemptNumber) {
    }
}
