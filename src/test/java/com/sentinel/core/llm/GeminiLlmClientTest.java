package com.sentinel.core.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.core.config.GeminiProperties;
import com.sentinel.core.error.InvalidModelResponseException;
import com.sentinel.core.orchestration.InvestigationDraft;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiLlmClientTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();
    private final HttpClient httpClient = mock(HttpClient.class);
    private final HttpResponse<String> httpResponse = mock(HttpResponse.class);
    private GeminiLlmClient client;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() throws Exception {
        GeminiProperties properties = new GeminiProperties(
                "test-key",
                "test-model",
                "test-embedding-model",
                URI.create("https://example.test/v1beta"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(7)
        );
        client = new GeminiLlmClient(properties, httpClient, new ObjectMapper(), VALIDATOR);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        when(httpResponse.statusCode()).thenReturn(200);
    }

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void parsesAndValidatesStructuredOutput() {
        when(httpResponse.body()).thenReturn("""
                {"candidates":[{"content":{"parts":[{"text":"{\\"diagnosis\\":\\"Check the deployment.\\"}"}]}}]}
                """);

        InvestigationDraft result = client.generateStructured(prompt(), InvestigationDraft.class);

        assertThat(result.diagnosis()).isEqualTo("Check the deployment.");
    }

    @Test
    void rejectsStructuredOutputThatFailsBeanValidation() {
        when(httpResponse.body()).thenReturn("""
                {"candidates":[{"content":{"parts":[{"text":"{\\"diagnosis\\":\\"\\"}"}]}}]}
                """);

        assertThatThrownBy(() -> client.generateStructured(prompt(), InvestigationDraft.class))
                .isInstanceOf(InvalidModelResponseException.class)
                .hasMessageContaining("diagnosis");
    }

    private Prompt prompt() {
        return new Prompt(
                "Return a diagnosis.",
                "Investigate an incident.",
                Map.of(
                        "type", "object",
                        "properties", Map.of("diagnosis", Map.of("type", "string")),
                        "required", List.of("diagnosis")
                )
        );
    }
}
