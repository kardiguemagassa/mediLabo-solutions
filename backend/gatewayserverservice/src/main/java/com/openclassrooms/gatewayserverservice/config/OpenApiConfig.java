package com.openclassrooms.gatewayserverservice.config;

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

                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Entrez le token JWT obtenu via le service d'authentification pour tester les routes protégées."))

                        // Réponses d'erreurs standardisées pour le Gateway (Fallback, Security)
                        .addResponses("Unauthorized", new ApiResponse().description("Authentification requise ou jeton invalide"))
                        .addResponses("Forbidden", new ApiResponse().description("Accès interdit - Permissions insuffisantes"))
                        .addResponses("ServiceUnavailable", new ApiResponse().description("Le service cible est momentanément indisponible (Circuit Breaker)")))

                .info(new Info()
                        .title("MediLabo - API Gateway Hub")
                        .version("1.0.0")
                        .description("Point d'entrée unique de l'écosystème MediLabo. " +
                                "Cette interface agrège la documentation de tous les microservices : " +
                                "Authentification, Patients, Notes et Évaluations.")
                        .contact(new Contact()
                                .name("Kardigué")
                                .email("magassakara@gmail.com")
                                .url("https://medilabo.fr/support"))
                        .license(new License()
                                .name("Proprietary - MediLabo Solutions")
                                .url("https://medilabo.fr/licenses")));
    }
}