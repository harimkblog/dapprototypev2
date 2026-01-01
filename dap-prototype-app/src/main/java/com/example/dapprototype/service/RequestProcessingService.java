package com.example.dapprototype.service;

import com.atlassian.oai.validator.report.ValidationReport;
import com.example.dapprototype.mapper.RequestMapper;
import com.example.dapprototype.model.CustomerEnrichment;
import com.example.dapprototype.model.ErrorResponse;
import com.example.dapprototype.model.RequestPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RequestProcessingService {

    private final OpenApiRequestValidator openApiRequestValidator;
    private final ObjectMapper objectMapper;
    private final RequestMapper requestMapper;

    public RequestProcessingService(OpenApiRequestValidator openApiRequestValidator, 
                                   ObjectMapper objectMapper,
                                   RequestMapper requestMapper) {
        this.openApiRequestValidator = openApiRequestValidator;
        this.objectMapper = objectMapper;
        this.requestMapper = requestMapper;
    }

    /**
     * Validates and processes a raw JSON request body.
     * 
     * @param rawBody the raw JSON request body
     * @return ResponseEntity with either the validated RequestPayload or an ErrorResponse
     */
    public ResponseEntity<?> validateAndProcessRequest(String rawBody) {
        // Validate request against OpenAPI spec
        ValidationReport report = openApiRequestValidator.validatePostJson("/request", rawBody, MediaType.APPLICATION_JSON_VALUE);
        if (report.hasErrors()) {
            ErrorResponse error = new ErrorResponse(false, "Validation failed", "VALIDATION_ERROR", 
                report.getMessages().stream()
                    .map(ValidationReport.Message::toString)
                    .toList());
            return ResponseEntity.badRequest().body(error);
        }

        // Deserialize after validation passes
        RequestPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, RequestPayload.class);
        } catch (JsonProcessingException ex) {
            ErrorResponse error = new ErrorResponse(false, "Invalid JSON payload", "VALIDATION_ERROR", 
                java.util.List.of("Invalid JSON payload"));
            return ResponseEntity.badRequest().body(error);
        }

        // Create CustomerEnrichment object from RequestPayload using MapStruct mapper
        CustomerEnrichment customerEnrichment = requestMapper.toCustomerEnrichment(payload);
        return ResponseEntity.ok(customerEnrichment);
    }
}
