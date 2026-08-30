package com.sentinel.core.error;

import com.sentinel.core.observability.RequestContext;
import com.sentinel.core.ratelimit.RateLimitExceededException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitErrorResponseTest {
    @AfterEach
    void clearRequestContext() {
        RequestContext.clear();
    }

    @Test
    void returnsSanitizedStandardErrorWithRequestIdAndRetryAfter() {
        RequestContext.setRequestId("request-rate-limited");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/evaluation/run");

        ResponseEntity<Object> response = new GlobalExceptionHandler()
                .handleRateLimitExceeded(new RateLimitExceededException(17), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("17");
        assertThat(response.getBody()).isInstanceOfSatisfying(ApiError.class, error -> {
            assertThat(error.status()).isEqualTo(429);
            assertThat(error.code()).isEqualTo("RATE_LIMIT_EXCEEDED");
            assertThat(error.message()).isEqualTo("Rate limit exceeded. Please wait before retrying.");
            assertThat(error.requestId()).isEqualTo("request-rate-limited");
        });
    }
}
