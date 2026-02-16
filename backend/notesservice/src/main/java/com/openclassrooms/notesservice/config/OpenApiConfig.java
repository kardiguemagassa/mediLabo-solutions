package com.openclassrooms.notesservice.config;

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
 * Configuration OpenAPI / Swagger pour le Note Service.
 * Harmonisée avec le standard de l'écosystème MediLabo.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI noteServiceOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        // 1. Définition du schéma de sécurité pour les notes (données sensibles)
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Entrez le token JWT fourni par l'Authorization Server"))

                        // 2. Réponses d'erreurs standardisées
                        .addResponses("Unauthorized", new ApiResponse().description("Accès refusé : Jeton absent ou invalide"))
                        .addResponses("Forbidden", new ApiResponse().description("Permissions insuffisantes pour consulter ces notes"))
                        .addResponses("NotFound", new ApiResponse().description("La note ou le dossier patient n'a pas été trouvé"))
                        .addResponses("InternalError", new ApiResponse().description("Erreur technique lors de la récupération des notes")))

                .info(new Info()
                        .title("MediLabo - Note Service")
                        .version("1.0.0")
                        .description("Microservice de gestion des notes cliniques et observations médicales. " +
                                "Stocke l'historique des praticiens pour chaque patient.")
                        .contact(new Contact()
                                .name("Kardigué MAGASSA")
                                .email("magassa***REMOVED_USER***@gmail.com")
                                .url("https://medilabo.fr/notes"))
                        .license(new License()
                                .name("Proprietary - MediLabo Solutions")
                                .url("https://medilabo.fr/licenses")));
    }
}