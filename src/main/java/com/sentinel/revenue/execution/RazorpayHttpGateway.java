package com.sentinel.revenue.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RazorpayHttpGateway implements RazorpayGateway {
    private final HttpClient client;
    private final ObjectMapper json;
    private final RazorpayProperties properties;
    private final CircuitBreaker circuitBreaker;
    private final IntervalFunction backoff = IntervalFunction.ofExponentialRandomBackoff(100, 2.0, 0.5);

    public RazorpayHttpGateway(@Qualifier("razorpayHttpClient") HttpClient client,
                               ObjectMapper json, RazorpayProperties properties) {
        this.client = client;
        this.json = json;
        this.properties = properties;
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.circuitBreakerFailureRate())
                .minimumNumberOfCalls(properties.circuitBreakerMinimumCalls())
                .slidingWindowSize(properties.circuitBreakerWindowSize())
                .waitDurationInOpenState(properties.circuitBreakerOpenDuration())
                .recordException(error -> error instanceof RazorpayFailure failure
                        && failure.kind() != RazorpayFailure.Kind.NON_RETRYABLE)
                .build();
        this.circuitBreaker = CircuitBreaker.of("razorpay-test-mode", config);
    }

    @Override public PaymentLinkResource createPaymentLink(PaymentLinkCommand command) {
        Map<String, Object> method = new LinkedHashMap<>();
        method.put("upi", command.hideUpi() ? 0 : 1);
        method.put("card", 1);
        method.put("netbanking", 1);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", command.amountMinor());
        payload.put("currency", command.currency());
        payload.put("accept_partial", false);
        payload.put("reference_id", command.referenceId());
        payload.put("description", command.description());
        payload.put("expire_by", command.expiresAt().getEpochSecond());
        payload.put("notify", Map.of("sms", command.notificationsEnabled(), "email", command.notificationsEnabled()));
        payload.put("reminder_enable", false);
        payload.put("notes", Map.of("sentinel_action", command.actionId().toString(),
                "customer_ref", command.maskedCustomerReference()));
        payload.put("options", Map.of("checkout", Map.of("method", method)));
        return link(send("POST", "/v1/payment_links", body(payload), false));
    }

    @Override public Optional<PaymentLinkResource> findPaymentLinkByReference(String referenceId) {
        String encoded = URLEncoder.encode(referenceId, StandardCharsets.UTF_8);
        JsonNode root = send("GET", "/v1/payment_links?reference_id=" + encoded, null, true);
        JsonNode items = root.path("items");
        if (!items.isArray() || items.isEmpty()) return Optional.empty();
        for (JsonNode item : items) {
            if (referenceId.equals(item.path("reference_id").asText())) return Optional.of(link(item));
        }
        return Optional.empty();
    }

    @Override public PaymentLinkResource fetchPaymentLink(String id) {
        return link(send("GET", "/v1/payment_links/" + segment(id), null, true));
    }
    @Override public PaymentLinkResource cancelPaymentLink(String id) {
        return link(send("POST", "/v1/payment_links/" + segment(id) + "/cancel", "{}", false));
    }
    @Override public void resendNotification(String id, String medium) {
        if (!properties.notificationsEnabled()) throw new IllegalStateException("Razorpay notifications are disabled");
        if (!"sms".equals(medium) && !"email".equals(medium)) throw new IllegalArgumentException("medium must be sms or email");
        send("POST", "/v1/payment_links/" + segment(id) + "/notify_by/" + medium, "{}", false);
    }
    @Override public ProviderPayment fetchPayment(String id) {
        JsonNode node = send("GET", "/v1/payments/" + segment(id), null, true);
        return new ProviderPayment(required(node, "id"), required(node, "status"),
                node.path("amount").asLong(), required(node, "currency"));
    }

    private JsonNode send(String method, String path, String body, boolean safeRead) {
        int attempts = safeRead ? properties.maximumAttempts() : 1;
        RazorpayFailure last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return circuitBreaker.executeSupplier(() -> exchange(method, path, body));
            } catch (CallNotPermittedException open) {
                throw new RazorpayFailure(RazorpayFailure.Kind.CIRCUIT_OPEN, "CIRCUIT_OPEN", open);
            } catch (RazorpayFailure failure) {
                last = failure;
                if (!safeRead || !failure.retryableRead() || attempt == attempts) throw failure;
                pause(attempt);
            }
        }
        throw last;
    }

    private JsonNode exchange(String method, String path, String body) {
        ensureEnabled();
        HttpRequest.Builder builder = HttpRequest.newBuilder(properties.endpoint(path))
                .timeout(properties.requestTimeout())
                .header("Accept", "application/json")
                .header("Authorization", basicAuth());
        if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) return parse(response.body());
            ProviderError providerError = providerError(response.statusCode(), response.body());
            if (response.statusCode() == 429 || response.statusCode() >= 500)
                throw new RazorpayFailure(RazorpayFailure.Kind.AMBIGUOUS, providerError);
            throw new RazorpayFailure(RazorpayFailure.Kind.NON_RETRYABLE, providerError);
        } catch (java.net.http.HttpTimeoutException timeout) {
            throw new RazorpayFailure(RazorpayFailure.Kind.AMBIGUOUS, "TIMEOUT", timeout);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new RazorpayFailure(RazorpayFailure.Kind.TEMPORARY, "INTERRUPTED", interrupted);
        } catch (IOException io) {
            throw new RazorpayFailure(RazorpayFailure.Kind.TEMPORARY, "IO_FAILURE", io);
        }
    }

    private JsonNode parse(String body) {
        try { return json.readTree(body); }
        catch (JsonProcessingException malformed) {
            throw new RazorpayFailure(RazorpayFailure.Kind.MALFORMED, "MALFORMED_RESPONSE", malformed);
        }
    }

    private ProviderError providerError(int status, String responseBody) {
        try {
            return ProviderError.from(status, json.readTree(responseBody));
        } catch (JsonProcessingException ignored) {
            return new ProviderError(status, "HTTP_" + status, null, null, null, null, null);
        }
    }
    private PaymentLinkResource link(JsonNode node) {
        return new PaymentLinkResource(required(node, "id"), required(node, "reference_id"),
                required(node, "short_url"), required(node, "status"));
    }
    private String required(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) throw new RazorpayFailure(RazorpayFailure.Kind.MALFORMED, "MALFORMED_RESPONSE");
        return value;
    }
    private String body(Map<String, Object> payload) {
        try { return json.writeValueAsString(payload); }
        catch (JsonProcessingException impossible) { throw new IllegalStateException("Could not encode Razorpay request"); }
    }
    private String basicAuth() {
        return "Basic " + Base64.getEncoder().encodeToString(
                (properties.keyId() + ":" + properties.keySecret()).getBytes(StandardCharsets.UTF_8));
    }
    private String segment(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Invalid Razorpay identifier");
        return value;
    }
    private void ensureEnabled() {
        if (!properties.enabled()) throw new IllegalStateException("Razorpay Test Mode execution is disabled");
    }
    private void pause(int attempt) {
        long millis = Math.max(1, backoff.apply(attempt));
        try { Thread.sleep(millis + ThreadLocalRandom.current().nextLong(Math.max(1, millis / 4))); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }
}
