package com.openclassrooms.gatewayserverservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convertisseur JWT pour extraire les autorités (rôles) - Version REACTIVE.
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Slf4j
@Component
public class JwtConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private static final String AUTHORITY_KEY = "authorities";

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        log.debug("JWT converti pour l'utilisateur: {} avec {} autorités",
                jwt.getSubject(), authorities.size());

        return Mono.just(new JwtAuthenticationToken(jwt, authorities, jwt.getSubject()));
    }

    /**
     * Extrait les autorités (rôles) depuis le JWT.
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object authoritiesClaim = jwt.getClaims().get(AUTHORITY_KEY);

        if (authoritiesClaim == null) {
            log.warn("Aucune autorité trouvée dans le JWT pour l'utilisateur: {}", jwt.getSubject());
            return Collections.emptyList();
        }

        // Si c'est une chaîne de caractères séparée par des virgules
        if (authoritiesClaim instanceof String) {
            String authoritiesStr = (String) authoritiesClaim;
            return Stream.of(authoritiesStr.split(","))
                    .map(String::trim)
                    .filter(auth -> !auth.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        // Si c'est déjà une collection
        if (authoritiesClaim instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<String> authoritiesCollection = (Collection<String>) authoritiesClaim;
            return authoritiesCollection.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        log.warn("Format d'autorités non reconnu dans le JWT: {}", authoritiesClaim.getClass());
        return Collections.emptyList();
    }
}
