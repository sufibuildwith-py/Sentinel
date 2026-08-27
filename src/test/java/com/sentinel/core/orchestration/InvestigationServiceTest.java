package com.sentinel.core.orchestration;

import com.sentinel.core.config.RunbookProperties;
import com.sentinel.core.llm.EmbeddingClient;
import com.sentinel.core.llm.LlmClient;
import com.sentinel.core.llm.Prompt;
import com.sentinel.core.memory.MemoryMatch;
import com.sentinel.core.memory.RunbookDocument;
import com.sentinel.core.memory.RunbookMemory;
import com.sentinel.dto.InvestigationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvestigationServiceTest {

    private final EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
    private final RunbookMemory runbookMemory = mock(RunbookMemory.class);
    private final LlmClient llmClient = mock(LlmClient.class);
    private InvestigationService investigationService;

    @BeforeEach
    void setUp() {
        investigationService = new InvestigationService(
                embeddingClient,
                runbookMemory,
                llmClient,
                new RunbookProperties(Path.of("runbooks"), 1, -1.0)
        );
    }

    @Test
    void orchestratesEmbeddingRetrievalAndStructuredGeneration() {
        float[] incidentEmbedding = {1.0f, 0.0f};
        RunbookDocument document = new RunbookDocument(
                "incident-005-payment-provider-outage.txt",
                "Payment processor outage with no recent deployment.",
                new float[]{0.9f, 0.1f}
        );
        when(embeddingClient.embed("payments are suddenly failing"))
                .thenReturn(incidentEmbedding);
        when(runbookMemory.findSimilar(incidentEmbedding, 1, -1.0))
                .thenReturn(List.of(new MemoryMatch(document, 0.9939)));
        when(llmClient.generateStructured(any(Prompt.class), eq(InvestigationDraft.class)))
                .thenReturn(new InvestigationDraft("The payment provider may be degraded."));

        InvestigationResponse response = investigationService.investigate("payments are suddenly failing");

        assertThat(response.getDiagnosis()).isEqualTo("The payment provider may be degraded.");
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(llmClient).generateStructured(promptCaptor.capture(), eq(InvestigationDraft.class));
        assertThat(promptCaptor.getValue().userMessage())
                .contains("payments are suddenly failing")
                .contains("incident-005-payment-provider-outage.txt")
                .contains("Similarity: 0.9939")
                .contains("Payment processor outage");
        assertThat(promptCaptor.getValue().responseSchema()).containsEntry("type", "object");
    }
}
