package com.sentinel.revenue.service;

import com.sentinel.revenue.model.RevenueIncidentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RevenueIncidentStateMachineTest {

    private final RevenueIncidentStateMachine stateMachine = new RevenueIncidentStateMachine();

    @Test
    void acceptsEachDocumentedPath() {
        assertThat(stateMachine.transition(RevenueIncidentStatus.DETECTED,
                RevenueIncidentStatus.INVESTIGATING))
                .isEqualTo(RevenueIncidentStatus.INVESTIGATING);
        assertThat(stateMachine.canTransition(RevenueIncidentStatus.POLICY_REVIEW,
                RevenueIncidentStatus.APPROVED)).isTrue();
        assertThat(stateMachine.canTransition(RevenueIncidentStatus.POLICY_REVIEW,
                RevenueIncidentStatus.HUMAN_REVIEW)).isTrue();
        assertThat(stateMachine.canTransition(RevenueIncidentStatus.MONITORING,
                RevenueIncidentStatus.RECOVERED)).isTrue();
        assertThat(stateMachine.canTransition(RevenueIncidentStatus.MONITORING,
                RevenueIncidentStatus.FAILED)).isTrue();
        assertThat(stateMachine.canTransition(RevenueIncidentStatus.MONITORING,
                RevenueIncidentStatus.STOPPED)).isTrue();
    }

    @Test
    void rejectsSkippedBackwardAndTerminalTransitions() {
        assertThatThrownBy(() -> stateMachine.transition(
                RevenueIncidentStatus.DETECTED, RevenueIncidentStatus.DIAGNOSED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DETECTED -> DIAGNOSED");
        assertThat(stateMachine.canTransition(
                RevenueIncidentStatus.EXECUTING, RevenueIncidentStatus.APPROVED)).isFalse();
        assertThat(stateMachine.canTransition(
                RevenueIncidentStatus.RECOVERED, RevenueIncidentStatus.MONITORING)).isFalse();
    }
}
