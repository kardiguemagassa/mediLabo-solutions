package com.openclassrooms.assessmentservice.config;

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

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                final String securitySchemeName = "bearerAuth";

                return new OpenAPI()
                        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                        .components(new Components()
                                // 1. Définition du schéma de sécurité
                                .addSecuritySchemes(securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description("Entrez le token JWT fourni par le service d'authentification"))

                                // 2. Définition de schémas de réponses d'erreurs réutilisables
                                .addResponses("Unauthorized", new ApiResponse().description("Jeton invalide ou expiré"))
                                .addResponses("Forbidden", new ApiResponse().description("Droits insuffisants pour accéder à cette ressource"))
                                .addResponses("InternalError", new ApiResponse().description("Erreur interne du serveur - Contactez le support technique")))

                        .info(new Info()
                                .title("MediLabo - Assessment Service")
                                .version("1.0.0")
                                .description("Microservice critique d'évaluation du risque de diabète. " +
                                        "Consomme les APIs 'Patient' et 'Notes' pour produire un diagnostic probabiliste.")
                                .contact(new Contact()
                                        .name("Support Technique MediLabo")
                                        .email("dev@medilabo.fr")
                                        .url("https://medilabo.fr/support"))
                                .license(new License()
                                        .name("Proprietary - MediLabo Solutions")
                                        .url("https://medilabo.fr/licenses")));
        }
}