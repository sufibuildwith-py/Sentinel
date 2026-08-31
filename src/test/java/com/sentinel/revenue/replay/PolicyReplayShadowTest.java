package com.sentinel.revenue.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.revenue.communication.CommunicationAdapter;
import com.sentinel.revenue.execution.RazorpayGateway;
import com.sentinel.revenue.governor.RecoverySafetyProperties;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.policy.*;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PolicyReplayShadowTest {
    private final Instant fixed = Instant.parse("2026-08-31T10:00:00Z");
    private final Clock clock = Clock.fixed(fixed, ZoneOffset.UTC);

    @Test
    void sameImmutableSnapshotAndVersionsReplayDeterministicallyWithExactAttribution() {
        Fixture f = fixture(safeContext(), PolicyDecision.AUTO, "ALLOW");
        PolicyReplayResult first = f.replay.replay(f.snapshot.getId());
        PolicyReplayResult second = f.replay.replay(f.snapshot.getId());
        assertThat(first).isEqualTo(second);
        assertThat(first.featureSchemaVersion()).isEqualTo("features-v7");
        assertThat(first.modelVersion()).isEqualTo("no-model");
        assertThat(first.policyVersion()).isEqualTo("policy-v1");
        assertThat(first.strategyVersion()).isEqualTo("strategy-v3");
        assertThat(first.governorVersion()).isEqualTo("governor-v1");
        assertThat(first.seed()).isEqualTo(20260901L);
    }

    @Test
    void proposedPolicyAndModelAreShadowComparedWithoutAnyToolOrHistoricalMutationPath() {
        Fixture f = fixture(safeContext(), PolicyDecision.AUTO, "ALLOW");
        RazorpayGateway provider = mock(RazorpayGateway.class);
        CommunicationAdapter communication = mock(CommunicationAdapter.class);
        PolicyProperties stricter = properties(0.99);
        ShadowComparison result = f.shadow.compare(f.snapshot.getId(), new ShadowCandidate(
                "NO_ACTION", "candidate-v2", new ReplayPolicyVersion("policy-proposed-v2", stricter),
                0L, new BigDecimal("0.7000"), new BigDecimal("0.4000"),
                List.of("NO_ACTION", "CREATE_PAYMENT_LINK"), "candidate loses to active baseline"));
        assertThat(result.policyChanged()).isTrue();
        assertThat(result.shadowPolicy()).isEqualTo(PolicyDecision.HUMAN);
        assertThat(result.actionChanged()).isTrue();
        assertThat(result.modelVersion()).isEqualTo("candidate-v2");
        verifyNoInteractions(provider, communication);
        assertNoToolDependencies(PolicyReplayService.class, ShadowDecisionEngine.class);
    }

    @Test
    void unsafeCandidateRemainsDeniedAndCannotCreateExecution() {
        PolicyContext unsafe = new PolicyContext(1.0, 10_000, Set.of("FAILED"), false,
                0, 0, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, true,
                fixed.plusSeconds(600), fixed, 1, 0.1, true);
        Fixture f = fixture(unsafe, PolicyDecision.DENY, "DENY");
        ShadowComparison result = f.shadow.compare(f.snapshot.getId(), new ShadowCandidate(
                "CREATE_PAYMENT_LINK", "candidate-v2", new ReplayPolicyVersion("policy-proposed-v2", properties(0.1)),
                10_000L, BigDecimal.ONE, BigDecimal.ONE, List.of("CREATE_PAYMENT_LINK"), "unsafe proposal"));
        assertThat(result.shadowPolicy()).isEqualTo(PolicyDecision.DENY);
        assertThat(result.shadowGovernor()).isEqualTo("DENY");
        assertThat(result.criticalRegression()).isFalse();
        assertNoToolDependencies(ShadowDecisionEngine.class);
    }

    private Fixture fixture(PolicyContext context, PolicyDecision productionPolicy, String productionGovernor) {
        PolicyReplaySnapshotRepository snapshots = mock(PolicyReplaySnapshotRepository.class);
        ShadowDecisionDifferenceRepository differences = mock(ShadowDecisionDifferenceRepository.class);
        PolicyEngine active = new PolicyEngine(properties(0.85), new MandatoryStopEvaluator(properties(0.85)));
        ReplayGovernorEvaluator governor = new ReplayGovernorEvaluator(governorProperties());
        PolicyReplayService replay = new PolicyReplayService(snapshots, active, governor, new ObjectMapper(), clock);
        GovernorReplayContext governorContext = new GovernorReplayContext(10_000, 0, 0, 0, 0,
                0, 0, 0, 0, PolicyDecision.AUTO, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, Set.of());
        PolicyReplaySnapshot snapshot = new PolicyReplaySnapshot(UUID.randomUUID(), context, governorContext,
                "features-v7", "no-model", "policy-v1", "strategy-v3", "governor-v1", 20260901,
                productionPolicy, "CREATE_PAYMENT_LINK", productionGovernor, 10_000L,
                new BigDecimal("0.9000"), "hash-fixed", fixed);
        UUID id = UUID.randomUUID(); ReflectionTestUtils.setField(snapshot, "id", id);
        when(snapshots.findById(id)).thenReturn(Optional.of(snapshot));
        when(differences.saveAndFlush(any())).thenAnswer(call -> {
            ShadowDecisionDifference value = call.getArgument(0);
            ReflectionTestUtils.setField(value, "id", UUID.randomUUID()); return value;
        });
        return new Fixture(snapshot, replay, new ShadowDecisionEngine(snapshots, differences, replay, clock));
    }

    private PolicyContext safeContext() { return new PolicyContext(0.9, 10_000, Set.of("FAILED"), false,
            0, 0, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, false, fixed.plusSeconds(600), fixed,
            1, 0.1, false); }
    private PolicyProperties properties(double confidence) { return new PolicyProperties(confidence, 100_000,
            3, 2, 0.7, Duration.ofMinutes(30), Set.of(RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK),
            Set.of("CAPTURED", "PAID", "REFUNDED")); }
    private RecoverySafetyProperties governorProperties() { return new RecoverySafetyProperties(1_000_000,
            100, 100_000, 30, 20, 3, 10, 0.25, 500_000, 2, 2); }
    private void assertNoToolDependencies(Class<?>... types) {
        for (Class<?> type : types) assertThat(Arrays.stream(type.getDeclaredFields())
                .map(field -> field.getType().getName())).noneMatch(name -> name.contains("Razorpay")
                || name.contains("CommunicationAdapter") || name.contains("RecoveryExecutionService")
                || name.contains("RecoveryWorker") || name.contains("Webhook"));
    }
    private record Fixture(PolicyReplaySnapshot snapshot, PolicyReplayService replay, ShadowDecisionEngine shadow) { }
}
