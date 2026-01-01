package com.example.dapprototype.controller;

import com.example.dapprototype.model.RequestPayload;
import com.example.dapprototype.model.RequestResponse;
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
        RequestResponse response = new RequestResponse(true, "Request processed successfully");
        return ResponseEntity.ok(response);
    }
}
