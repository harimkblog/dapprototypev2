package com.example.dapprototype.service;

import com.atlassian.oai.validator.report.ValidationReport;
import com.example.dapprototype.classloader.TxnClassLoaderService;
import com.example.dapprototype.model.CustomerEnrichment;
import com.example.dapprototype.model.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

@Service
public class RequestProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(RequestProcessingService.class);
    private static final String REQUEST_PAYLOAD_CLASS = "com.example.dapprototype.model.RequestPayload";
    private static final String REQUEST_MAPPER_CLASS = "com.example.dapprototype.mapper.RequestMapper";
    
    private final OpenApiRequestValidator openApiRequestValidator;
    private final ObjectMapper objectMapper;
    private final TxnClassLoaderService txnClassLoaderService;
    
    // Dynamically loaded classes
    private Class<?> requestPayloadClass;
    private Object requestMapperInstance;

    public RequestProcessingService(OpenApiRequestValidator openApiRequestValidator, 
                                   ObjectMapper objectMapper,
                                   TxnClassLoaderService txnClassLoaderService) {
        this.openApiRequestValidator = openApiRequestValidator;
        this.objectMapper = objectMapper;
        this.txnClassLoaderService = txnClassLoaderService;
        
        // Load classes dynamically on initialization
        initializeDynamicClasses();
    }
    
    /**
     * Initializes dynamically loaded classes using TxnClassLoader.
     */
    private void initializeDynamicClasses() {
        try {
            // Load RequestPayload class dynamically
            requestPayloadClass = txnClassLoaderService.loadClass(REQUEST_PAYLOAD_CLASS);
            logger.info("Loaded {} using {}", REQUEST_PAYLOAD_CLASS, 
                       requestPayloadClass.getClassLoader().getClass().getName());
            
            // Load RequestMapper class dynamically
            Class<?> requestMapperClass = txnClassLoaderService.loadClass(REQUEST_MAPPER_CLASS);
            logger.info("Loaded {} using {}", REQUEST_MAPPER_CLASS, 
                       requestMapperClass.getClassLoader().getClass().getName());
            
            // Get the INSTANCE field from RequestMapper (MapStruct generated)
            requestMapperInstance = requestMapperClass.getField("INSTANCE").get(null);
            logger.info("Retrieved RequestMapper INSTANCE");
            
        } catch (Exception e) {
            logger.error("Failed to initialize dynamic classes", e);
            throw new RuntimeException("Failed to initialize dynamic classes", e);
        }
    }

    /**
     * Validates and processes a raw JSON request body using dynamically loaded classes.
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

        // Deserialize after validation passes using dynamically loaded RequestPayload class
        Object payload;
        try {
            payload = objectMapper.readValue(rawBody, requestPayloadClass);
            logger.debug("Deserialized payload using class: {}", payload.getClass().getName());
            logger.debug("Payload class loader: {}", payload.getClass().getClassLoader());
        } catch (JsonProcessingException ex) {
            logger.error("Failed to deserialize JSON to {}", REQUEST_PAYLOAD_CLASS, ex);
            ErrorResponse error = new ErrorResponse(false, "Invalid JSON payload", "VALIDATION_ERROR", 
                java.util.List.of("Invalid JSON payload"));
            return ResponseEntity.badRequest().body(error);
        }

        // Create CustomerEnrichment object from RequestPayload using dynamically loaded mapper
        try {
            CustomerEnrichment customerEnrichment = mapToCustomerEnrichment(payload);
            logger.debug("Mapped to CustomerEnrichment: {}", customerEnrichment);
            return ResponseEntity.ok(customerEnrichment);
        } catch (Exception e) {
            logger.error("Failed to map payload to CustomerEnrichment", e);
            ErrorResponse error = new ErrorResponse(false, "Error processing request", "PROCESSING_ERROR", 
                java.util.List.of(e.getMessage()));
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Maps the dynamically loaded RequestPayload object to CustomerEnrichment
     * using reflection to invoke the mapper method.
     * 
     * @param payload the RequestPayload object (loaded dynamically)
     * @return CustomerEnrichment object
     * @throws Exception if reflection fails
     */
    private CustomerEnrichment mapToCustomerEnrichment(Object payload) throws Exception {
        // Use reflection to call: requestMapper.toCustomerEnrichment(payload)
        Method mapperMethod = requestMapperInstance.getClass()
            .getMethod("toCustomerEnrichment", requestPayloadClass);
        
        Object result = mapperMethod.invoke(requestMapperInstance, payload);
        
        // The result should be a CustomerEnrichment object
        if (result instanceof CustomerEnrichment) {
            return (CustomerEnrichment) result;
        } else {
            throw new IllegalStateException("Mapper did not return CustomerEnrichment: " + 
                (result != null ? result.getClass().getName() : "null"));
        }
    }
}
