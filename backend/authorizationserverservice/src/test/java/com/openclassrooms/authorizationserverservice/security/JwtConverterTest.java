package com.openclassrooms.authorizationserverservice.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;


import static org.assertj.core.api.Assertions.assertThat;

class JwtConverterTest {

    private JwtConverter jwtConverter;

    @BeforeEach
    void setUp() {
        jwtConverter = new JwtConverter();
    }

    @Test
    @DisplayName("Convert : Devrait transformer un JWT avec authorities en JwtAuthenticationToken")
    void convert_ShouldReturnJwtAuthenticationTokenWithAuthorities() {
        // GIVEN
        String authoritiesStr = "ROLE_USER,ROLE_ADMIN,MFA_REQUIRED";
        String subject = "user-test-id";

        // On construit un vrai objet Jwt pour éviter les problèmes de mock sur les classes finales
        Jwt jwt = Jwt.withTokenValue("mock-token-value")
                .header("alg", "none")
                .subject(subject)
                .claim("authorities", authoritiesStr)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // WHEN
        JwtAuthenticationToken result = jwtConverter.convert(jwt);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(subject);
        assertThat(result.getToken()).isEqualTo(jwt);

        // Vérification de la conversion des autorités
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "MFA_REQUIRED");
    }

    @Test
    @DisplayName("Convert : Devrait fonctionner même si le claim authorities est vide")
    void convert_WhenAuthoritiesEmpty_ShouldReturnEmptyAuthorityList() {
        // GIVEN
        Jwt jwt = Jwt.withTokenValue("mock-token-value")
                .header("alg", "none")
                .subject("guest")
                .claim("authorities", "")
                .build();

        // WHEN
        JwtAuthenticationToken result = jwtConverter.convert(jwt);

        // THEN
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities()).isEmpty();
    }
}