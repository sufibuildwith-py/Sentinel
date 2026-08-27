package com.sentinel.revenue.investigation;

import com.sentinel.core.llm.Prompt;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptContextBuilderTest {
    @Test
    void masksCustomerAndStripsPiiCredentialsAndRawPaymentDetails() {
        RevenueIncident incident = new RevenueIncident("UPI_DEGRADATION",
                RevenueIncidentStatus.DETECTED, "HIGH", 125000, Instant.parse("2026-08-27T10:00:00Z"),
                List.of("pay_raw_9842"), List.of("real-customer-0182"),
                List.of("real-customer-0182 john@example.com +919876543210 password=hunter2 "
                        + "4111 1111 1111 1111 pay_secret_99 alice@okaxis"), null, null);
        PatternAnalysis pattern = new PatternAnalysis(null,
                List.of("Bank X failure share 73%. api_key=topsecret"), 0.73);
        CustomerContext context = new CustomerContext(1, 2, java.util.Map.of(), 0, null,
                List.of("Phone 9876543210"));
        RootCauseInput input = new RootCauseInput(incident,
                new TriageResult("PAYMENT_RAIL_DEGRADATION", "HIGH", "analyze", List.of(), List.of(), false),
                new AnalystFindings(pattern, context,
                        List.of("Bank X failure share 73%. api_key=topsecret", "Phone 9876543210"), 0.73),
                List.of());

        Prompt prompt = new PromptContextBuilder().build(input);

        assertThat(prompt.userMessage())
                .doesNotContain("real-customer-0182", "john@example.com", "+919876543210",
                        "9876543210", "hunter2", "topsecret", "4111 1111 1111 1111",
                        "pay_secret_99", "alice@okaxis", "pay_raw_9842")
                .contains("customer_0001", "[REDACTED_EMAIL]", "[REDACTED_PHONE]",
                        "[REDACTED_PAYMENT_DETAIL]", "[REDACTED_PAYMENT_ID]");
    }
}
