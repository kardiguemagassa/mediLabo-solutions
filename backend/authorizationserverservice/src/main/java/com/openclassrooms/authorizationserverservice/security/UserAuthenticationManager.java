package com.openclassrooms.authorizationserverservice.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.stereotype.Component;

/**
 * Fournit le {@link AuthenticationManager} principal utilisé pour
 * l’authentification des utilisateurs (login / mot de passe).
 *
 * <p>
 * Cette classe relie le moteur d’authentification Spring Security
 * ({@link AuthenticationManager}) à l’implémentation métier
 * {@link UserAuthenticationProvider}.
 * </p>
 *
 * <p>
 * Le {@link ProviderManager} délègue le processus d’authentification
 * au(x) {@link org.springframework.security.authentication.AuthenticationProvider}
 * configuré(s), ici {@link UserAuthenticationProvider}.
 * </p>
 *
 * <h2>Rôle dans l’architecture</h2>
 * <ul>
 *     <li>Reçoit une demande d’authentification (username + password)</li>
 *     <li>La transmet au {@link UserAuthenticationProvider}</li>
 *     <li>Retourne une {@link org.springframework.security.core.Authentication} validée ou une exception</li>
 * </ul>
 *
 * <p>
 * Cette approche permet de séparer :
 * <ul>
 *     <li>La mécanique Spring Security</li>
 *     <li>La logique métier d’authentification (base de données, MFA, statut du compte, etc.)</li>
 * </ul>
 * </p>
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Component
@AllArgsConstructor
public class UserAuthenticationManager {
    private final UserAuthenticationProvider userAuthenticationProvider;

    /**
     * Crée le {@link AuthenticationManager} utilisé par Spring Security
     * pour authentifier les utilisateurs.
     *
     * <p>
     * Le {@link ProviderManager} est l’implémentation standard de Spring Security.
     * Il délègue la validation des identifiants à {@link UserAuthenticationProvider}.
     * </p>
     *
     * @return un gestionnaire d’authentification configuré avec le provider utilisateur
     */
    @Bean
    public AuthenticationManager authenticationManager () {
        return new ProviderManager(userAuthenticationProvider);
    }
}
