package com.sentinel.core.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineObservationFilterTest {
    @Test
    void preservesSafeCorrelationIdAndUsesOnlyBoundedMetricTags() throws Exception {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        PipelineObservationFilter filter = new PipelineObservationFilter(meters);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/revenue/events/batch");
        request.addHeader(PipelineObservationFilter.CORRELATION_HEADER, "demo-run-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(PipelineObservationFilter.CORRELATION_HEADER)).isEqualTo("demo-run-42");
        assertThat(meters.get("sentinel.pipeline.stage.duration").tag("stage", "ingestion").timer().count())
                .isEqualTo(1);
        assertThat(meters.getMeters()).allSatisfy(meter ->
                assertThat(meter.getId().getTags()).extracting(tag -> tag.getKey())
                        .doesNotContain("paymentId", "customerId", "incidentId", "correlationId"));
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void replacesUnsafeCorrelationValue() throws Exception {
        PipelineObservationFilter filter = new PipelineObservationFilter(new SimpleMeterRegistry());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/evaluation/report");
        request.addHeader(PipelineObservationFilter.CORRELATION_HEADER, "customer_0182@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getHeader(PipelineObservationFilter.CORRELATION_HEADER))
                .matches("[0-9a-f-]{36}");
    }
}
