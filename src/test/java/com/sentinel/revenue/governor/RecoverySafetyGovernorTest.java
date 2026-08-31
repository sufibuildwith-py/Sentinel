package com.sentinel.revenue.governor;

import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecoverySafetyGovernorTest {
    @Test
    void allowsLowValueActionInsideEnvelope() {
        Fixture fixture = fixture(false);
        GovernorEvaluation result = fixture.governor.evaluate(fixture.action,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, 25_000, Instant.now());
        assertThat(result.allowed()).isTrue();
        assertThat(result.allowedValueMinor()).isEqualTo(25_000);
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void killSwitchAndValueLimitDenyBeforeExecution() {
        Fixture fixture = fixture(true);
        GovernorEvaluation result = fixture.governor.evaluate(fixture.action,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, 150_000, Instant.now());
        assertThat(result.allowed()).isFalse();
        assertThat(result.allowedValueMinor()).isZero();
        assertThat(result.violations()).anyMatch(value -> value.contains("ALL_AUTONOMOUS_EXECUTION"));
        assertThat(result.violations()).anyMatch(value -> value.contains("MAX_VALUE_PER_INCIDENT"));
    }

    private Fixture fixture(boolean autonomousKill) {
        KillSwitchService killSwitches = mock(KillSwitchService.class);
        when(killSwitches.enabled(KillSwitch.ALL_AUTONOMOUS_EXECUTION)).thenReturn(autonomousKill);
        RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        RecoveryOutcomeRepository outcomes = mock(RecoveryOutcomeRepository.class);
        RecoveryJobRepository jobs = mock(RecoveryJobRepository.class);
        RecoveryGovernorDecisionRepository decisions = mock(RecoveryGovernorDecisionRepository.class);
        RecoveryAction action = mock(RecoveryAction.class);
        when(action.getId()).thenReturn(UUID.randomUUID());
        when(action.getIncidentId()).thenReturn(UUID.randomUUID());
        when(action.getPolicyDecision()).thenReturn(PolicyDecision.AUTO);
        when(action.getExecutionAttempts()).thenReturn(0);
        when(actions.findAll()).thenReturn(List.of());
        when(jobs.findAll()).thenReturn(List.of());
        when(decisions.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        RecoverySafetyProperties properties = new RecoverySafetyProperties(1_000_000, 10,
                100_000, 10, 10, 3, 5, 0.25, 500_000, 2, 2);
        return new Fixture(new RecoverySafetyGovernor(properties, killSwitches, actions,
                outcomes, jobs, decisions), action);
    }
    private record Fixture(RecoverySafetyGovernor governor, RecoveryAction action) { }
}
