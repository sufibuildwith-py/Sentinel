package com.sentinel.revenue.investigation;

import com.sentinel.core.llm.Prompt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class PromptContextBuilder {
    private static final Pattern EMAIL = Pattern.compile("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?91[-\\s]?)?[6-9]\\d{9}(?!\\d)");
    private static final Pattern CARD = Pattern.compile("(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)");
    private static final Pattern CREDENTIAL = Pattern.compile(
            "(?i)(password|passwd|secret|api[_ -]?key|access[_ -]?token|authorization)\\s*[:=]\\s*[^,;\\s]+"
    );
    private static final Pattern UPI_ID = Pattern.compile("(?i)\\b[a-z0-9._-]{2,}@[a-z]{2,}\\b");
    private static final Pattern RAW_PAYMENT_ID = Pattern.compile("(?i)\\b(?:pay|order|plink)_[a-z0-9_-]+\\b");

    public Prompt build(RootCauseInput input) {
        Map<String, String> customerAliases = aliases(input.incident().getAffectedCustomers());
        List<String> lines = new ArrayList<>();
        lines.add("Incident category: " + clean(input.triage().category(), customerAliases));
        lines.add("Severity: " + clean(input.incident().getSeverity(), customerAliases));
        lines.add("Affected payment count: " + input.incident().getAffectedPayments().size());
        lines.add("Affected customer aliases: " + customerAliases.values());
        lines.add("Amount at risk (minor units): " + input.incident().getAmountAtRiskMinor());
        lines.add("Detection evidence:");
        input.incident().getEvidence().stream().filter(line -> !line.startsWith("Contributing event IDs"))
                .map(line -> clean(line, customerAliases)).forEach(line -> lines.add("- " + line));
        lines.add("Computed analyst evidence:");
        input.analyst().evidence().stream().map(line -> clean(line, customerAliases))
                .forEach(line -> lines.add("- " + line));
        lines.add("Historical matches: " + input.memory().size());
        input.memory().forEach(match -> lines.add("- rootCause=%s; strategy=%s; outcome=%s; recoveredMinor=%d; recoveryRate=%s; similarity=%s"
                .formatted(clean(match.rootCause(), customerAliases), match.strategy(), match.outcome(),
                        match.recoveredAmountMinor(), match.recoveryRate() == null ? "unavailable"
                                : decimal(match.recoveryRate()), decimal(match.similarity()))));

        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "rootCause", Map.of("type", "string", "minLength", 1),
                        "confidence", Map.of("type", "number", "minimum", 0, "maximum", 1),
                        "evidence", Map.of("type", "array", "minItems", 1, "maxItems", 20,
                                "items", Map.of("type", "string", "minLength", 1)),
                        "alternativeHypotheses", Map.of("type", "array", "maxItems", 10,
                                "items", Map.of("type", "string", "minLength", 1))),
                "required", List.of("rootCause", "confidence", "evidence", "alternativeHypotheses"));
        return new Prompt("You are Sentinel's diagnostic agent. Diagnose only; never propose, approve, or execute financial actions. Use only the supplied computed evidence. Return the requested incident-report JSON.",
                String.join("\n", lines), schema);
    }

    private Map<String, String> aliases(List<String> customerIds) {
        Map<String, String> result = new LinkedHashMap<>();
        customerIds.stream().distinct().sorted().forEach(id ->
                result.put(id, "customer_%04d".formatted(result.size() + 1)));
        return result;
    }

    String clean(String value, Map<String, String> customerAliases) {
        String sanitized = value == null ? "" : value;
        for (Map.Entry<String, String> alias : customerAliases.entrySet()) {
            sanitized = sanitized.replace(alias.getKey(), alias.getValue());
        }
        sanitized = CREDENTIAL.matcher(sanitized).replaceAll("$1=[REDACTED]");
        sanitized = EMAIL.matcher(sanitized).replaceAll("[REDACTED_EMAIL]");
        sanitized = UPI_ID.matcher(sanitized).replaceAll("[REDACTED_UPI_ID]");
        sanitized = PHONE.matcher(sanitized).replaceAll("[REDACTED_PHONE]");
        sanitized = CARD.matcher(sanitized).replaceAll("[REDACTED_PAYMENT_DETAIL]");
        return RAW_PAYMENT_ID.matcher(sanitized).replaceAll("[REDACTED_PAYMENT_ID]");
    }

    private String decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }
}
