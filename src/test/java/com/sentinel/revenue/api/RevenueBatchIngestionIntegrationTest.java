package com.sentinel.revenue.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.core.llm.LlmClient;
import com.sentinel.core.memory.RunbookMemory;
import com.sentinel.revenue.dataset.SyntheticPaymentDatasetGenerator;
import com.sentinel.revenue.model.FindingSource;
import com.sentinel.revenue.repository.IncidentFindingRepository;
import com.sentinel.revenue.repository.PaymentEventRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "gemini.api-key=test-key")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RevenueBatchIngestionIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PaymentEventRepository paymentEvents;
    @Autowired RevenueIncidentRepository incidents;
    @Autowired IncidentFindingRepository findings;
    @Autowired JdbcTemplate jdbcTemplate;

    @MockBean RunbookMemory runbookMemory;
    @MockBean LlmClient llmClient;

    @BeforeEach
    void resetDemoState() throws Exception {
        mockMvc.perform(post("/api/v1/demo/reset"))
                .andExpect(status().isOk());
    }

    @Test
    void postingSyntheticDatasetPersistsUniqueEventsAndIsRepeatablyIdempotent()
            throws Exception {
        String batchJson = objectMapper.writeValueAsString(
                new SyntheticPaymentDatasetGenerator().generate());

        mockMvc.perform(post("/api/v1/revenue/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(285))
                .andExpect(jsonPath("$.duplicatesSkipped").value(15))
                .andExpect(jsonPath("$.validationErrors").isEmpty());

        assertThat(paymentEvents.count()).isEqualTo(285);
        assertThat(incidents.findAll())
                .extracting(incident -> incident.getType())
                .containsExactlyInAnyOrder("UPI_DEGRADATION", "PROVIDER_OUTAGE");
        assertThat(incidents.findAll())
                .noneMatch(incident -> "NORMAL_FAILURE_MIX".equals(incident.getType()));
        var activeFindings = incidents.findAll().stream()
                .flatMap(incident -> findings.findAllByIncidentIncidentId(incident.getIncidentId()).stream())
                .toList();
        assertThat(activeFindings).hasSize(2).allSatisfy(finding -> {
            assertThat(finding.getSource()).isEqualTo(FindingSource.DETECTOR);
            assertThat(finding.getSummary())
                    .contains("contributing failed events")
                    .contains("minor units at risk")
                    .contains("detection rules passed");
            assertThat(finding.getEvidence())
                    .anyMatch(line -> line.startsWith("PASS MINIMUM_FAILED_VOLUME"))
                    .anyMatch(line -> line.startsWith("PASS MINIMUM_SUCCESS_RATE_DROP"))
                    .anyMatch(line -> line.startsWith("PASS MINIMUM_BASELINE_DEVIATION"))
                    .anyMatch(line -> line.startsWith("PASS MINIMUM_AMOUNT_AT_RISK"))
                    .anyMatch(line -> line.startsWith("Contributing event IDs ("));
        });

        mockMvc.perform(post("/api/v1/revenue/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.duplicatesSkipped").value(300))
                .andExpect(jsonPath("$.validationErrors").isEmpty());

        assertThat(incidents.count()).isEqualTo(2);
    }

    @Test
    void demoUpiInjectionUsesRealIngestionDetectionAndPersistencePipeline()
            throws Exception {
        MvcResult injection = mockMvc.perform(post("/api/v1/demo/inject/upi-outage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingestion.count").value(30))
                .andExpect(jsonPath("$.ingestion.duplicatesSkipped").value(0))
                .andExpect(jsonPath("$.incidentsCreated").value(1))
                .andExpect(jsonPath("$.incidents[0].type").value("UPI_DEGRADATION"))
                .andExpect(jsonPath("$.incidents[0].affectedPaymentCount").value(24))
                .andReturn();

        assertThat(paymentEvents.count()).isEqualTo(30);
        assertThat(incidents.count()).isEqualTo(1);
        UUID injectedIncidentId = UUID.fromString(objectMapper.readTree(
                injection.getResponse().getContentAsString()).at("/incidents/0/incidentId").asText());
        assertThat(findings.findAllByIncidentIncidentId(injectedIncidentId)).singleElement().satisfies(finding ->
                assertThat(finding.getSummary()).contains("all 4 detection rules passed"));
    }

    @Test
    void demoResetHidesSyntheticStatePreservesAuditAndAllowsDeterministicReplay() throws Exception {
        mockMvc.perform(post("/api/v1/demo/inject/upi-outage"))
                .andExpect(status().isOk());
        Integer auditBefore = jdbcTemplate.queryForObject("select count(*) from audit_events", Integer.class);

        mockMvc.perform(post("/api/v1/demo/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentsReset").value(1))
                .andExpect(jsonPath("$.eventsReset").value(30))
                .andExpect(jsonPath("$.auditHistoryPreserved").value(true));

        assertThat(incidents.count()).isZero();
        assertThat(paymentEvents.count()).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_events", Integer.class))
                .isEqualTo(auditBefore);

        mockMvc.perform(post("/api/v1/demo/inject/upi-outage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingestion.count").value(30))
                .andExpect(jsonPath("$.incidentsCreated").value(1));
    }
}
