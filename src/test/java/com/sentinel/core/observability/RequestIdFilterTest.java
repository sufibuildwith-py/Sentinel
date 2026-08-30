package com.sentinel.core.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    @Test
    void preservesSafeRequestIdAcrossContextMdcAndResponse() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/revenue/incidents");
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "judge-demo-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(RequestContext.getRequestId()).isEqualTo("judge-demo-42");
            assertThat(MDC.get("requestId")).isEqualTo("judge-demo-42");
        });

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("judge-demo-42");
        assertThat(RequestContext.getRequestId()).isNull();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void replacesUnsafeRequestId() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "customer@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).matches("[0-9a-f-]{36}");
    }
}
