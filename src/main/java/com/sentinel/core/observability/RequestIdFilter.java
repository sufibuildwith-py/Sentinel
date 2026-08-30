package com.sentinel.core.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

public final class RequestIdFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        RequestContext.setRequestId(requestId);
        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            RequestContext.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String supplied = request.getHeader(REQUEST_ID_HEADER);
        if (supplied == null || supplied.isBlank()) {
            supplied = request.getHeader(PipelineObservationFilter.CORRELATION_HEADER);
        }
        return supplied != null && SAFE_REQUEST_ID.matcher(supplied).matches()
                ? supplied : UUID.randomUUID().toString();
    }
}
