package com.sentinel.evaluation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataScannerTest {
    private final SensitiveDataScanner scanner = new SensitiveDataScanner();

    @Test
    void rejectsSecretsPiiAndRawIdentifiersButAllowsAggregateEvidence() {
        assertThat(scanner.findings("customer_0182 paid pay_private_123456; owner@example.com; +919876543210"))
                .hasSizeGreaterThanOrEqualTo(4);
        assertThat(scanner.findings("464 synthetic scenarios; 432 incidents; recovered 1032000 minor units"))
                .isEmpty();
    }
}
