package com.example.dapprototype.service;

import com.atlassian.oai.validator.report.ValidationReport;
import com.example.dapprototype.classloader.TxnClassLoaderService;
import com.example.dapprototype.model.Customer;
import com.example.dapprototype.model.CustomerRequest;
import com.example.dapprototype.model.DAResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Service
public class RequestProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(RequestProcessingService.class);
    private static final String REQUEST_INFO_CLASS = "com.example.dapprototype.model.PaymentRequestInfo";
    private static final String REQUEST_MAPPER_CLASS = "com.example.dapprototype.mapper.PaymentRequestMapper";
    private static final String DECISION_DATA_CLASS = "com.example.dapprototype.model.PaymentAssessmentData";
    
    private final OpenApiRequestValidator openApiRequestValidator;
    private final ObjectMapper objectMapper;
    private final TxnClassLoaderService txnClassLoaderService;
    private final MockCustomerAPI mockCustomerAPI;
    private final MockRulesAPI mockRulesAPI;
    
    // Dynamically loaded classes
    private Class<?> requestInfoClass;
    private Class<?> paymentAssessmentDataClass;
    private Object requestMapperInstance;

    public RequestProcessingService(OpenApiRequestValidator openApiRequestValidator, 
                                   ObjectMapper objectMapper,
                                   TxnClassLoaderService txnClassLoaderService,
                                   MockCustomerAPI mockCustomerAPI,
                                   MockRulesAPI mockRulesAPI) {
        this.openApiRequestValidator = openApiRequestValidator;
        this.objectMapper = objectMapper;
        this.txnClassLoaderService = txnClassLoaderService;
        this.mockCustomerAPI = mockCustomerAPI;
        this.mockRulesAPI = mockRulesAPI;
        
        // Load classes dynamically on initialization
        initializeDynamicClasses();
    }
    
    /**
     * Initializes dynamically loaded classes using TxnClassLoader.
     */
    private void initializeDynamicClasses() {
        try {
            // Load PaymentRequestInfo class dynamically
            requestInfoClass = txnClassLoaderService.loadClass(REQUEST_INFO_CLASS);
            logger.info("Loaded {} using {}", REQUEST_INFO_CLASS, 
                       requestInfoClass.getClassLoader().getClass().getName());
            
            // Load PaymentAssessmentData class dynamically
            paymentAssessmentDataClass = txnClassLoaderService.loadClass(DECISION_DATA_CLASS);
            logger.info("Loaded {} using {}", DECISION_DATA_CLASS,
                       paymentAssessmentDataClass.getClassLoader().getClass().getName());
            
            // Load PaymentRequestMapper class dynamically
            Class<?> paymentRequestMapperClass = txnClassLoaderService.loadClass(REQUEST_MAPPER_CLASS);
            logger.info("Loaded {} using {}", REQUEST_MAPPER_CLASS, 
                       paymentRequestMapperClass.getClassLoader().getClass().getName());
            
            // Get the INSTANCE field from PaymentRequestMapper (MapStruct generated)
            requestMapperInstance = paymentRequestMapperClass.getField("INSTANCE").get(null);
            logger.info("Retrieved PaymentRequestMapper INSTANCE");
            
        } catch (Exception e) {
            logger.error("Failed to initialize dynamic classes", e);
            throw new RuntimeException("Failed to initialize dynamic classes", e);
        }
    }

    /**
     * Validates and processes a raw JSON request body using dynamically loaded classes.
     * 
     * @param rawBody the raw JSON request body
     * @return ResponseEntity with either the validated PaymentRequestInfo or a DAResponse
     */
    public ResponseEntity<?> validateAndProcessRequest(String rawBody) {
        // Validate request against OpenAPI spec
        ValidationReport report = openApiRequestValidator.validatePostJson("/request", rawBody, MediaType.APPLICATION_JSON_VALUE);
        if (report.hasErrors()) {
            DAResponse error = new DAResponse(false, "Validation failed", "VALIDATION_ERROR", 
                report.getMessages().stream()
                    .map(ValidationReport.Message::toString)
                    .toList(), null);
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
            DAResponse error = new DAResponse(false, "Invalid JSON payload", "VALIDATION_ERROR", 
                java.util.List.of("Invalid JSON payload"), null);
            return ResponseEntity.badRequest().body(error);
        }

        // Create CustomerRequest object from RequestInfo using dynamically loaded mapper
        CustomerRequest customerRequest;
        try {
            customerRequest = mapToCustomerRequest(requestInfo);
            logger.debug("Mapped to CustomerRequest: {}", customerRequest);
        } catch (Exception e) {
            logger.error("Failed to map requestInfo to CustomerRequest", e);
            DAResponse error = new DAResponse(false, "Error processing request", "PROCESSING_ERROR", 
                java.util.List.of(e.getMessage()), null);
            return ResponseEntity.status(500).body(error);
        }
                      
        // Create PaymentAssessmentData object and set all attributes
        Object paymentAssessmentData;
        try {
            // Call MockCustomerAPI to get customer details
            List<Customer> customers = mockCustomerAPI.getCustomers(customerRequest);
            logger.debug("Retrieved {} customers from API", customers.size());
            paymentAssessmentData = paymentAssessmentDataClass.getDeclaredConstructor().newInstance();

            Method setRequestInfoMethod = paymentAssessmentDataClass.getMethod("setRequestInfo", requestInfoClass);
            setRequestInfoMethod.invoke(paymentAssessmentData, requestInfo);
            Map<String, String> customerTags = customerRequest.getCustomerTags();
        
            // transformation of customer response into a format suitable for PaymentAssessmentData
            // this is being done in a generic manner
            if (customerTags != null) {
                for (Customer customer : customers) {
                    String tag = customerTags.get(customer.getCustomerId());
                    Method setCustomerMethod = paymentAssessmentDataClass.getMethod(tag, Customer.class);
                    setCustomerMethod.invoke(paymentAssessmentData, customer);
                }
            }

            logger.debug("Created PaymentAssessmentData with requestInfo and customers: {}", paymentAssessmentData);
        } catch (Exception e) {
            logger.error("Failed to create PaymentAssessmentData", e);
            DAResponse error = new DAResponse(false, "Error creating payment assessment data", "PROCESSING_ERROR", 
                java.util.List.of(e.getMessage()), null);
            return ResponseEntity.status(500).body(error);
        }
        
        return evaluateRulesAndCreateResponse(paymentAssessmentData);
    }
    
    /**
     * Evaluates rules on the payment assessment data and creates a response.
     * 
     * @param paymentAssessmentData the payment assessment data object
     * @return ResponseEntity with DAResponse
     */
    private ResponseEntity<?> evaluateRulesAndCreateResponse(Object paymentAssessmentData) {
        // Evaluate rules and get rulesResponse
        try {
            mockRulesAPI.evaluateRules(paymentAssessmentData);
            
            // Extract rulesResponse from paymentAssessmentData
            Method getRulesResponseMethod = paymentAssessmentDataClass.getMethod("getRulesResponse");
            Object rulesResponse = getRulesResponseMethod.invoke(paymentAssessmentData);
            
            // Create success response with rulesResponse
            DAResponse successResponse = new DAResponse(
                true, 
                "Request processed successfully", 
                "SUCCESS", 
                java.util.List.of(), 
                (com.example.dapprototype.model.RulesResponse) rulesResponse
            );
            
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            logger.error("Failed to evaluate rules", e);
            DAResponse error = new DAResponse(false, "Error evaluating rules", "PROCESSING_ERROR", 
                java.util.List.of(e.getMessage()), null);
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
