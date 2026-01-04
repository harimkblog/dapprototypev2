package com.example.dapprototype.controller;

import com.example.dapprototype.classloader.TxnClassLoaderService;
import com.example.dapprototype.config.OpenApiValidatorConfig;
import com.example.dapprototype.service.MockCustomerAPI;
import com.example.dapprototype.service.OpenApiRequestValidator;
import com.example.dapprototype.service.RequestProcessingService;
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
@Import({RequestProcessingService.class, OpenApiRequestValidator.class, OpenApiValidatorConfig.class, TxnClassLoaderService.class, MockCustomerAPI.class})
class RequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/request returns success payload")
    void submitRequestReturnsSuccess() throws Exception {
        String validJson = "{\"activityId\": \"abcd\", \"activityTimeStamp\": \"2025-12-30T13:36:00Z\", \"payeeCustomerId\": \"CUST001\", \"payerCustomerId\": \"CUST002\"}";

        mockMvc.perform(post("/api/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
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
