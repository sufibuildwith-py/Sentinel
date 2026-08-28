package com.sentinel.revenue.execution;

public record ProviderPayment(String id, String status, long amountMinor, String currency) {
    public boolean isAlreadyPaid() {
        return status != null && switch (status.toLowerCase()) {
            case "authorized", "captured", "paid", "refunded" -> true;
            default -> false;
        };
    }
}
