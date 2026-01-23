package com.openclassrooms.patientservice.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convertisseur JWT pour extraire les authorities
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String SCOPE_CLAIM = "scope";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String principalClaimValue = jwt.getSubject(); // user_uuid

        return new JwtAuthenticationToken(jwt, authorities, principalClaimValue);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Essayer d'extraire depuis "authorities" (comma-separated)
        Object authoritiesClaim = jwt.getClaim(AUTHORITIES_CLAIM);
        if (authoritiesClaim instanceof String authoritiesString) {
            if (!authoritiesString.isEmpty()) {
                return Stream.of(authoritiesString.split(","))
                        .map(String::trim)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }
        }

        // Essayer d'extraire depuis "scope" (space-separated)
        Object scopeClaim = jwt.getClaim(SCOPE_CLAIM);
        if (scopeClaim instanceof String scopeString) {
            if (!scopeString.isEmpty()) {
                return Stream.of(scopeString.split(" "))
                        .map(String::trim)
                        .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}