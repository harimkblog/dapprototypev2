package com.example.dapprototype.service;

import com.atlassian.oai.validator.report.ValidationReport;
import com.example.dapprototype.classloader.TxnClassLoaderService;
import com.example.dapprototype.model.CustomerRequest;
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
    private static final String REQUEST_INFO_CLASS = "com.example.dapprototype.model.RequestInfo";
    private static final String REQUEST_MAPPER_CLASS = "com.example.dapprototype.mapper.RequestMapper";
    
    private final OpenApiRequestValidator openApiRequestValidator;
    private final ObjectMapper objectMapper;
    private final TxnClassLoaderService txnClassLoaderService;
    
    // Dynamically loaded classes
    private Class<?> requestInfoClass;
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
            // Load RequestInfo class dynamically
            requestInfoClass = txnClassLoaderService.loadClass(REQUEST_INFO_CLASS);
            logger.info("Loaded {} using {}", REQUEST_INFO_CLASS, 
                       requestInfoClass.getClassLoader().getClass().getName());
            
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
     * @return ResponseEntity with either the validated RequestInfo or an ErrorResponse
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

        // Deserialize after validation passes using dynamically loaded RequestInfo class
        Object requestInfo;
        try {
            requestInfo = objectMapper.readValue(rawBody, requestInfoClass);
            logger.debug("Deserialized requestInfo using class: {}", requestInfo.getClass().getName());
            logger.debug("RequestInfo class loader: {}", requestInfo.getClass().getClassLoader());
        } catch (JsonProcessingException ex) {
            logger.error("Failed to deserialize JSON to {}", REQUEST_INFO_CLASS, ex);
            ErrorResponse error = new ErrorResponse(false, "Invalid JSON payload", "VALIDATION_ERROR", 
                java.util.List.of("Invalid JSON payload"));
            return ResponseEntity.badRequest().body(error);
        }

        // Create CustomerRequest object from RequestInfo using dynamically loaded mapper
        try {
            CustomerRequest customerRequest = mapToCustomerRequest(requestInfo);
            logger.debug("Mapped to CustomerRequest: {}", customerRequest);
            return ResponseEntity.ok(customerRequest);
        } catch (Exception e) {
            logger.error("Failed to map requestInfo to CustomerRequest", e);
            ErrorResponse error = new ErrorResponse(false, "Error processing request", "PROCESSING_ERROR", 
                java.util.List.of(e.getMessage()));
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Maps the dynamically loaded RequestInfo object to CustomerRequest
     * using reflection to invoke the mapper method.
     * 
     * @param requestInfo the RequestInfo object (loaded dynamically)
     * @return CustomerRequest object
     * @throws Exception if reflection fails
     */
    private CustomerRequest mapToCustomerRequest(Object requestInfo) throws Exception {
        // Use reflection to call: requestMapper.toCustomerRequest(requestInfo)
        Method mapperMethod = requestMapperInstance.getClass()
            .getMethod("toCustomerRequest", requestInfoClass);
        
        Object result = mapperMethod.invoke(requestMapperInstance, requestInfo);
        
        // The result should be a CustomerRequest object
        if (result instanceof CustomerRequest) {
            return (CustomerRequest) result;
        } else {
            throw new IllegalStateException("Mapper did not return CustomerRequest: " + 
                (result != null ? result.getClass().getName() : "null"));
        }
    }
}
