package com.sentinel.revenue.execution;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;

class RazorpayPropertiesTest {
    @Test void rejectsLiveModeAndRequiresTestModeWhenEnabled() {
        assertThatThrownBy(() -> properties(true, "rzp_live_key", "secret"))
                .hasMessageContaining("Live Mode");
        assertThatThrownBy(() -> properties(true, "rzp_test_key", ""))
                .hasMessageContaining("Test Mode");
        assertThatCode(() -> properties(false, "", "")).doesNotThrowAnyException();
    }
    private RazorpayProperties properties(boolean enabled, String key, String secret) {
        return new RazorpayProperties(enabled, key, secret, URI.create("https://api.razorpay.com"),
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofMinutes(30), Duration.ofHours(24),
                3, 50, 2, 4, Duration.ofSeconds(30), false);
    }
}
