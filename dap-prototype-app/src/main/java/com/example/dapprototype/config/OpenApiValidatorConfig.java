package com.example.dapprototype.config;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class OpenApiValidatorConfig {

    @Bean
    public OpenApiInteractionValidator openApiInteractionValidator() throws IOException {
        // Load the OpenAPI definition from the classpath; placed at src/main/resources/openapi.yaml
        return OpenApiInteractionValidator.createFor(new ClassPathResource("openapi.yaml").getURL().toString())
                .build();
    }
}
