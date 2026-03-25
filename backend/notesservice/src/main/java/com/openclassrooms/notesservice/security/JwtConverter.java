package com.openclassrooms.notesservice.security;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Convertit le JWT en Authentication avec les authorities appropriées.
 *
 * Gère les formats suivants :
 * - "authorities" comme String séparée par des virgules (ex: "SUPER_ADMIN,user:read")
 * - "authorities" comme List JSON
 * - "role" / "roles" claims
 *
 * Ajoute automatiquement le préfixe "ROLE_" pour les rôles connus.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-02
 */
@Slf4j
@Component
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final List<String> KNOWN_ROLES = List.of(
            "SUPER_ADMIN", "ADMIN", "HEAD_PRACTITIONER", "PRACTITIONER", "DOCTOR", "USER", "ORGANIZER"
    );

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        String principalName = jwt.getClaimAsString("sub");

        log.debug("Converting JWT for user: {} with authorities: {}", principalName, authorities);

        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // ─────────────────────────────────────────────────────────────────────
        // 1. Extraire depuis le claim "role" (String simple)
        // ─────────────────────────────────────────────────────────────────────
        String role = jwt.getClaimAsString("role");
        if (role != null && !role.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            log.debug("Added role from 'role' claim: ROLE_{}", role.toUpperCase());
        }

        // ─────────────────────────────────────────────────────────────────────
        // 2. Extraire depuis le claim "roles" (List)
        // ─────────────────────────────────────────────────────────────────────
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.forEach(r -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()));
                log.debug("Added role from 'roles' claim: ROLE_{}", r.toUpperCase());
            });
        }

        // ─────────────────────────────────────────────────────────────────────
        // 3. Extraire depuis le claim "authorities" (String OU List)
        // ─────────────────────────────────────────────────────────────────────
        Object authoritiesClaim = jwt.getClaim("authorities");

        if (authoritiesClaim != null) {
            List<String> authList;

            // Cas 1: authorities est une String séparée par des virgules
            if (authoritiesClaim instanceof String authString) {
                authList = Arrays.asList(authString.split(","));
                log.debug("Authorities claim is a String, splitting by comma");
            }
            // Cas 2: authorities est une List
            else if (authoritiesClaim instanceof List<?>) {
                authList = ((List<?>) authoritiesClaim).stream()
                        .map(Object::toString)
                        .toList();
                log.debug("Authorities claim is a List");
            }
            else {
                authList = List.of();
                log.warn("Authorities claim has unexpected type: {}", authoritiesClaim.getClass());
            }

            // Traiter chaque authority
            for (String auth : authList) {
                String trimmedAuth = auth.trim();

                if (trimmedAuth.isEmpty()) continue;

                // Si c'est un rôle connu, ajouter avec préfixe ROLE_
                if (KNOWN_ROLES.contains(trimmedAuth.toUpperCase())) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + trimmedAuth.toUpperCase()));
                    log.debug("Added role: ROLE_{}", trimmedAuth.toUpperCase());
                }

                // Ajouter aussi l'authority brute (pour les permissions comme "user:read")
                authorities.add(new SimpleGrantedAuthority(trimmedAuth));
                log.debug("Added authority: {}", trimmedAuth);
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // 4. Extraire les scopes
        // ─────────────────────────────────────────────────────────────────────
        String scope = jwt.getClaimAsString("scope");
        if (scope != null && !scope.isEmpty()) {
            Arrays.stream(scope.split(" "))
                    .filter(s -> !s.isEmpty())
                    .forEach(s -> {
                        authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
                        log.debug("Added scope: SCOPE_{}", s);
                    });
        }

        log.debug("Total authorities extracted: {}", authorities.size());
        return authorities;
    }
}