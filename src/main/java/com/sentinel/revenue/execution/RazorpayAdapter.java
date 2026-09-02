package com.sentinel.revenue.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Entity;
import com.razorpay.Payment;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.sentinel.revenue.model.ProviderOrder;
import com.sentinel.revenue.model.ExecutionMode;
import com.sentinel.revenue.repository.ProviderOrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RazorpayAdapter {

    private static final String OFFICIAL_API = "https://api.razorpay.com";

    private final ObjectProvider<RazorpayClient> clients;
    private final RazorpayProperties properties;
    private final ProviderOrderRepository providerOrders;
    private final HttpClient httpClient;
    private final ObjectMapper json;

    public RazorpayAdapter(ObjectProvider<RazorpayClient> clients,
                           RazorpayProperties properties,
                           ProviderOrderRepository providerOrders,
                           @Qualifier("razorpayHttpClient") HttpClient httpClient,
                           ObjectMapper json) {
        this.clients = clients;
        this.properties = properties;
        this.providerOrders = providerOrders;
        this.httpClient = httpClient;
        this.json = json;
    }

    @Transactional
    @CircuitBreaker(name = "razorpay")
    public ProviderOrder createOrder(UUID incidentId, long amountPaise,
                                     String currency, String idempotencyKey) {
        Optional<ProviderOrder> existing = providerOrders.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        JSONObject response = properties.enabled()
                ? createOrderAtProvider(incidentId, amountPaise, currency, idempotencyKey)
                : stubOrder(amountPaise, currency, idempotencyKey);
        ProviderOrder order = new ProviderOrder(incidentId, response.getString("id"), amountPaise,
                currency, response.optString("status", "created").toUpperCase(),
                response.optString("receipt", reference(idempotencyKey)), idempotencyKey,
                executionMode());
        return persistIdempotently(order, idempotencyKey);
    }

    @Retry(name = "razorpay")
    @CircuitBreaker(name = "razorpay")
    public JSONObject fetchPayment(String razorpayPaymentId) {
        if (!properties.enabled()) {
            return new JSONObject().put("id", razorpayPaymentId).put("status", "failed")
                    .put("amount", 0).put("currency", "INR");
        }
        if (useSdk()) {
            return executeSdk(() -> clients.getObject().payments.fetch(razorpayPaymentId));
        }
        return exchange("GET", "/v1/payments/" + safeSegment(razorpayPaymentId), null);
    }

    @Transactional
    @CircuitBreaker(name = "razorpay")
    public JSONObject createPaymentLink(UUID incidentId, long amountPaise,
                                        String description, String idempotencyKey) {
        Optional<ProviderOrder> existing = providerOrders.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return paymentLink(existing.get());
        }

        JSONObject request = new JSONObject()
                .put("amount", amountPaise)
                .put("currency", "INR")
                .put("accept_partial", false)
                .put("reference_id", reference(idempotencyKey))
                .put("description", description)
                .put("expire_by", Instant.now().plus(properties.linkExpiry()).getEpochSecond())
                .put("reminder_enable", false)
                .put("notify", new JSONObject().put("sms", false).put("email", false))
                .put("notes", new JSONObject().put("sentinel_incident", incidentId.toString()));
        JSONObject response;
        if (!properties.enabled()) {
            response = new JSONObject()
                    .put("id", "plink_fixture_" + digest(idempotencyKey).substring(0, 12))
                    .put("reference_id", reference(idempotencyKey))
                    .put("short_url", "https://example.invalid/sentinel-test-link")
                    .put("status", "created");
        } else if (useSdk()) {
            response = executeSdk(() -> clients.getObject().paymentLink.create(request));
        } else {
            response = exchange("POST", "/v1/payment_links", request);
        }

        ProviderOrder link = new ProviderOrder(incidentId, response.getString("id"), amountPaise,
                "INR", response.optString("status", "created").toUpperCase(),
                response.optString("short_url", null), idempotencyKey, executionMode());
        ProviderOrder saved = persistIdempotently(link, idempotencyKey);
        return paymentLink(saved);
    }

    @Retry(name = "razorpay")
    @CircuitBreaker(name = "razorpay")
    public List<JSONObject> fetchActiveDowntimes() {
        if (!properties.enabled()) {
            return List.of();
        }
        if (useSdk()) {
            try {
                List<Payment> downtimes = clients.getObject().payments.fetchPaymentDowntime();
                return downtimes.stream().map(Entity::toJson).toList();
            } catch (RazorpayException failure) {
                throw providerFailure(failure);
            }
        }
        JSONObject response = exchange("GET", "/v1/payments/downtimes", null);
        JSONArray items = response.optJSONArray("items");
        if (items == null) {
            return List.of();
        }
        List<JSONObject> result = new ArrayList<>();
        for (int index = 0; index < items.length(); index++) {
            result.add(items.getJSONObject(index));
        }
        return List.copyOf(result);
    }

    private JSONObject createOrderAtProvider(UUID incidentId, long amountPaise,
                                             String currency, String idempotencyKey) {
        JSONObject request = new JSONObject()
                .put("amount", amountPaise)
                .put("currency", currency)
                .put("receipt", reference(idempotencyKey))
                .put("notes", new JSONObject().put("sentinel_incident", incidentId.toString()));
        if (useSdk()) {
            return executeSdk(() -> clients.getObject().orders.create(request));
        }
        return exchange("POST", "/v1/orders", request);
    }

    private JSONObject stubOrder(long amountPaise, String currency, String idempotencyKey) {
        return new JSONObject()
                .put("id", "order_fixture_" + digest(idempotencyKey).substring(0, 12))
                .put("amount", amountPaise)
                .put("currency", currency)
                .put("receipt", reference(idempotencyKey))
                .put("status", "created");
    }

    private <T extends Entity> JSONObject executeSdk(SdkCall<T> call) {
        try {
            return call.execute().toJson();
        } catch (RazorpayException failure) {
            throw providerFailure(failure);
        }
    }

    private RazorpayFailure providerFailure(RazorpayException failure) {
        ProviderError error = ProviderError.fromMessage(json, failure.getMessage());
        RazorpayFailure.Kind kind = error.httpStatus() >= 500 || error.httpStatus() == 429
                ? RazorpayFailure.Kind.TEMPORARY : RazorpayFailure.Kind.NON_RETRYABLE;
        return new RazorpayFailure(kind, error);
    }

    private JSONObject exchange(String method, String path, JSONObject body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(properties.endpoint(path))
                .timeout(properties.requestTimeout())
                .header("Accept", "application/json")
                .header("Authorization", basicAuth());
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body.toString()));
        }
        try {
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                RazorpayFailure.Kind kind = response.statusCode() >= 500 || response.statusCode() == 429
                        ? RazorpayFailure.Kind.TEMPORARY : RazorpayFailure.Kind.NON_RETRYABLE;
                throw new RazorpayFailure(kind, providerError(response.statusCode(), response.body()));
            }
            JsonNode parsed = json.readTree(response.body());
            return new JSONObject(parsed.toString());
        } catch (java.net.http.HttpTimeoutException timeout) {
            throw new RazorpayFailure(RazorpayFailure.Kind.AMBIGUOUS, "TIMEOUT", timeout);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new RazorpayFailure(RazorpayFailure.Kind.TEMPORARY, "INTERRUPTED", interrupted);
        } catch (IOException failure) {
            throw new RazorpayFailure(RazorpayFailure.Kind.TEMPORARY, "IO_FAILURE", failure);
        }
    }

    private ProviderError providerError(int status, String responseBody) {
        try {
            return ProviderError.from(status, json.readTree(responseBody));
        } catch (IOException ignored) {
            return new ProviderError(status, "HTTP_" + status, null, null, null, null, null);
        }
    }

    private ProviderOrder persistIdempotently(ProviderOrder order, String idempotencyKey) {
        try {
            return providerOrders.saveAndFlush(order);
        } catch (DataIntegrityViolationException duplicate) {
            return providerOrders.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> duplicate);
        }
    }

    private JSONObject paymentLink(ProviderOrder order) {
        return new JSONObject()
                .put("id", order.getRazorpayOrderId())
                .put("reference_id", reference(order.getIdempotencyKey()))
                .put("short_url", order.getProviderReference())
                .put("status", order.getStatus().toLowerCase());
    }

    private boolean useSdk() {
        return properties.baseUrl().toString().replaceAll("/+$", "").equals(OFFICIAL_API)
                && clients.getIfAvailable() != null;
    }

    private ExecutionMode executionMode() {
        return properties.enabled() ? ExecutionMode.RAZORPAY_TEST_MODE : ExecutionMode.SIMULATION;
    }

    private String reference(String idempotencyKey) {
        return "sntl_" + digest(idempotencyKey).substring(0, 32);
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private String basicAuth() {
        return "Basic " + Base64.getEncoder().encodeToString(
                (properties.keyId() + ':' + properties.keySecret()).getBytes(StandardCharsets.UTF_8));
    }

    private String safeSegment(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid Razorpay identifier");
        }
        return value;
    }

    @FunctionalInterface
    private interface SdkCall<T extends Entity> {
        T execute() throws RazorpayException;
    }
}
