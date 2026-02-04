package com.openclassrooms.assessmentservice.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConverterTest {

    private JwtConverter jwtConverter;

    @BeforeEach
    void setUp() {
        jwtConverter = new JwtConverter();
    }

    @Test
    @DisplayName("Should extract all types of authorities from a complex JWT")
    void convert_ShouldExtractAllAuthorities() {
        // GIVEN: Un JWT contenant un mélange de claims
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-test")
                .claim("role", "admin") // Test claim unique
                .claim("roles", List.of("practitioner", "user")) // Test liste
                .claim("authorities", List.of("READ_PRIVILEGE")) // Test authorities directes
                .claim("scope", "patient:read patient:write") // Test scopes
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // WHEN
        AbstractAuthenticationToken authentication = jwtConverter.convert(jwt);

        // THEN
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("user-test");

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities).containsExactlyInAnyOrder(
                "ROLE_ADMIN",
                "ROLE_PRACTITIONER",
                "ROLE_USER",
                "READ_PRIVILEGE",
                "SCOPE_patient:read",
                "SCOPE_patient:write"
        );
    }

    @Test
    @DisplayName("Should handle empty or missing claims gracefully")
    void convert_ShouldHandleMissingClaims() {
        // GIVEN: Un JWT minimaliste
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "anonymous")
                .build();

        // WHEN
        AbstractAuthenticationToken authentication = jwtConverter.convert(jwt);

        // THEN
        Assertions.assertNotNull(authentication);
        assertThat(authentication.getAuthorities()).isEmpty();
    }
}