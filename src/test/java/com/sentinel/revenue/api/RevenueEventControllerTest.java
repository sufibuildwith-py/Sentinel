package com.sentinel.revenue.api;

import com.sentinel.revenue.service.PaymentEventIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RevenueEventController.class)
class RevenueEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentEventIngestionService ingestionService;

    @Test
    void delegatesValidBatchToServiceAndReturnsSummary() throws Exception {
        when(ingestionService.ingest(any())).thenReturn(
                new BatchIngestionSummary(1, 1,
                        List.of(new BatchValidationError(2, "pay_bad", "currency", "invalid"))));

        mockMvc.perform(post("/api/v1/revenue/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "events": [{
                                    "paymentId": "pay_1",
                                    "orderId": "order_1",
                                    "customerId": "customer_1",
                                    "amountMinor": 10000,
                                    "currency": "INR",
                                    "method": "UPI",
                                    "status": "FAILED",
                                    "timestamp": "2026-01-15T09:00:00Z",
                                    "attemptNumber": 1,
                                    "previousFailureCount": 0,
                                    "metadata": {"synthetic": true}
                                  }]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.duplicatesSkipped").value(1))
                .andExpect(jsonPath("$.validationErrors[0].field").value("currency"));

        verify(ingestionService).ingest(any());
    }

    @Test
    void rejectsEmptyBatchBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/revenue/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\": []}"))
                .andExpect(status().isBadRequest());
    }
}
