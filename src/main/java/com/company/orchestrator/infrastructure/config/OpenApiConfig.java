package com.company.orchestrator.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
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
                .description("Eclipse EDC - Policy-Aware Data Transfer Orchestration System")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("API Support")
                    .email("support@company.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}

