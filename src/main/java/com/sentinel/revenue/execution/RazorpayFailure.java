package com.sentinel.revenue.execution;

public final class RazorpayFailure extends RuntimeException {
    public enum Kind { NON_RETRYABLE, TEMPORARY, AMBIGUOUS, CIRCUIT_OPEN, MALFORMED }
    private final Kind kind;
    private final String safeCode;

    public RazorpayFailure(Kind kind, String safeCode) {
        super("Razorpay request failed (" + safeCode + ")");
        this.kind = kind;
        this.safeCode = safeCode;
    }

    public RazorpayFailure(Kind kind, String safeCode, Throwable cause) {
        super("Razorpay request failed (" + safeCode + ")", cause);
        this.kind = kind;
        this.safeCode = safeCode;
    }

    public Kind kind() { return kind; }
    public String safeCode() { return safeCode; }
    public boolean retryableRead() { return kind == Kind.TEMPORARY || kind == Kind.AMBIGUOUS; }
}
