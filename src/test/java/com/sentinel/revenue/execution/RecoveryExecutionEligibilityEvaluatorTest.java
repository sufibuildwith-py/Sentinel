package com.sentinel.revenue.execution;

import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.model.RecoveryGovernorDecision;
import com.sentinel.revenue.model.RecoveryPlan;
import com.sentinel.revenue.model.RecoveryStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecoveryExecutionEligibilityEvaluatorTest {

    @Test
    void approvedHumanActionIsNotOfferedWhenDeploymentExecutionIsDisabled() {
        RazorpayProperties properties = mock(RazorpayProperties.class);
        RecoveryAction action = approvedHumanAction();
        RecoveryPlan plan = paymentLinkPlan();

        RecoveryExecutionEligibility result =
                new RecoveryExecutionEligibilityEvaluator(properties).evaluate(action, plan, null);

        assertThat(result.enabled()).isFalse();
        assertThat(result.eligible()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("RAZORPAY_EXECUTION_DISABLED");
    }

    @Test
    void approvedHumanActionIsEligibleWhenAuthoritativeControlsAndDeploymentAllowIt() {
        RazorpayProperties properties = mock(RazorpayProperties.class);
        when(properties.enabled()).thenReturn(true);

        RecoveryExecutionEligibility result = new RecoveryExecutionEligibilityEvaluator(properties)
                .evaluate(approvedHumanAction(), paymentLinkPlan(), null);

        assertThat(result.eligible()).isTrue();
        assertThat(result.reasonCode()).isEqualTo("ELIGIBLE");
    }

    @Test
    void persistedGovernorDenialOverridesOtherwiseExecutableHumanApproval() {
        RazorpayProperties properties = mock(RazorpayProperties.class);
        when(properties.enabled()).thenReturn(true);
        RecoveryGovernorDecision governor = mock(RecoveryGovernorDecision.class);
        when(governor.isAllowed()).thenReturn(false);

        RecoveryExecutionEligibility result = new RecoveryExecutionEligibilityEvaluator(properties)
                .evaluate(approvedHumanAction(), paymentLinkPlan(), governor);

        assertThat(result.eligible()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("GOVERNOR_DENIED");
    }

    private RecoveryAction approvedHumanAction() {
        RecoveryAction action = mock(RecoveryAction.class);
        when(action.getStatus()).thenReturn(RecoveryActionStatus.APPROVED);
        when(action.getPolicyDecision()).thenReturn(PolicyDecision.HUMAN);
        when(action.getApprovedAt()).thenReturn(java.time.Instant.parse("2026-09-01T18:41:53Z"));
        return action;
    }

    private RecoveryPlan paymentLinkPlan() {
        RecoveryPlan plan = mock(RecoveryPlan.class);
        when(plan.getStrategy()).thenReturn(RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK);
        return plan;
    }
}
