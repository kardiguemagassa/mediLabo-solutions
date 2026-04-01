package com.openclassrooms.patientservice.config;

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
 * Configuration OpenAPI / Swagger pour le Patient Service.
 * Centralise la documentation des endpoints de gestion administrative des patients.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI patientServiceOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        // Schéma de sécurité (JWT Bearer)
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Entrez le token JWT obtenu via l'Authorization Server"))

                        // Réponses d'erreurs standardisées pour le domaine Patient
                        .addResponses("Unauthorized", new ApiResponse().description("Identification requise : Token manquant ou expiré"))
                        .addResponses("Forbidden", new ApiResponse().description("Droits insuffisants : Vous n'avez pas le rôle requis (ex: ADMIN, PRACTITIONER)"))
                        .addResponses("NotFound", new ApiResponse().description("Patient introuvable pour l'identifiant fourni"))
                        .addResponses("Conflict", new ApiResponse().description("Conflit : Un patient avec ces informations existe déjà (ex: Email ou Numéro de dossier)")))

                .info(new Info()
                        .title("MediLabo - Patient Service")
                        .version("1.0.0")
                        .description("Microservice de gestion administrative des patients. " +
                                "Gère les données démographiques, les coordonnées et les identifiants médicaux uniques.")
                        .contact(new Contact()
                                .name("Kardigué MAGASSA")
                                .email("magassa***REMOVED_USER***@gmail.com")
                                .url("https://medilabo.fr/patients"))
                        .license(new License()
                                .name("Proprietary - MediLabo Solutions")
                                .url("https://medilabo.fr/licenses")));
    }
}