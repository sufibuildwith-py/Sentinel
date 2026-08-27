package com.sentinel.revenue.investigation;

import com.sentinel.core.agent.AgentContext;
import com.sentinel.core.agent.AgentResult;
import com.sentinel.core.llm.LlmClient;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RootCauseAgentTest {
    private StructuredLlmGateway gateway;

    @AfterEach void close() { if (gateway != null) gateway.close(); }

    @Test
    void retriesInvalidOutputOnceThenReturnsTraceableFallback() {
        LlmClient llm = mock(LlmClient.class);
        when(llm.generateStructured(any(), eq(RootCauseResult.class)))
                .thenReturn(new RootCauseResult("", 0.9, List.of(""), List.of(), false));
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        gateway = new StructuredLlmGateway(llm, properties());
        RootCauseAgent agent = new RootCauseAgent(new PromptContextBuilder(), gateway, validator);

        AgentResult<RootCauseResult> result = agent.execute(input("UPI_DEGRADATION", 0.80), context());

        assertThat(result.output().rootCause()).isEqualTo("UPI issuer degradation");
        assertThat(result.output().confidence()).isEqualTo(0.48);
        assertThat(result.output().llmUnavailable()).isTrue();
        assertThat(result.output().evidence()).anyMatch(line -> line.contains("two bounded attempts"));
        verify(llm, times(2)).generateStructured(any(), eq(RootCauseResult.class));
    }

    static RootCauseInput input(String type, double signatureShare) {
        RevenueIncident incident = new RevenueIncident(type, RevenueIncidentStatus.INVESTIGATING,
                "HIGH", 200000, Instant.now(), List.of("p1"), List.of("c1"),
                List.of("all detector rules passed"), null, null);
        PatternAnalysis pattern = new PatternAnalysis(null,
                List.of("100% of failures have the incident signature."), signatureShare);
        CustomerContext customer = new CustomerContext(1, 0, Map.of(), 0, null,
                List.of("No historical denominator."));
        String category = type.contains("UPI") ? "PAYMENT_RAIL_DEGRADATION" : "PAYMENT_PROVIDER_OUTAGE";
        return new RootCauseInput(incident,
                new TriageResult(category, "HIGH", "analyze", List.of(), List.of(), false),
                new AnalystFindings(pattern, customer, pattern.evidence(), signatureShare), List.of());
    }

    static AgentContext context() {
        Instant now = Instant.now();
        return new AgentContext("test", now, now.plusSeconds(10), Map.of());
    }

    public static InvestigationProperties properties() {
        return new InvestigationProperties(Duration.ofSeconds(1), 5, 0.0,
                50, 2, 4, Duration.ofSeconds(30));
    }
}
