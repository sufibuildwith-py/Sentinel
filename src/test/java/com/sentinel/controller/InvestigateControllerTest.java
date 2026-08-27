package com.sentinel.controller;

import com.sentinel.core.error.UpstreamTimeoutException;
import com.sentinel.core.orchestration.InvestigationService;
import com.sentinel.dto.InvestigationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.net.http.HttpTimeoutException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvestigateController.class)
class InvestigateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvestigationService investigationService;

    @Test
    void preservesInvestigationEndpointContract() throws Exception {
        when(investigationService.investigate("checkout API returning 500s"))
                .thenReturn(new InvestigationResponse("Check the latest checkout deployment."));

        mockMvc.perform(post("/investigate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"incident":"checkout API returning 500s"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.diagnosis").value("Check the latest checkout deployment."));

        verify(investigationService).investigate("checkout API returning 500s");
    }

    @Test
    void rejectsBlankIncidentsWithAConsistentError() throws Exception {
        mockMvc.perform(post("/investigate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"incident":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/investigate"))
                .andExpect(jsonPath("$.violations[0].field").value("incident"));
    }

    @Test
    void rejectsMalformedJsonWithAConsistentError() throws Exception {
        mockMvc.perform(post("/investigate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request body is malformed"));
    }

    @Test
    void mapsUpstreamTimeoutWithoutLeakingDetails() throws Exception {
        when(investigationService.investigate("database timeout"))
                .thenThrow(new UpstreamTimeoutException(
                        "Gemini generation",
                        new HttpTimeoutException("sensitive upstream detail")
                ));

        mockMvc.perform(post("/investigate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"incident":"database timeout"}
                                """))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("UPSTREAM_TIMEOUT"))
                .andExpect(jsonPath("$.message").value("An upstream service timed out"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("sensitive upstream detail"))));
    }
}
