package com.sentinel.revenue.investigation;

import com.sentinel.core.agent.*;
import com.sentinel.core.error.UpstreamServiceException;
import com.sentinel.core.llm.LlmRuntimeStatus;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RootCauseAgent implements SentinelAgent<RootCauseInput, RootCauseResult> {
    private static final Logger log = LoggerFactory.getLogger(RootCauseAgent.class);
    private final PromptContextBuilder prompts;
    private final StructuredLlmGateway llm;
    private final Validator validator;
    private final LlmRuntimeStatus runtimeStatus;

    @Autowired
    public RootCauseAgent(PromptContextBuilder prompts, StructuredLlmGateway llm, Validator validator, LlmRuntimeStatus runtimeStatus) {
        this.prompts = prompts;
        this.llm = llm;
        this.validator = validator;
        this.runtimeStatus = runtimeStatus;
    }

    public RootCauseAgent(PromptContextBuilder prompts, StructuredLlmGateway llm, Validator validator) {
        this(prompts, llm, validator, null);
    }

    @Override
    public AgentResult<RootCauseResult> execute(RootCauseInput input, AgentContext context) {
        Instant started = Instant.now();
        RootCauseResult output = null;
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                RootCauseResult candidate = llm.generate(prompts.build(input), RootCauseResult.class);
                if (candidate != null && validator.validate(candidate).isEmpty()) {
                    output = new RootCauseResult(candidate.rootCause(), candidate.confidence(),
                            candidate.evidence(), candidate.alternativeHypotheses(), false);
                    break;
                }
            } catch (RuntimeException failure) {
                lastFailure = failure;
                // A second bounded attempt is allowed; deterministic diagnosis follows.
            }
        }
        boolean fallback = output == null;
        if (fallback) {
            log.warn("Root-cause LLM fallback after two attempts: {}", safeFailureCategory(lastFailure));
            if (runtimeStatus != null) runtimeStatus.record("FALLBACK");
            output = deterministic(input);
        }
        RootCauseResult result = output;
        List<Evidence> evidence = result.evidence().stream().map(line -> new Evidence(
                fallback ? "deterministic-fallback" : "validated-llm", line,
                input.incident().getDetectedAt(), Map.of("llmUnavailable", fallback))).toList();
        return new AgentResult<>("RootCauseAgent", result.rootCause(),
                new Confidence(result.confidence()), evidence, List.of(), started, Instant.now(),
                fallback ? AgentStatus.PARTIAL : AgentStatus.SUCCEEDED, result);
    }

    private String safeFailureCategory(RuntimeException failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof UpstreamServiceException upstream) {
                return upstream.getUpstreamStatus() == null
                        ? "UPSTREAM_ERROR"
                        : "UPSTREAM_HTTP_" + upstream.getUpstreamStatus();
            }
            current = current.getCause();
        }
        return failure == null ? "NO_RESPONSE" : failure.getClass().getSimpleName();
    }

    private RootCauseResult deterministic(RootCauseInput input) {
        String category = input.triage().category();
        String incidentType = input.incident().getType();
        String rootCause = category.contains("UPI") || incidentType.contains("UPI")
                ? "UPI issuer degradation"
                : category.contains("PROVIDER") || incidentType.contains("PROVIDER") ? "Payment provider outage"
                : "Concentrated payment failure pattern";
        double confidence = Math.min(0.60, input.analyst().confidence() * 0.60);
        List<String> evidence = new ArrayList<>();
        evidence.add("LLM unavailable after two bounded attempts; deterministic diagnosis used.");
        evidence.add("Fallback confidence %.4f equals 60%% of computed dominant failure-signature share %.4f, capped at 0.60."
                .formatted(confidence, input.analyst().confidence()));
        input.analyst().evidence().stream().limit(6).forEach(evidence::add);
        if (!input.memory().isEmpty()) {
            SimilarHistoricalIncident match = input.memory().get(0);
            evidence.add("Nearest stored incident similarity %.4f had root cause '%s', strategy %s and outcome %s."
                    .formatted(match.similarity(), match.rootCause(), match.strategy(), match.outcome()));
        }
        return new RootCauseResult(rootCause, confidence, evidence,
                List.of("Merchant-specific integration issue", "Transient customer-side failure"), true);
    }
}
