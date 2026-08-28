package com.sentinel.revenue.webhook;

import org.junit.jupiter.api.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureVerifierTest {
    @Test void validatesTheExactRawBytesAndRejectsNormalizedJson() throws Exception {
        String secret = "test_webhook_secret";
        byte[] raw = "{\n  \"event\": \"payment_link.paid\"\n}".getBytes(StandardCharsets.UTF_8);
        WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(new RazorpayWebhookProperties(secret));
        String signature = sign(raw, secret);
        assertThat(verifier.verify(raw, signature)).isTrue();
        assertThat(verifier.verify("{\"event\":\"payment_link.paid\"}".getBytes(StandardCharsets.UTF_8), signature)).isFalse();
        assertThat(verifier.verify(raw, "not-hex")).isFalse();
    }
    static String sign(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }
}
