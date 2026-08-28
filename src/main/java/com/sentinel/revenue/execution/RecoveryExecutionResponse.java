package com.sentinel.revenue.execution;

import com.sentinel.revenue.model.RecoveryActionStatus;

import java.util.UUID;

public record RecoveryExecutionResponse(UUID incidentId, UUID actionId,
                                        RecoveryActionStatus actionStatus,
                                        String providerId, String referenceId,
                                        String shortUrl, String providerStatus,
                                        String mode, boolean existing,
                                        String message) { }
