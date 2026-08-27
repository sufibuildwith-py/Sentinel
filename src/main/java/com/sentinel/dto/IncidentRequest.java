package com.sentinel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class IncidentRequest {

    @NotBlank(message = "incident must not be blank")
    @Size(max = 4_000, message = "incident must not exceed 4000 characters")
    private String incident;

    public IncidentRequest() {
        // Required by Jackson.
    }

    public String getIncident() {
        return incident;
    }

    public void setIncident(String incident) {
        this.incident = incident;
    }
}
