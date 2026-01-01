package com.example.dapprototype.controller;

import com.example.dapprototype.config.OpenApiValidatorConfig;
import com.example.dapprototype.mapper.RequestMapperImpl;
import com.example.dapprototype.model.RequestInfo;
import com.example.dapprototype.model.RequestPayload;
import com.example.dapprototype.service.OpenApiRequestValidator;
import com.example.dapprototype.service.RequestProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RequestController.class)
@Import({RequestProcessingService.class, OpenApiRequestValidator.class, OpenApiValidatorConfig.class, RequestMapperImpl.class})
class RequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/request returns success payload")
    void submitRequestReturnsSuccess() throws Exception {
        RequestPayload payload = new RequestPayload(new RequestInfo("abcd", "2025-12-30T13:36:00Z"));

        mockMvc.perform(post("/api/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Request processed successfully"));
    }

    @Test
    @DisplayName("POST /api/request with missing body is 400")
    void submitRequestMissingBody() throws Exception {
        mockMvc.perform(post("/api/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
