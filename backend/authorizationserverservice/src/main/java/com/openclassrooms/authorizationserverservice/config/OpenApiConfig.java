package com.openclassrooms.authorizationserverservice.config;

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
 * Configuration OpenAPI / Swagger pour l'Authorization Server.
 * Standardisé sur le modèle "Enterprise" des autres microservices.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authorizationServerOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        // 1. Définition du schéma de sécurité pour tester les routes protégées (ex: /user/me)
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Entrez le token JWT généré par ce serveur pour tester les endpoints sécurisés"))

                        // 2. Réponses d'erreurs standardisées pour l'ensemble du serveur d'auth
                        .addResponses("Unauthorized", new ApiResponse().description("Authentification requise ou jeton invalide"))
                        .addResponses("Forbidden", new ApiResponse().description("Droits insuffisants (Scope/Role) pour accéder à cette ressource"))
                        .addResponses("BadRequest", new ApiResponse().description("Requête mal formée ou paramètres manquants"))
                        .addResponses("InternalError", new ApiResponse().description("Erreur interne du serveur d'autorisation")))

                .info(new Info()
                        .title("MediLabo - Authorization Server")
                        .version("1.0.0")
                        .description("Service central de sécurité basé sur OAuth2 et OpenID Connect (OIDC). " +
                                "Responsable de l'émission des JSON Web Tokens (JWT) et de la gestion des clés JWKS.")
                        .contact(new Contact()
                                .name("Kardigué MAGASSA")
                                .email("magassa***REMOVED_USER***@gmail.com")
                                .url("https://medilabo.fr/security"))
                        .license(new License()
                                .name("Proprietary - MediLabo Solutions")
                                .url("https://medilabo.fr/licenses")));
    }
}