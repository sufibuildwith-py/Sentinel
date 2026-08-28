package com.sentinel.evaluation;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class SensitiveDataScanner {
    private static final List<Pattern> FORBIDDEN = List.of(
            Pattern.compile("(?i)rzp_(?:live|test)_[a-z0-9]{8,}"),
            Pattern.compile("(?i)api[_-]?key\\s*[:=]"),
            Pattern.compile("(?i)webhook[_-]?secret\\s*[:=]"),
            Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?<!\\d)(?:\\+?91[- ]?)?[6-9]\\d{9}(?!\\d)"),
            Pattern.compile("(?i)customer_[0-9]{4,}"),
            Pattern.compile("(?i)\\bpay_(?!load)[a-z0-9_]{6,}"));

    public List<String> findings(String value) {
        if (value == null || value.isBlank()) return List.of();
        return FORBIDDEN.stream().filter(pattern -> pattern.matcher(value).find())
                .map(Pattern::pattern).toList();
    }
}
