package com.openclassrooms.notesservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Note Service API",
                version = "1.0",
                description = "API de gestion des notes médicales - MediLabo Solutions",
                contact = @Contact(
                        name = "Kardigué MAGASSA",
                        email = "contact@medilabo.com"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8082", description = "Serveur de développement")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}