package com.example.dapprototype.model;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private static OpenApiInteractionValidator validator;

    @BeforeAll
    static void setUpValidator() throws Exception {
        validator = OpenApiInteractionValidator.createFor(new ClassPathResource("openapi.yaml").getURL().toString())
                .build();
    }

    @Test
    @DisplayName("OpenAPI validator accepts a valid payload")
    void requestInfoValid() {
        String body = "{\"requestInfo\": {\"activityId\": \"abcd\", \"activityTimeStamp\": \"2025-12-30T13:36:00Z\"}}";
        ValidationReport report = validator.validateRequest(SimpleRequest.Builder.post("/request")
                .withContentType("application/json")
                .withBody(body)
                .build());
        assertThat(report.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("OpenAPI validator rejects missing activityId")
    void requestInfoBlankActivityId() {
        String body = "{\"requestInfo\": {\"activityTimeStamp\": \"2025-12-30T13:36:00Z\"}}";
        ValidationReport report = validator.validateRequest(SimpleRequest.Builder.post("/request")
                .withContentType("application/json")
                .withBody(body)
                .build());
        assertThat(report.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("OpenAPI validator rejects null requestInfo")
    void requestPayloadNullRequestInfo() {
        String body = "{}";
        ValidationReport report = validator.validateRequest(SimpleRequest.Builder.post("/request")
                .withContentType("application/json")
                .withBody(body)
                .build());
        assertThat(report.hasErrors()).isTrue();
    }
}
