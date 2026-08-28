package com.sentinel.revenue.webhook;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WebhookSignatureVerifier {
    private final RazorpayWebhookProperties properties;
    public WebhookSignatureVerifier(RazorpayWebhookProperties properties) { this.properties = properties; }

    public boolean verify(byte[] rawBody, String suppliedHex) {
        if (!properties.configured() || suppliedHex == null || suppliedHex.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] supplied = HexFormat.of().parseHex(suppliedHex.trim());
            return MessageDigest.isEqual(mac.doFinal(rawBody), supplied);
        } catch (Exception invalid) { return false; }
    }

    public String digest(byte[] rawBody) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawBody)); }
        catch (Exception impossible) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
}
