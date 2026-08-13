package com.sentinel.dto;

// This is what the client sends us: { "incident": "checkout API returning 500s" }
public class IncidentRequest {

    private String incident;

    public IncidentRequest() {
        // needed by Jackson to build this object from JSON
    }

    public String getIncident() {
        return incident;
    }

    public void setIncident(String incident) {
        this.incident = incident;
    }
}
