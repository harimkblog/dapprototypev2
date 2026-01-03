package com.example.dapprototype.controller;

import com.example.dapprototype.model.DecisionResponse;
import com.example.dapprototype.service.RequestProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RequestController {

    private final RequestProcessingService requestProcessingService;

    public RequestController(RequestProcessingService requestProcessingService) {
        this.requestProcessingService = requestProcessingService;
    }

    @PostMapping("/request")
    public ResponseEntity<?> submitRequest(@RequestBody String rawBody) {
        ResponseEntity<?> validationResult = requestProcessingService.validateAndProcessRequest(rawBody);
        
        // If validation failed, return the error response
        if (validationResult.getStatusCode().is4xxClientError()) {
            return validationResult;
        }
        
        // Validation passed, return success response
        DecisionResponse response = new DecisionResponse(true, "Request processed successfully", null, null);
        return ResponseEntity.ok(response);
    }
}
