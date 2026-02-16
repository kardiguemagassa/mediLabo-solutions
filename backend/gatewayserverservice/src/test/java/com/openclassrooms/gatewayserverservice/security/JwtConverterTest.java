package com.openclassrooms.gatewayserverservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

import java.util.Map;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;

class JwtConverterTest {

    private JwtConverter jwtConverter;

    @BeforeEach
    void setUp() {
        jwtConverter = new JwtConverter();
    }

    private Jwt createJwt(Map<String, Object> claims) {
        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                claims
        );
    }

    @Test
    void convert_ShouldWorkWithAuthoritiesAsString() {
        // GIVEN: Un JWT avec une chaîne "ROLE_USER, ROLE_ADMIN"
        Jwt jwt = createJwt(Map.of("sub", "user123", "authorities", "ROLE_USER, ROLE_ADMIN"));

        // WHEN & THEN
        StepVerifier.create(Objects.requireNonNull(jwtConverter.convert(jwt)))
                .assertNext(token -> {
                    assertEquals("user123", token.getName());
                    Collection<GrantedAuthority> authorities = token.getAuthorities();
                    assertEquals(2, authorities.size());
                    assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
                    assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
                })
                .verifyComplete();
    }

    @Test
    void convert_ShouldWorkWithAuthoritiesAsCollection() {
        // GIVEN: Un JWT avec une liste de rôles
        Jwt jwt = createJwt(Map.of(
                "sub", "user456",
                "authorities", Arrays.asList("ROLE_PATIENT", "ROLE_DOCTOR")
        ));

        // WHEN & THEN
        StepVerifier.create(Objects.requireNonNull(jwtConverter.convert(jwt)))
                .assertNext(token -> {
                    assertEquals(2, token.getAuthorities().size());
                    assertTrue(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT")));
                })
                .verifyComplete();
    }

    @Test
    void convert_ShouldReturnEmptyListWhenAuthoritiesClaimIsMissing() {
        // GIVEN: Pas de clé 'authorities'
        Jwt jwt = createJwt(Map.of("sub", "user789"));

        // WHEN & THEN
        StepVerifier.create(Objects.requireNonNull(jwtConverter.convert(jwt)))
                .assertNext(token -> assertTrue(token.getAuthorities().isEmpty()))
                .verifyComplete();
    }

    @Test
    void convert_ShouldReturnEmptyListWhenFormatIsUnknown() {
        // GIVEN: Un format inattendu (ex: un Integer)
        Jwt jwt = createJwt(Map.of("sub", "user000", "authorities", 12345));

        // WHEN & THEN
        StepVerifier.create(Objects.requireNonNull(jwtConverter.convert(jwt)))
                .assertNext(token -> assertTrue(token.getAuthorities().isEmpty()))
                .verifyComplete();
    }
}