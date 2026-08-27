package com.sentinel.controller;

import com.sentinel.dto.IncidentRequest;
import com.sentinel.dto.InvestigationResponse;
import com.sentinel.core.orchestration.InvestigationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InvestigateController {

    private final InvestigationService investigationService;

    public InvestigateController(InvestigationService investigationService) {
        this.investigationService = investigationService;
    }

    @PostMapping("/investigate")
    public InvestigationResponse investigate(@Valid @RequestBody IncidentRequest request) {
        return investigationService.investigate(request.getIncident());
    }
}
