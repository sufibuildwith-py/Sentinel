package com.sentinel.revenue.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sentinel.revenue.api.PaymentEventBatchRequest;
import com.sentinel.revenue.api.PaymentEventRequest;
import net.datafaker.Faker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class SyntheticPaymentDatasetGenerator {

    public static final long SEED = 20_260_827L;
    public static final int EVENTS_PER_SCENARIO = 30;

    private static final Instant BASE_TIME = Instant.parse("2026-01-15T09:00:00Z");
    private static final String[] METHODS = {"UPI", "CARD", "NETBANKING", "WALLET"};
    private static final String[] BANKS = {"HDFC", "ICICI", "SBI", "AXIS", "KOTAK"};

    private final Random random;
    private final Faker faker;

    public SyntheticPaymentDatasetGenerator() {
        this(SEED);
    }

    public SyntheticPaymentDatasetGenerator(long seed) {
        this.random = new Random(seed);
        this.faker = new Faker(Locale.forLanguageTag("en-IN"), random);
    }

    public PaymentEventBatchRequest generate() {
        List<PaymentEventRequest> events = new ArrayList<>(
                Scenario.values().length * EVENTS_PER_SCENARIO);
        for (Scenario scenario : Scenario.values()) {
            events.addAll(generateScenario(scenario).events());
        }
        return new PaymentEventBatchRequest(events);
    }

    public PaymentEventBatchRequest generateScenario(Scenario scenario) {
        List<PaymentEventRequest> events = new ArrayList<>(EVENTS_PER_SCENARIO);
        int sequenceStart = scenario.ordinal() * EVENTS_PER_SCENARIO;
        for (int scenarioIndex = 0; scenarioIndex < EVENTS_PER_SCENARIO; scenarioIndex++) {
            events.add(eventFor(scenario, scenarioIndex, sequenceStart + scenarioIndex));
        }
        return new PaymentEventBatchRequest(events);
    }

    private PaymentEventRequest eventFor(Scenario scenario, int scenarioIndex, int sequence) {
        String paymentId = scenario == Scenario.DUPLICATE
                ? "pay_duplicate_" + String.format("%03d", scenarioIndex / 2)
                : "pay_" + scenario.name().toLowerCase(Locale.ROOT) + "_"
                        + String.format("%03d", scenarioIndex);
        String method = methodFor(scenario, scenarioIndex);
        String status = statusFor(scenario, scenarioIndex);
        String errorCode = errorCodeFor(scenario, status, scenarioIndex);
        long amountMinor = amountFor(scenario);
        int attemptNumber = scenario == Scenario.CUSTOMER_ABANDONMENT
                ? 1 + scenarioIndex % 3 : 1;

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("synthetic", true);
        metadata.put("groundTruthLabel", scenario.name());
        metadata.put("expectedAnomaly", scenario.expectedAnomaly);
        metadata.put("datasetSeed", SEED);
        metadata.put("merchantId", "merchant_demo");
        if (scenario == Scenario.ALREADY_PAID) {
            metadata.put("alreadyPaid", true);
        }
        if (scenario == Scenario.DUPLICATE) {
            metadata.put("intentionalDuplicate", scenarioIndex % 2 == 1);
        }
        if (scenario == Scenario.API_FAILURE) {
            metadata.put("upstreamHttpStatus", 503);
        }

        return new PaymentEventRequest(
                paymentId,
                "order_" + String.format("%06d", sequence),
                faker.regexify("customer_[0-9]{6}"),
                amountMinor,
                "INR",
                method,
                issuerFor(scenario),
                status,
                errorCode,
                errorDescription(errorCode),
                BASE_TIME.plus(sequence, ChronoUnit.MINUTES),
                attemptNumber,
                previousSuccessfulMethod(scenario),
                previousFailureCount(scenario, scenarioIndex),
                scenarioIndex % 7 == 0 ? "subscription_" + String.format("%04d", sequence) : null,
                metadata);
    }

    private String methodFor(Scenario scenario, int index) {
        return switch (scenario) {
            case UPI_DEGRADATION, CUSTOMER_ABANDONMENT, ALREADY_PAID -> "UPI";
            case PROVIDER_OUTAGE, INSUFFICIENT_FUNDS, HIGH_VALUE -> "CARD";
            default -> METHODS[index % METHODS.length];
        };
    }

    private String issuerFor(Scenario scenario) {
        return switch (scenario) {
            case UPI_DEGRADATION -> "HDFC";
            case PROVIDER_OUTAGE -> "RAZORPAY_GATEWAY";
            default -> BANKS[random.nextInt(BANKS.length)];
        };
    }

    private String statusFor(Scenario scenario, int index) {
        return switch (scenario) {
            case UPI_DEGRADATION -> index % 5 == 0 ? "CAPTURED" : "FAILED";
            case PROVIDER_OUTAGE, INSUFFICIENT_FUNDS, API_FAILURE -> "FAILED";
            case NORMAL_FAILURE_MIX -> index % 5 == 0 ? "FAILED" : "CAPTURED";
            case CUSTOMER_ABANDONMENT -> "ABANDONED";
            case MIXED_METHOD_DEGRADATION -> index % 3 == 0 ? "CAPTURED" : "FAILED";
            case ALREADY_PAID -> "CAPTURED";
            case HIGH_VALUE -> index % 4 == 0 ? "FAILED" : "AUTHORIZED";
            case DUPLICATE -> "FAILED";
        };
    }

    private String errorCodeFor(Scenario scenario, String status, int index) {
        if (!"FAILED".equals(status)) {
            return null;
        }
        return switch (scenario) {
            case UPI_DEGRADATION -> "UPI_ISSUER_UNAVAILABLE";
            case PROVIDER_OUTAGE -> "PROVIDER_UNAVAILABLE";
            case NORMAL_FAILURE_MIX -> switch (index % 3) {
                case 0 -> "PAYMENT_DECLINED";
                case 1 -> "NETWORK_ERROR";
                default -> "BAD_REQUEST_ERROR";
            };
            case INSUFFICIENT_FUNDS -> "INSUFFICIENT_FUNDS";
            case MIXED_METHOD_DEGRADATION -> "PAYMENT_RAIL_DEGRADED";
            case HIGH_VALUE -> "RISK_REVIEW_REQUIRED";
            case DUPLICATE -> "NETWORK_ERROR";
            case API_FAILURE -> "RAZORPAY_API_FAILURE";
            default -> null;
        };
    }

    private long amountFor(Scenario scenario) {
        if (scenario == Scenario.HIGH_VALUE) {
            return 500_000L + random.nextInt(1_500_001);
        }
        return 10_000L + random.nextInt(90_001);
    }

    private String previousSuccessfulMethod(Scenario scenario) {
        return switch (scenario) {
            case UPI_DEGRADATION, CUSTOMER_ABANDONMENT -> "CARD";
            default -> null;
        };
    }

    private int previousFailureCount(Scenario scenario, int index) {
        return switch (scenario) {
            case CUSTOMER_ABANDONMENT -> 2 + index % 4;
            case INSUFFICIENT_FUNDS, UPI_DEGRADATION -> 1 + index % 3;
            default -> 0;
        };
    }

    private String errorDescription(String errorCode) {
        return errorCode == null ? null : errorCode.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(new SyntheticPaymentDatasetGenerator().generate()));
    }

    public enum Scenario {
        UPI_DEGRADATION(true),
        PROVIDER_OUTAGE(true),
        NORMAL_FAILURE_MIX(false),
        INSUFFICIENT_FUNDS(false),
        CUSTOMER_ABANDONMENT(false),
        MIXED_METHOD_DEGRADATION(false),
        ALREADY_PAID(false),
        HIGH_VALUE(false),
        DUPLICATE(false),
        API_FAILURE(false);

        private final boolean expectedAnomaly;

        Scenario(boolean expectedAnomaly) {
            this.expectedAnomaly = expectedAnomaly;
        }
    }
}
