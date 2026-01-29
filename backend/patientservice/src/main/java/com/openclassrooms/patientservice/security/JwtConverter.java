package com.openclassrooms.patientservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convertisseur JWT pour extraire les authorities depuis les claims.
 *
 * Supporte plusieurs formats de claims :
 * - "authorities": "ADMIN,PRACTITIONER" (comma-separated string)
 * - "authorities": ["ADMIN", "PRACTITIONER"] (array)
 * - "roles": ["ROLE_ADMIN", "ROLE_USER"] (array avec préfixe)
 * - "scope": "read write" (space-separated, préfixé avec SCOPE_)
 *
 * Le principal (subject) est le user_uuid.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-01-09
 */
@Slf4j
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String ROLES_CLAIM = "roles";
    private static final String SCOPE_CLAIM = "scope";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAllAuthorities(jwt);

        // Le subject contient le user_uuid
        String principal = jwt.getSubject();

        log.debug("JWT converted - Principal: {}, Authorities: {}", principal, authorities);

        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    /**
     * Extrait toutes les authorities depuis différents claims du JWT.
     */
    private Collection<GrantedAuthority> extractAllAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 1. Extraire depuis "authorities"
        authorities.addAll(extractFromAuthoritiesClaim(jwt));

        // 2. Extraire depuis "roles"
        authorities.addAll(extractFromRolesClaim(jwt));

        // 3. Extraire depuis "scope"
        authorities.addAll(extractFromScopeClaim(jwt));

        if (authorities.isEmpty()) {
            log.warn("No authorities found in JWT for subject: {}", jwt.getSubject());
        }

        return authorities;
    }

    /**
     * Extrait depuis "authorities" (string ou array).
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractFromAuthoritiesClaim(Jwt jwt) {
        Object claim = jwt.getClaim(AUTHORITIES_CLAIM);

        // Format: "ADMIN,PRACTITIONER,USER"
        if (claim instanceof String authString && !authString.isEmpty()) {
            return Stream.of(authString.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        }

        // Format: ["ADMIN", "PRACTITIONER"]
        if (claim instanceof List<?> authList) {
            return ((List<String>) authList).stream().map(String::trim).filter(s -> !s.isEmpty()).map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    /**
     * Extrait depuis "roles" (array, supprime préfixe ROLE_ si présent).
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractFromRolesClaim(Jwt jwt) {
        Object claim = jwt.getClaim(ROLES_CLAIM);

        if (claim instanceof List<?> rolesList) {
            return ((List<String>) rolesList).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                    .map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        }

        return List.of();
    }

    /**
     * Extrait depuis "scope" (space-separated, ajoute préfixe SCOPE_).
     */
    private Collection<GrantedAuthority> extractFromScopeClaim(Jwt jwt) {
        Object claim = jwt.getClaim(SCOPE_CLAIM);

        if (claim instanceof String scopeString && !scopeString.isEmpty()) {
            return Stream.of(scopeString.split(" "))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope)).collect(Collectors.toList());
        }

        return List.of();
    }
}