package com.sentinel.revenue.repository;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRepositoryContractTest {
    @Test
    void exposesNoUpdateOrDeleteOperations() {
        assertThat(Arrays.stream(AuditEventRepository.class.getMethods()).map(method -> method.getName()))
                .containsExactlyInAnyOrder("append", "findTrail")
                .doesNotContain("save", "delete", "deleteAll", "flush");
    }
}
