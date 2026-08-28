package com.sentinel.revenue.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HumanDecisionRequest(
        @NotBlank @Size(max = 128) String actor,
        @NotBlank @Size(max = 1000) String reason) {
}
