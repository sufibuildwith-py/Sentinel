package com.sentinel.revenue.communication;
import org.springframework.stereotype.Component;
import java.util.Locale;
@Component
public class CustomerIntentValidator {
    public CustomerIntent validate(String proposedIntent) {
        if (proposedIntent == null || proposedIntent.isBlank()) return CustomerIntent.UNKNOWN;
        try { return CustomerIntent.valueOf(proposedIntent.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException unsupported) { return CustomerIntent.UNKNOWN; }
    }
}
