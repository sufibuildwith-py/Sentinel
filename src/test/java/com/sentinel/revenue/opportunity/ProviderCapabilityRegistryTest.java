package com.sentinel.revenue.opportunity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderCapabilityRegistryTest {
    private final ProviderCapabilityRegistry registry = new ProviderCapabilityRegistry();

    @Test
    void marketplaceIsVersionedProviderRealisticAndKeepsNoActionFirstClass() {
        assertThat(registry.catalog()).allSatisfy(definition -> {
            assertThat(definition.version()).isEqualTo(ProviderCapabilityRegistry.CATALOG_VERSION);
            assertThat(definition.executionAdapter()).isNotBlank();
            assertThat(definition.verificationMethod()).isNotBlank();
        });
        assertThat(registry.requireDefinition(OpportunityAction.NO_ACTION).materiallyExecutable()).isFalse();
        assertThat(registry.requireDefinition(OpportunityAction.CREATE_PAYMENT_LINK).providerCapability())
                .isEqualTo(ProviderCapability.RAZORPAY_PAYMENT_LINKS);
        assertThat(registry.requireDefinition(OpportunityAction.CUSTOMER_OUTREACH).verificationMethod())
                .isEqualTo("SIMULATED_NOT_SENT");
    }

    @Test
    void unsupportedProviderActionFailsClosed() {
        assertThatThrownBy(() -> registry.requireDefinition(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported recovery action");
    }
}
