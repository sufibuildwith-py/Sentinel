package com.sentinel.revenue.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.revenue.governor.RecoverySafetyProperties;
import com.sentinel.revenue.model.RecoveryStrategy;
import com.sentinel.revenue.repository.CompiledPolicyConstitutionRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PolicyConstitutionCompilerTest {
    @Test
    void compilesExistingDeterministicLimitsWithImmutableAuthorityBoundary() {
        PolicyProperties policy = new PolicyProperties(0.8, 100_000, 3, 2, 0.5,
                Duration.ofHours(1), Set.of(RecoveryStrategy.NO_ACTION,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK), Set.of("PAID", "REFUNDED"));
        RecoverySafetyProperties governor = new RecoverySafetyProperties(1_000_000, 10,
                100_000, 10, 4, 3, 2, 0.25, 500_000, 2, 1);
        PolicyConstitutionCompiler compiler = new PolicyConstitutionCompiler(policy, governor,
                mock(CompiledPolicyConstitutionRepository.class), new ObjectMapper(), Clock.systemUTC());

        PolicyConstitutionCompiler.CompiledPreview preview = compiler.preview("merchant-1", "policy-v2");

        assertThat(preview.hash()).hasSize(64);
        assertThat(preview.constitution().authority().llm()).isEqualTo("PROPOSE_ONLY");
        assertThat(preview.constitution().authority().financialExecution()).isEqualTo("DETERMINISTIC_ONLY");
        assertThat(preview.constitution().limits().maximumDailyExposureMinor()).isEqualTo(1_000_000);
        assertThat(preview.status()).isEqualTo("COMPILED_PENDING_PROMOTION");
    }
}
