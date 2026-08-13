package com.sentinel.dto;

// What we send back: { "diagnosis": "..." }
public class InvestigationResponse {

    private String diagnosis;

    public InvestigationResponse(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }
}
