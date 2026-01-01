package com.example.dapprototype.service;

import com.example.dapprototype.classloader.TxnClassLoaderService;
import com.example.dapprototype.config.OpenApiValidatorConfig;
import com.example.dapprototype.model.CustomerEnrichment;
import com.example.dapprototype.model.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({OpenApiRequestValidator.class, OpenApiValidatorConfig.class, TxnClassLoaderService.class})
class RequestProcessingServiceTest {

    @Autowired
    private RequestProcessingService requestProcessingService;

    @Test
    @DisplayName("validateAndProcessRequest returns success for valid payload and creates CustomerEnrichment")
    void validateAndProcessRequest_withValidPayload_returnsSuccess() throws Exception {
        String rawBody = "{\"requestInfo\": {\"activityId\": \"abcd\", \"activityTimeStamp\": \"2025-12-30T13:36:00Z\"}}";

        ResponseEntity<?> result = requestProcessingService.validateAndProcessRequest(rawBody);

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isInstanceOf(CustomerEnrichment.class);
        CustomerEnrichment enrichment = (CustomerEnrichment) result.getBody();
        assertThat(enrichment.getActivityId()).isEqualTo("abcd");
    }

    @Test
    @DisplayName("validateAndProcessRequest returns error for missing requestInfo")
    void validateAndProcessRequest_withMissingRequestInfo_returnsError() {
        String rawBody = "{}";

        ResponseEntity<?> result = requestProcessingService.validateAndProcessRequest(rawBody);

        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
        assertThat(result.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) result.getBody();
        assertThat(errorResponse.isSuccess()).isFalse();
        assertThat(errorResponse.getCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("validateAndProcessRequest returns error for missing requestId")
    void validateAndProcessRequest_withMissingRequestId_returnsError() {
        String rawBody = "{\"requestInfo\": {\"activityTimeStamp\": \"2025-12-30T13:36:00Z\"}}";

        ResponseEntity<?> result = requestProcessingService.validateAndProcessRequest(rawBody);

        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
        assertThat(result.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) result.getBody();
        assertThat(errorResponse.isSuccess()).isFalse();
        assertThat(errorResponse.getCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("validateAndProcessRequest returns error for missing timestamp")
    void validateAndProcessRequest_withMissingTimestamp_returnsError() {
        String rawBody = "{\"requestInfo\": {\"activityId\": \"abcd\"}}";

        ResponseEntity<?> result = requestProcessingService.validateAndProcessRequest(rawBody);

        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
        assertThat(result.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) result.getBody();
        assertThat(errorResponse.isSuccess()).isFalse();
        assertThat(errorResponse.getCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("validateAndProcessRequest returns error for invalid JSON")
    void validateAndProcessRequest_withInvalidJson_returnsError() {
        String rawBody = "{invalid json}";

        ResponseEntity<?> result = requestProcessingService.validateAndProcessRequest(rawBody);

        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
        assertThat(result.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) result.getBody();
        assertThat(errorResponse.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("validateAndProcessRequest returns error for invalid timestamp format")
    void validateAndProcessRequest_withInvalidTimestampFormat_returnsError() {
        String rawBody = "{\"requestInfo\": {\"activityId\": \"abcd\", \"activityTimeStamp\": \"not-a-date\"}}";

        ResponseEntity<?> result = requestProcessingService.validateAndProcessRequest(rawBody);

        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
        assertThat(result.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) result.getBody();
        assertThat(errorResponse.isSuccess()).isFalse();
        assertThat(errorResponse.getCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("validateAndProcessRequest returns error for null requestId")
    void validateAndProcessRequest_withNullRequestId_returnsError() {
        String rawBody = "{\"requestInfo\": {\"activityId\": null, \"activityTimeStamp\": \"2025-12-30T13:36:00Z\"}}";

        ResponseEntity<?> result = requestProcessingService.validateAndProcessRequest(rawBody);

        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
        assertThat(result.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) result.getBody();
        assertThat(errorResponse.isSuccess()).isFalse();
        assertThat(errorResponse.getCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("validateAndProcessRequest returns error for empty requestId")
    void validateAndProcessRequest_withEmptyRequestId_returnsError() {
        String rawBody = "{\"requestInfo\": {\"activityId\": \"\", \"activityTimeStamp\": \"2025-12-30T13:36:00Z\"}}";

        ResponseEntity<?> result = requestProcessingService.validateAndProcessRequest(rawBody);

        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
        assertThat(result.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) result.getBody();
        assertThat(errorResponse.isSuccess()).isFalse();
        assertThat(errorResponse.getCode()).isEqualTo("VALIDATION_ERROR");
    }
}
