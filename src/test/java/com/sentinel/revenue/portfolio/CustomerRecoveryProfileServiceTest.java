package com.sentinel.revenue.portfolio;

import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CustomerRecoveryProfileServiceTest {
    @Test
    void producesBoundedAggregateWithoutReturningCustomerIdentifiers() {
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        PaymentEventRepository payments = mock(PaymentEventRepository.class);
        CustomerInteractionRepository interactions = mock(CustomerInteractionRepository.class);
        PromiseToPayRepository promises = mock(PromiseToPayRepository.class);
        UUID incidentId = UUID.randomUUID();
        RevenueIncident incident = mock(RevenueIncident.class);
        when(incident.getAffectedCustomers()).thenReturn(List.of("customer-private-1"));
        when(incidents.findById(incidentId)).thenReturn(Optional.of(incident));
        PaymentEvent successful = payment("SUCCESS", "UPI", null, 9);
        PaymentEvent failed = payment("FAILED", "CARD", "BANK_TIMEOUT", 10);
        when(payments.findAllByCustomerIdOrderByTimestampAsc("customer-private-1"))
                .thenReturn(List.of(successful, failed));
        when(interactions.findAllByCustomerRefOrderByCreatedAtAsc(anyString())).thenReturn(List.of());
        when(promises.findAllByCustomerRef(anyString())).thenReturn(List.of());

        CustomerRecoveryProfile profile = new CustomerRecoveryProfileService(incidents, payments,
                interactions, promises).forIncident(incidentId);

        assertThat(profile.preferredPaymentRail()).isEqualTo("UPI");
        assertThat(profile.failureClasses()).containsEntry("BANK_TIMEOUT", 1L);
        assertThat(profile.toString()).doesNotContain("customer-private-1");
        assertThat(profile.featureDefinitions()).allSatisfy(feature ->
                assertThat(feature.allowedUse()).containsAnyOf("only", "policy"));
    }

    private PaymentEvent payment(String status, String method, String error, int hour) {
        PaymentEvent event = mock(PaymentEvent.class);
        when(event.getStatus()).thenReturn(status); when(event.getMethod()).thenReturn(method);
        when(event.getErrorCode()).thenReturn(error);
        when(event.getTimestamp()).thenReturn(Instant.parse("2026-08-31T%02d:00:00Z".formatted(hour)));
        return event;
    }
}
