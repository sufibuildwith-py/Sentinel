package com.sentinel.revenue.execution;

import java.time.Instant;
import java.util.UUID;

public record PaymentLinkCommand(long amountMinor, String currency, String referenceId,
                                 String description, Instant expiresAt, UUID actionId,
                                 String maskedCustomerReference, boolean hideUpi,
                                 boolean notificationsEnabled) { }
