package com.sentinel.core.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitInterceptorTest {
    private final RateLimitInterceptor interceptor = new RateLimitInterceptor(
            new RateLimitProperties(1, 1, 1));

    @Test
    void limitsOnlyConfiguredPostEndpointsPerForwardedClientAddress() {
        MockHttpServletRequest first = request("POST", "/api/v1/demo/reset", "203.0.113.10");
        MockHttpServletRequest second = request("POST", "/api/v1/demo/inject/upi-outage", "203.0.113.10");
        MockHttpServletRequest otherClient = request("POST", "/api/v1/demo/reset", "203.0.113.11");

        assertThat(interceptor.preHandle(first, new MockHttpServletResponse(), new Object())).isTrue();
        assertThatThrownBy(() -> interceptor.preHandle(second, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(RateLimitExceededException.class)
                .extracting("retryAfterSeconds")
                .matches(value -> (long) value >= 1);
        assertThat(interceptor.preHandle(otherClient, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void doesNotLimitHealthReadsOrUnlistedMutations() {
        assertThat(interceptor.preHandle(request("GET", "/actuator/health", null),
                new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(interceptor.preHandle(request("POST", "/api/v1/revenue/events/batch", null),
                new MockHttpServletResponse(), new Object())).isTrue();
    }

    private MockHttpServletRequest request(String method, String path, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("127.0.0.1");
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor + ", 10.0.0.1");
        }
        return request;
    }
}
