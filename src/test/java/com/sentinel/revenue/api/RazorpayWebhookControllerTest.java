package com.sentinel.revenue.api;

import com.sentinel.revenue.webhook.WebhookRequestHandler;
import com.sentinel.revenue.webhook.WebhookResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import com.sentinel.core.error.GlobalExceptionHandler;
import com.sentinel.revenue.webhook.InvalidWebhookSignatureException;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RazorpayWebhookController.class)
@Import(GlobalExceptionHandler.class)
class RazorpayWebhookControllerTest {
    @Autowired MockMvc mvc;
    @MockBean WebhookRequestHandler handler;
    @Test void passesTheUnmodifiedRawBodyAndHeadersToValidationBoundary() throws Exception {
        byte[] raw = "{\n  \"event\" : \"payment_link.paid\"\n}".getBytes(StandardCharsets.UTF_8);
        when(handler.handle(any(), eq("signature"), eq("event_1")))
                .thenReturn(new WebhookResult("event_1", "APPLIED", false, "ok"));
        mvc.perform(post("/api/v1/webhooks/razorpay").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "signature")
                        .header("X-Razorpay-Event-Id", "event_1").content(raw))
                .andExpect(status().isOk()).andExpect(jsonPath("$.disposition").value("APPLIED"));
        verify(handler).handle(argThat(bytes -> java.util.Arrays.equals(bytes, raw)),
                eq("signature"), eq("event_1"));
    }

    @Test void invalidSignatureIs401AndMissingEventIdIs400() throws Exception {
        when(handler.handle(any(), isNull(), eq("event_1"))).thenThrow(new InvalidWebhookSignatureException());
        mvc.perform(post("/api/v1/webhooks/razorpay").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Event-Id", "event_1").content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
        when(handler.handle(any(), eq("signature"), isNull()))
                .thenThrow(new IllegalArgumentException("X-Razorpay-Event-Id is required"));
        mvc.perform(post("/api/v1/webhooks/razorpay").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "signature").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
