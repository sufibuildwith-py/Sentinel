package com.sentinel.revenue.api;

import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.service.RevenueOperationsReadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RevenueOperationsController.class)
class RevenueOperationsControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean RevenueOperationsReadService reads;

    @Test
    void exposesDashboardSafeIncidentSummary() throws Exception {
        UUID id = UUID.randomUUID();
        when(reads.incidents()).thenReturn(List.of(new IncidentSummaryView(id, "UPI_DEGRADATION",
                RevenueIncidentStatus.DETECTED, "HIGH", 284000, Instant.parse("2026-08-28T10:00:00Z"),
                41, 38, null, null, null, null, 0)));

        mockMvc.perform(get("/api/v1/revenue/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].incidentId").value(id.toString()))
                .andExpect(jsonPath("$[0].amountAtRiskMinor").value(284000))
                .andExpect(jsonPath("$[0].affectedPaymentCount").value(41))
                .andExpect(jsonPath("$[0].paymentId").doesNotExist())
                .andExpect(jsonPath("$[0].customerId").doesNotExist());
    }
}
