package com.example.dapprototype.service;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class OpenApiRequestValidator {

    private final OpenApiInteractionValidator validator;

    public OpenApiRequestValidator(OpenApiInteractionValidator validator) {
        this.validator = validator;
    }

    public ValidationReport validatePostJson(String path, String rawBody, String contentType) {
        String resolvedContentType = contentType != null ? contentType : MediaType.APPLICATION_JSON_VALUE;
        SimpleRequest request = SimpleRequest.Builder.post(path)
                .withContentType(resolvedContentType)
                .withBody(rawBody == null ? "" : rawBody)
                .build();
        return validator.validateRequest(request);
    }
}
