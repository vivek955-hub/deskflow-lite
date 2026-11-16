package com.vivek.deskflow.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Adds the "Authorize" button to Swagger UI so you can paste a JWT and try the protected APIs. */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI deskflowOpenApi() {
        return new OpenAPI()
                .info(new Info().title("DeskFlow Lite API").version("1.0.0")
                        .description("Multi-tenant helpdesk with AI-assisted ticket triage"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
