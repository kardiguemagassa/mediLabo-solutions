package com.openclassrooms.assessmentservice.security;

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
import java.util.Collection;
import java.util.List;

/**
 * Convertit le JWT en Authentication avec les authorities appropriées.
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */

@Slf4j
@Component
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        String principalName = jwt.getClaimAsString("sub");

        log.debug("Converting JWT for user: {} with authorities: {}", principalName, authorities);

        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Extraire le rôle depuis le claim "role"
        String role = jwt.getClaimAsString("role");
        if (role != null && !role.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            log.debug("Added role from 'role' claim: ROLE_{}", role.toUpperCase());
        }

        // Extraire les rôles depuis le claim "roles" (liste)
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            authorities.addAll(roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                    .toList());
            log.debug("Added roles from 'roles' claim: {}", roles);
        }

        // Extraire les authorities depuis le claim "authorities"
        List<String> authoritiesClaim = jwt.getClaimAsStringList("authorities");
        if (authoritiesClaim != null) {
            authorities.addAll(authoritiesClaim.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList());
            log.debug("Added authorities from 'authorities' claim: {}", authoritiesClaim);
        }

        // Extraire les scopes
        String scope = jwt.getClaimAsString("scope");
        if (scope != null) {
            String[] scopes = scope.split(" ");
            for (String s : scopes) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
            }
        }

        return authorities;
    }
}