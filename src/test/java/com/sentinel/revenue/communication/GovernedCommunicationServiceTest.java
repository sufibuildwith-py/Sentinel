package com.sentinel.revenue.communication;

import com.sentinel.revenue.governor.*;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GovernedCommunicationServiceTest {
    @Test
    void simulationIsExplicitAndPersistedOnlyAfterPolicyPasses() {
        CustomerContactPreferenceRepository preferences = mock(CustomerContactPreferenceRepository.class);
        CustomerInteractionRepository interactions = mock(CustomerInteractionRepository.class);
        KillSwitchService killSwitches = mock(KillSwitchService.class);
        when(preferences.findById("customer_0182")).thenReturn(Optional.of(new CustomerContactPreference(
                "customer_0182", true, false, false, "UTC", Instant.now())));
        when(interactions.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        GovernedCommunicationService service = new GovernedCommunicationService(
                new SimulationCommunicationAdapter(), properties(), preferences, interactions, killSwitches);

        CustomerInteraction result = service.communicate(UUID.randomUUID(), UUID.randomUUID(),
                "customer_0182", "SMS", CustomerIntent.PAY_NOW, "PAYMENT_RECOVERY_LINK",
                Instant.parse("2026-08-31T12:00:00Z"));

        assertThat(result.getDeliveryMode()).isEqualTo("TEST_SIMULATION");
        assertThat(result.getStatus()).isEqualTo("SIMULATED_NOT_SENT");
        assertThat(result.getPolicyTrace()).contains("CONSENT PASS", "TEMPLATE_ALLOWLIST PASS",
                "MODE=TEST_SIMULATION");
    }

    @Test
    void optOutBlocksAdapterInvocation() {
        CommunicationAdapter adapter = mock(CommunicationAdapter.class);
        CustomerContactPreferenceRepository preferences = mock(CustomerContactPreferenceRepository.class);
        CustomerInteractionRepository interactions = mock(CustomerInteractionRepository.class);
        when(preferences.findById("customer_0182")).thenReturn(Optional.of(new CustomerContactPreference(
                "customer_0182", true, false, true, "UTC", Instant.now())));
        GovernedCommunicationService service = new GovernedCommunicationService(adapter, properties(),
                preferences, interactions, mock(KillSwitchService.class));
        assertThatThrownBy(() -> service.communicate(UUID.randomUUID(), UUID.randomUUID(),
                "customer_0182", "SMS", CustomerIntent.PAY_NOW, "PAYMENT_RECOVERY_LINK",
                Instant.parse("2026-08-31T12:00:00Z"))).hasMessage("OPTED_OUT");
        verifyNoInteractions(adapter);
    }

    private CommunicationProperties properties() {
        return new CommunicationProperties(LocalTime.of(21, 0), LocalTime.of(8, 0),
                2, Set.of("PAYMENT_RECOVERY_LINK"), 30);
    }
}
