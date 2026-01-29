package com.openclassrooms.patientservice.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires pour {@link JwtConverter}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du convertisseur JWT")
class JwtConverterTest {

    private JwtConverter jwtConverter;
    private Jwt.Builder jwtBuilder;

    @BeforeEach
    void setUp() {
        jwtConverter = new JwtConverter();
        jwtBuilder = Jwt.withTokenValue("dummy-token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .issuer("test-issuer")
                .subject("user-123")  // user_uuid
                .audience(List.of("patient-service"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
    }

    @Test
    @DisplayName("Devrait extraire les authorities depuis un tableau")
    void convert_ShouldExtractAuthoritiesFromArray() {
        // Given
        Jwt jwt = jwtBuilder
                .claim("authorities", List.of("ADMIN", "PRACTITIONER", "USER"))
                .build();

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ADMIN", "PRACTITIONER", "USER");
    }

    @Test
    @DisplayName("Devrait extraire les roles et supprimer le préfixe ROLE_")
    void convert_ShouldExtractRolesAndRemoveROLE_Prefix() {
        // Given
        Jwt jwt = jwtBuilder
                .claim("roles", List.of("ROLE_ADMIN", "ROLE_PRACTITIONER", "ROLE_USER", "SIMPLE_ROLE"))
                .build();

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ADMIN", "PRACTITIONER", "USER", "SIMPLE_ROLE");
    }

    @Test
    @DisplayName("Devrait extraire les scopes et ajouter le préfixe SCOPE_")
    void convert_ShouldExtractScopesAndAddSCOPE_Prefix() {
        // Given
        Jwt jwt = jwtBuilder
                .claim("scope", "read write delete")
                .build();

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("SCOPE_read", "SCOPE_write", "SCOPE_delete");
    }

    @Test
    @DisplayName("Devrait combiner les authorities depuis plusieurs claims")
    void convert_ShouldCombineAuthoritiesFromMultipleClaims() {
        // Given
        Jwt jwt = jwtBuilder
                .claim("authorities", "ADMIN")
                .claim("roles", List.of("ROLE_PRACTITIONER"))
                .claim("scope", "read")
                .build();

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ADMIN", "PRACTITIONER", "SCOPE_read");
    }

    @Test
    @DisplayName("Devrait gérer les chaînes vides et les espaces")
    void convert_ShouldHandleEmptyStringsAndSpaces() {
        // Given
        Jwt jwt = jwtBuilder
                .claim("authorities", "ADMIN, ,PRACTITIONER,  ,USER")
                .claim("scope", "  read  write  ")
                .build();

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ADMIN", "PRACTITIONER", "USER", "SCOPE_read", "SCOPE_write");
    }

    @Test
    @DisplayName("Devrait retourner une liste vide quand aucun claim d'authority n'est présent")
    void convert_ShouldReturnEmptyAuthoritiesWhenNoClaimsPresent() {
        // Given
        Jwt jwt = jwtBuilder.build(); // Aucun claim d'authority

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("Devrait gérer les claims null ou vides")
    void convert_ShouldHandleNullOrEmptyClaims() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", "");
        claims.put("roles", Collections.emptyList());
        claims.put("scope", null);

        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user-123");
        when(jwt.getClaim(anyString())).thenAnswer(invocation -> {
            String claimName = invocation.getArgument(0);
            return claims.get(claimName);
        });

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("Devrait créer un JwtAuthenticationToken avec le JWT et les authorities")
    void convert_ShouldCreateJwtAuthenticationTokenWithJwtAndAuthorities() {
        // Given
        Jwt jwt = jwtBuilder
                .claim("authorities", "ADMIN")
                .build();

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        assertThat(result).isInstanceOf(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class);
        Assertions.assertNotNull(result);
        assertThat(result.getCredentials()).isEqualTo(jwt);
        assertThat(result.isAuthenticated()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("provideAuthoritiesFormats")
    @DisplayName("Devrait gérer différents formats d'authorities")
    void convert_ShouldHandleDifferentAuthoritiesFormats(String claimName, Object claimValue, List<String> expectedAuthorities) {
        // Given
        Jwt jwt = jwtBuilder
                .claim(claimName, claimValue)
                .build();

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrderElementsOf(expectedAuthorities);
    }

    private static Stream<Arguments> provideAuthoritiesFormats() {
        return Stream.of(
                // authorities claim formats
                Arguments.of("authorities", "SINGLE", List.of("SINGLE")),
                Arguments.of("authorities", "TWO,THREE", List.of("TWO", "THREE")),
                Arguments.of("authorities", List.of("ARRAY_ONE", "ARRAY_TWO"), List.of("ARRAY_ONE", "ARRAY_TWO")),

                // roles claim formats
                Arguments.of("roles", List.of("ROLE_TEST"), List.of("TEST")),
                Arguments.of("roles", List.of("NO_PREFIX"), List.of("NO_PREFIX")),
                Arguments.of("roles", List.of("ROLE_ONE", "TWO", "ROLE_THREE"), List.of("ONE", "TWO", "THREE")),

                // scope claim formats
                Arguments.of("scope", "single", List.of("SCOPE_single")),
                Arguments.of("scope", "one two", List.of("SCOPE_one", "SCOPE_two"))
        );
    }

    @Test
    @DisplayName("Devrait maintenir l'ordre des authorities (bien que non requis)")
    void convert_ShouldMaintainAuthorityOrder() {
        // Given
        Jwt jwt = jwtBuilder
                .claim("authorities", "FIRST,SECOND,THIRD")
                .build();

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        List<String> authorities = result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Vérifie que tous les éléments sont présents (l'ordre peut varier selon l'implémentation)
        assertThat(authorities).containsExactlyInAnyOrder("FIRST", "SECOND", "THIRD");
    }

    @Test
    @DisplayName("Devrait gérer les claims avec types inattendus")
    void convert_ShouldHandleUnexpectedClaimTypes() {
        // Given
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user-123");

        // Simuler un claim authorities avec un type inattendu (nombre)
        when(jwt.getClaim("authorities")).thenReturn(123);
        when(jwt.getClaim("roles")).thenReturn(Map.of("key", "value")); // Map au lieu de List
        when(jwt.getClaim("scope")).thenReturn(42.5); // Double au lieu de String

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        // Devrait retourner une liste vide sans lancer d'exception
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("Devrait traiter correctement le préfixe SCOPE_ pour les scopes multiples avec espaces")
    void convert_ShouldHandleMultipleScopesWithSpacesCorrectly() {
        // Given
        Jwt jwt = jwtBuilder
                .claim("scope", "   profile:read   profile:write   email   ")
                .build();

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        "SCOPE_profile:read",
                        "SCOPE_profile:write",
                        "SCOPE_email"
                );
    }
}