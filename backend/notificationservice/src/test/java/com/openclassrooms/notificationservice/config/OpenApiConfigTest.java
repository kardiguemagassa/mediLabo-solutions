package com.openclassrooms.notificationservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
    }

    @Test
    void customOpenAPI_ShouldReturnNonNullOpenAPI() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        assertNotNull(result);
    }

    @Test
    void customOpenAPI_ShouldConfigureSecurityRequirement() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        List<SecurityRequirement> securityRequirements = result.getSecurity();
        assertNotNull(securityRequirements);
        assertFalse(securityRequirements.isEmpty());

        SecurityRequirement securityRequirement = securityRequirements.getFirst();
        assertNotNull(securityRequirement);

        assertTrue(securityRequirement.containsKey("bearerAuth"));
    }

    @Test
    void customOpenAPI_ShouldConfigureComponents() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Components components = result.getComponents();
        assertNotNull(components);
    }

    @Test
    void customOpenAPI_ShouldConfigureSecurityScheme() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Components components = result.getComponents();
        Map<String, SecurityScheme> securitySchemes = components.getSecuritySchemes();
        assertNotNull(securitySchemes);
        assertTrue(securitySchemes.containsKey("bearerAuth"));

        SecurityScheme securityScheme = securitySchemes.get("bearerAuth");
        assertNotNull(securityScheme);
        assertEquals("bearerAuth", securityScheme.getName());
        assertEquals(SecurityScheme.Type.HTTP, securityScheme.getType());
        assertEquals("bearer", securityScheme.getScheme());
        assertEquals("JWT", securityScheme.getBearerFormat());
        assertTrue(securityScheme.getDescription().contains("Entrez le token JWT"));
    }

    @Test
    void customOpenAPI_ShouldConfigureErrorResponses() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Components components = result.getComponents();
        Map<String, ApiResponse> responses = components.getResponses();
        assertNotNull(responses);

        // Vérifier Unauthorized
        assertTrue(responses.containsKey("Unauthorized"));
        ApiResponse unauthorized = responses.get("Unauthorized");
        assertNotNull(unauthorized);
        assertTrue(unauthorized.getDescription().contains("Échec de l'authentification"));

        // Vérifier Forbidden
        assertTrue(responses.containsKey("Forbidden"));
        ApiResponse forbidden = responses.get("Forbidden");
        assertNotNull(forbidden);
        assertTrue(forbidden.getDescription().contains("Accès refusé"));

        // Vérifier NotFound
        assertTrue(responses.containsKey("NotFound"));
        ApiResponse notFound = responses.get("NotFound");
        assertNotNull(notFound);
        assertTrue(notFound.getDescription().contains("n'existe pas"));

        // Vérifier InternalError
        assertTrue(responses.containsKey("InternalError"));
        ApiResponse internalError = responses.get("InternalError");
        assertNotNull(internalError);
        assertTrue(internalError.getDescription().contains("Erreur technique"));
    }

    @Test
    void customOpenAPI_ShouldConfigureInfo() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Info info = result.getInfo();
        assertNotNull(info);

        assertEquals("MediLabo - Notification Service", info.getTitle());
        assertEquals("1.0.0", info.getVersion());
        assertTrue(info.getDescription().contains("Microservice de communication inter-applicative"));
    }

    @Test
    void customOpenAPI_ShouldConfigureContact() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Info info = result.getInfo();
        Contact contact = info.getContact();

        assertNotNull(contact);
        assertEquals("Équipe Backend MediLabo", contact.getName());
        assertEquals("dev-notify@medilabo.fr", contact.getEmail());
        assertEquals("https://medilabo.fr/internal/support", contact.getUrl());
    }

    @Test
    void customOpenAPI_ShouldConfigureLicense() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Info info = result.getInfo();
        License license = info.getLicense();

        assertNotNull(license);
        assertEquals("Propriétaire - MediLabo Solutions", license.getName());
        assertEquals("https://medilabo.fr/licenses", license.getUrl());
    }

    @Test
    void customOpenAPI_ShouldHaveExactlyFourErrorResponses() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Components components = result.getComponents();
        Map<String, ApiResponse> responses = components.getResponses();

        assertNotNull(responses);
        assertEquals(4, responses.size());
        assertTrue(responses.containsKey("Unauthorized"));
        assertTrue(responses.containsKey("Forbidden"));
        assertTrue(responses.containsKey("NotFound"));
        assertTrue(responses.containsKey("InternalError"));
    }

    @Test
    void customOpenAPI_ShouldHaveExactlyOneSecurityScheme() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Components components = result.getComponents();
        Map<String, SecurityScheme> securitySchemes = components.getSecuritySchemes();

        assertNotNull(securitySchemes);
        assertEquals(1, securitySchemes.size());
        assertTrue(securitySchemes.containsKey("bearerAuth"));
    }

    @Test
    void customOpenAPI_ShouldHaveExactlyOneSecurityRequirement() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        List<SecurityRequirement> securityRequirements = result.getSecurity();
        assertNotNull(securityRequirements);
        assertEquals(1, securityRequirements.size());
    }

    @Test
    void customOpenAPI_SecuritySchemeDescription_ShouldBeDescriptive() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Components components = result.getComponents();
        SecurityScheme securityScheme = components.getSecuritySchemes().get("bearerAuth");

        String description = securityScheme.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("JWT"));
        assertTrue(description.contains("cookie"));
        assertTrue(description.contains("tester les routes"));
    }

    @Test
    void customOpenAPI_ErrorResponsesDescriptions_ShouldBeSpecific() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Components components = result.getComponents();
        Map<String, ApiResponse> responses = components.getResponses();

        // Unauthorized
        ApiResponse unauthorized = responses.get("Unauthorized");
        assertTrue(unauthorized.getDescription().contains("Token manquant") ||
                unauthorized.getDescription().contains("invalide") ||
                unauthorized.getDescription().contains("expiré"));

        // Forbidden
        ApiResponse forbidden = responses.get("Forbidden");
        assertTrue(forbidden.getDescription().contains("permissions nécessaires"));

        // NotFound
        ApiResponse notFound = responses.get("NotFound");
        assertTrue(notFound.getDescription().contains("conversation") ||
                notFound.getDescription().contains("message"));

        // InternalError
        ApiResponse internalError = responses.get("InternalError");
        assertTrue(internalError.getDescription().contains("technique") ||
                internalError.getDescription().contains("envoi") ||
                internalError.getDescription().contains("récupération"));
    }

    @Test
    void customOpenAPI_InfoDescription_ShouldMentionAllActors() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        String description = result.getInfo().getDescription();
        assertNotNull(description);
        assertTrue(description.contains("Praticiens"));
        assertTrue(description.contains("Organisateurs"));
        assertTrue(description.contains("patients"));
    }

    @Test
    void customOpenAPI_ContactInfo_ShouldBeComplete() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Contact contact = result.getInfo().getContact();

        assertAll(
                () -> assertNotNull(contact.getName()),
                () -> assertNotNull(contact.getEmail()),
                () -> assertNotNull(contact.getUrl()),
                () -> assertTrue(contact.getEmail().contains("@")),
                () -> assertTrue(contact.getUrl().startsWith("https://"))
        );
    }

    @Test
    void customOpenAPI_LicenseInfo_ShouldBeComplete() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        License license = result.getInfo().getLicense();

        assertAll(
                () -> assertNotNull(license.getName()),
                () -> assertNotNull(license.getUrl()),
                () -> assertTrue(license.getName().contains("Propriétaire") ||
                        license.getName().contains("MediLabo")),
                () -> assertTrue(license.getUrl().startsWith("https://"))
        );
    }

    @Test
    void customOpenAPI_ShouldBeSingleton() {
        // When
        OpenAPI result1 = openApiConfig.customOpenAPI();
        OpenAPI result2 = openApiConfig.customOpenAPI();

        // Then
        assertNotSame(result1, result2); // Chaque appel @Bean crée une nouvelle instance
    }

    @Test
    void customOpenAPI_AllComponents_ShouldBeConfigured() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Components components = result.getComponents();

        assertAll(
                () -> assertNotNull(components.getSecuritySchemes()),
                () -> assertNotNull(components.getResponses()),
                () -> assertFalse(components.getSecuritySchemes().isEmpty()),
                () -> assertFalse(components.getResponses().isEmpty())
        );
    }

    @Test
    void customOpenAPI_SecuritySchemeType_ShouldBeHTTP() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        SecurityScheme securityScheme = result.getComponents()
                .getSecuritySchemes()
                .get("bearerAuth");

        assertEquals(SecurityScheme.Type.HTTP, securityScheme.getType());
    }

    @Test
    void customOpenAPI_SecuritySchemeScheme_ShouldBeBearer() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        SecurityScheme securityScheme = result.getComponents()
                .getSecuritySchemes()
                .get("bearerAuth");

        assertEquals("bearer", securityScheme.getScheme());
    }

    @Test
    void customOpenAPI_SecuritySchemeBearerFormat_ShouldBeJWT() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        SecurityScheme securityScheme = result.getComponents()
                .getSecuritySchemes()
                .get("bearerAuth");

        assertEquals("JWT", securityScheme.getBearerFormat());
    }

    @Test
    void customOpenAPI_ShouldHaveCompleteInfo() {
        // When
        OpenAPI result = openApiConfig.customOpenAPI();

        // Then
        Info info = result.getInfo();

        assertAll(
                () -> assertNotNull(info.getTitle()),
                () -> assertNotNull(info.getVersion()),
                () -> assertNotNull(info.getDescription()),
                () -> assertNotNull(info.getContact()),
                () -> assertNotNull(info.getLicense())
        );
    }
}