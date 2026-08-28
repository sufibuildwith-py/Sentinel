package com.sentinel.revenue.webhook;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebhookRequestHandlerTest {
    @Test void invalidAndMissingSignaturesAreAuditedWithoutParsingPayload() {
        WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(new RazorpayWebhookProperties("secret"));
        WebhookSecurityAuditService security = mock(WebhookSecurityAuditService.class);
        WebhookOutcomeProcessor processor = mock(WebhookOutcomeProcessor.class);
        WebhookRequestHandler handler = new WebhookRequestHandler(verifier, security, processor);
        byte[] piiBody = "{\"email\":\"private@example.com\"}".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> handler.handle(piiBody, null, "evt_1"))
                .isInstanceOf(InvalidWebhookSignatureException.class);
        verify(security).record(anyString(), eq(false), eq(true), eq("INVALID_SIGNATURE"));
        verifyNoInteractions(processor);
    }

    @Test void validSignatureRequiresEventIdAndSequentialDuplicateReturnsSafeResult() throws Exception {
        String secret = "secret";
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        WebhookSecurityAuditService security = mock(WebhookSecurityAuditService.class);
        WebhookOutcomeProcessor processor = mock(WebhookOutcomeProcessor.class);
        WebhookRequestHandler handler = new WebhookRequestHandler(
                new WebhookSignatureVerifier(new RazorpayWebhookProperties(secret)), security, processor);
        String signature = WebhookSignatureVerifierTest.sign(body, secret);
        assertThatThrownBy(() -> handler.handle(body, signature, null)).isInstanceOf(IllegalArgumentException.class);
        when(processor.alreadyProcessed("evt_1")).thenReturn(true);
        when(processor.duplicate("evt_1")).thenReturn(new WebhookResult("evt_1", "DUPLICATE", true, "safe"));
        assertThat(handler.handle(body, signature, "evt_1").duplicate()).isTrue();
        verify(processor, never()).process(anyString(), any(), anyString());
    }
}
