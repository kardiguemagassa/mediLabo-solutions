package com.openclassrooms.notificationservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI pour le service de Notification.
 * Permet de documenter les APIs de messagerie et de gérer l'authentification JWT dans Swagger UI
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                final String securitySchemeName = "bearerAuth";

                return new OpenAPI()
                        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                        .components(new Components()
                                .addSecuritySchemes(securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description("Entrez le token JWT extrait du cookie d'authentification pour tester les routes."))

                                .addResponses("Unauthorized", new ApiResponse().description("Échec de l'authentification : Token manquant, invalide ou expiré."))
                                .addResponses("Forbidden", new ApiResponse().description("Accès refusé : Vous n'avez pas les permissions nécessaires pour voir ces messages."))
                                .addResponses("NotFound", new ApiResponse().description("La conversation ou le message demandé n'existe pas."))
                                .addResponses("InternalError", new ApiResponse().description("Erreur technique lors de l'envoi ou de la récupération des notifications.")))

                        .info(new Info()
                                .title("MediLabo - Notification Service")
                                .version("1.0.0")
                                .description("Microservice de communication inter-applicative. " +
                                        "Permet aux Praticiens et Organisateurs d'échanger des messages sécurisés concernant le suivi des patients.")
                                .contact(new Contact()
                                        .name("Équipe Backend MediLabo")
                                        .email("dev-notify@medilabo.fr")
                                        .url("https://medilabo.fr/internal/support"))
                                .license(new License()
                                        .name("Propriétaire - MediLabo Solutions")
                                        .url("https://medilabo.fr/licenses")));
        }
}