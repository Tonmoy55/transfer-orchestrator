package com.company.orchestrator.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transferOrchestratorOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Transfer Orchestrator API")
                .description("""
                    Eclipse EDC - Policy-Aware Data Transfer Orchestration System
                    
                    ## Response Format
                    All API responses follow a standardized format:
                    ```json
                    {
                      "statusCode": 200,
                      "message": "Success message",
                      "data": { ... },
                      "timestamp": "2025-12-12T18:30:00",
                      "success": true
                    }
                    ```
                    
                    - `statusCode`: HTTP status code
                    - `message`: Human-readable message
                    - `data`: Response payload (can be object, array, or null)
                    - `timestamp`: ISO 8601 timestamp
                    - `success`: Boolean indicating success/failure
                    """)
                .version("v1.0.0")
                .contact(new Contact()
                    .name("API Support")
                    .email("support@company.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }

    @Bean
    public OpenApiCustomizer customizeOpenApi() {
        return openApi -> {
            // Add common response examples to all operations
            openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> {
                    ApiResponses responses = operation.getResponses();

                    // Add 500 error response to all operations if not present
                    if (!responses.containsKey("500")) {
                        responses.addApiResponse("500", new ApiResponse()
                            .description("Internal Server Error")
                            .content(new Content()
                                .addMediaType("application/json", new MediaType()
                                    .schema(new Schema<>()
                                        .type("object")
                                        .example("""
                                            {
                                              "statusCode": 500,
                                              "message": "Internal server error occurred",
                                              "data": {
                                                "errorType": "Exception",
                                                "errorDetails": "Error details"
                                              },
                                              "timestamp": "2025-12-12T18:30:00",
                                              "success": false
                                            }
                                            """)))));
                    }
                })
            );
        };
    }
}


