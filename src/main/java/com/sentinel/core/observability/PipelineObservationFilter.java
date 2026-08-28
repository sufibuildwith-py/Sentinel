package com.sentinel.core.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@ConditionalOnBean(MeterRegistry.class)
public class PipelineObservationFilter extends OncePerRequestFilter {
    public static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final Pattern SAFE_CORRELATION = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private final MeterRegistry meters;

    public PipelineObservationFilter(MeterRegistry meters) { this.meters = meters; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = correlationId(request.getHeader(CORRELATION_HEADER));
        response.setHeader(CORRELATION_HEADER, correlationId);
        String stage = stage(request.getRequestURI());
        Timer.Sample sample = Timer.start(meters);
        boolean failed = false;
        MDC.put("correlationId", correlationId);
        try {
            chain.doFilter(request, response);
            failed = response.getStatus() >= 500;
        } catch (IOException | ServletException | RuntimeException exception) {
            failed = true;
            throw exception;
        } finally {
            String outcome = failed ? "error" : "success";
            sample.stop(Timer.builder("sentinel.pipeline.stage.duration")
                    .description("Bounded Sentinel pipeline stage duration")
                    .tags("stage", stage, "outcome", outcome)
                    .register(meters));
            if (failed) Counter.builder("sentinel.pipeline.stage.errors")
                    .tags("stage", stage).register(meters).increment();
            MDC.remove("correlationId");
        }
    }

    private String correlationId(String supplied) {
        return supplied != null && SAFE_CORRELATION.matcher(supplied).matches()
                ? supplied : UUID.randomUUID().toString();
    }

    private String stage(String uri) {
        String path = uri == null ? "" : uri.toLowerCase(Locale.ROOT);
        if (path.contains("/events")) return "ingestion";
        if (path.contains("/investigate")) return "diagnosis";
        if (path.contains("/plan") || path.contains("/approve") || path.contains("/reject")) return "policy";
        if (path.contains("/execute")) return "execution";
        if (path.contains("/webhook")) return "webhook";
        if (path.contains("/evaluation")) return "evaluation";
        if (path.contains("/demo/inject")) return "detection";
        return path.startsWith("/api/") ? "api" : "http";
    }
}
