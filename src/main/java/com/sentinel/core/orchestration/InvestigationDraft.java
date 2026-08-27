package com.sentinel.core.orchestration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvestigationDraft(
        @NotBlank @Size(max = 4_000) String diagnosis
) {
}
