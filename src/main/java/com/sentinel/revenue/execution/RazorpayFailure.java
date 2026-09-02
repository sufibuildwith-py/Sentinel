package com.sentinel.revenue.execution;

public final class RazorpayFailure extends RuntimeException {
    public enum Kind { NON_RETRYABLE, TEMPORARY, AMBIGUOUS, CIRCUIT_OPEN, MALFORMED }
    private final Kind kind;
    private final String safeCode;
    private final ProviderError providerError;

    public RazorpayFailure(Kind kind, String safeCode) {
        this(kind, safeCode, null, null);
    }

    public RazorpayFailure(Kind kind, String safeCode, Throwable cause) {
        this(kind, safeCode, null, cause);
    }

    public RazorpayFailure(Kind kind, ProviderError providerError) {
        this(kind, providerError.safeCode(), providerError, null);
    }

    private RazorpayFailure(Kind kind, String safeCode, ProviderError providerError, Throwable cause) {
        super("Razorpay request failed (" + safeCode + ")", cause);
        this.kind = kind;
        this.safeCode = safeCode;
        this.providerError = providerError;
    }

    public Kind kind() { return kind; }
    public String safeCode() { return safeCode; }
    public ProviderError providerError() { return providerError; }
    public String safeReason() { return providerError == null ? safeCode : providerError.safeSummary(); }
    public boolean retryableRead() { return kind == Kind.TEMPORARY || kind == Kind.AMBIGUOUS; }
}
