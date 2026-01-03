package com.example.dapprototype.service;

import com.atlassian.oai.validator.report.ValidationReport;
import com.example.dapprototype.classloader.TxnClassLoaderService;
import com.example.dapprototype.model.Customer;
import com.example.dapprototype.model.CustomerRequest;
import com.example.dapprototype.model.DecisionResponse;
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
    private static final String REQUEST_INFO_CLASS = "com.example.dapprototype.model.RequestInfo";
    private static final String REQUEST_MAPPER_CLASS = "com.example.dapprototype.mapper.RequestMapper";
    private static final String DECISION_DATA_CLASS = "com.example.dapprototype.model.DecisionData";
    
    private final OpenApiRequestValidator openApiRequestValidator;
    private final ObjectMapper objectMapper;
    private final TxnClassLoaderService txnClassLoaderService;
    private final CustomerAPI customerAPI;
    
    // Dynamically loaded classes
    private Class<?> requestInfoClass;
    private Class<?> decisionDataClass;
    private Object requestMapperInstance;

    public RequestProcessingService(OpenApiRequestValidator openApiRequestValidator, 
                                   ObjectMapper objectMapper,
                                   TxnClassLoaderService txnClassLoaderService,
                                   CustomerAPI customerAPI) {
        this.openApiRequestValidator = openApiRequestValidator;
        this.objectMapper = objectMapper;
        this.txnClassLoaderService = txnClassLoaderService;
        this.customerAPI = customerAPI;
        
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
            
            // Load DecisionData class dynamically
            decisionDataClass = txnClassLoaderService.loadClass(DECISION_DATA_CLASS);
            logger.info("Loaded {} using {}", DECISION_DATA_CLASS,
                       decisionDataClass.getClassLoader().getClass().getName());
            
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
     * @return ResponseEntity with either the validated RequestInfo or a DecisionResponse
     */
    public ResponseEntity<?> validateAndProcessRequest(String rawBody) {
        // Validate request against OpenAPI spec
        ValidationReport report = openApiRequestValidator.validatePostJson("/request", rawBody, MediaType.APPLICATION_JSON_VALUE);
        if (report.hasErrors()) {
            DecisionResponse error = new DecisionResponse(false, "Validation failed", "VALIDATION_ERROR", 
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
            DecisionResponse error = new DecisionResponse(false, "Invalid JSON payload", "VALIDATION_ERROR", 
                java.util.List.of("Invalid JSON payload"));
            return ResponseEntity.badRequest().body(error);
        }

        // Create CustomerRequest object from RequestInfo using dynamically loaded mapper
        CustomerRequest customerRequest;
        try {
            customerRequest = mapToCustomerRequest(requestInfo);
            logger.debug("Mapped to CustomerRequest: {}", customerRequest);
        } catch (Exception e) {
            logger.error("Failed to map requestInfo to CustomerRequest", e);
            DecisionResponse error = new DecisionResponse(false, "Error processing request", "PROCESSING_ERROR", 
                java.util.List.of(e.getMessage()));
            return ResponseEntity.status(500).body(error);
        }
                      
        // Create DecisionData object and set all attributes
        Object decisionData;
        try {
            // Call CustomerAPI to get customer details
            List<Customer> customers = customerAPI.getCustomers(customerRequest);
            logger.debug("Retrieved {} customers from API", customers.size());
            decisionData = decisionDataClass.getDeclaredConstructor().newInstance();

            Method setRequestInfoMethod = decisionDataClass.getMethod("setRequestInfo", requestInfoClass);
            setRequestInfoMethod.invoke(decisionData, requestInfo);
            Map<String, String> customerTags = customerRequest.getCustomerTags();
        
            // transformation of customer response into a format suitable for DecisionData
            // this is being done in a generic manner
            if (customerTags != null) {
                for (Customer customer : customers) {
                    String tag = customerTags.get(customer.getCustomerId());
                    Method setCustomerMethod = decisionDataClass.getMethod(tag, Customer.class);
                    setCustomerMethod.invoke(decisionData, customer);
                }
            }

            logger.debug("Created DecisionData with requestInfo and customers: {}", decisionData);
        } catch (Exception e) {
            logger.error("Failed to create DecisionData", e);
            DecisionResponse error = new DecisionResponse(false, "Error creating decision data", "PROCESSING_ERROR", 
                java.util.List.of(e.getMessage()));
            return ResponseEntity.status(500).body(error);
        }
        
        return ResponseEntity.ok(customerRequest);
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
