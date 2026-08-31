package com.sentinel.revenue.api;

import com.sentinel.revenue.service.DemoRevenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemoController.class)
class DemoControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean DemoRevenueService demoRevenueService;

    @Test
    void resetDelegatesToDemoService() throws Exception {
        when(demoRevenueService.resetSyntheticState()).thenReturn(new DemoResetResponse(
                2, 285, true, "Synthetic operational state reset; append-only audit and evaluation history preserved"));

        mockMvc.perform(post("/api/v1/demo/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentsReset").value(2))
                .andExpect(jsonPath("$.eventsReset").value(285))
                .andExpect(jsonPath("$.auditHistoryPreserved").value(true));

        verify(demoRevenueService).resetSyntheticState();
    }

    @Test
    void injectionReturnsPersistedIncidentSummary() throws Exception {
        UUID incidentId = UUID.randomUUID();
        when(demoRevenueService.injectUpiOutage()).thenReturn(new DemoInjectionResponse(
                new BatchIngestionSummary(30, 0, List.of()),
                1,
                List.of(new DemoIncidentSummary(
                        incidentId, "UPI_DEGRADATION", 240_000, 24))));

        mockMvc.perform(post("/api/v1/demo/inject/upi-outage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingestion.count").value(30))
                .andExpect(jsonPath("$.incidentsCreated").value(1))
                .andExpect(jsonPath("$.incidents[0].incidentId")
                        .value(incidentId.toString()))
                .andExpect(jsonPath("$.incidents[0].type").value("UPI_DEGRADATION"));
    }
}
