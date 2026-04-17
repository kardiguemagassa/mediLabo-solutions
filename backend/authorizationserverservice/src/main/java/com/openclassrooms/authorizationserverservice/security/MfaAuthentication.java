package com.openclassrooms.authorizationserverservice.security;

import lombok.Getter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

/**
 * Représente une authentification temporaire utilisée pendant un processus de Multi-Factor Authentication (MFA).
 * Cette classe encapsule une authentification primaire déjà validée
 * (login + mot de passe, OAuth2, etc.) mais qui n’a pas encore terminé le
 * processus MFA (TOTP, SMS, Email, etc.).
 * Objectif
 * Elle permet au système de sécurité Spring de :
 * Reconnaître un utilisateur partiellement authentifié
 * Limiter ses accès tant que le second facteur n’est pas validé
 * Autoriser uniquement les endpoints MFA (ex: /mfa/verify)
 * Stratégie de sécurité
 * L’utilisateur est techniquement connecté, mais son niveau de confiance est
 * limité. Il n’obtient pas encore les rôles réels de l’utilisateur
 * (USER, ADMIN, etc.), seulement un rôle temporaire MFA
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Getter
public class MfaAuthentication extends AnonymousAuthenticationToken {
    private final Authentication primaryAuthentication;

    /**
     * Crée une authentification MFA temporaire à partir d’une authentification primaire.
     * L'authentification primaire (login/password, OAuth2…) est stockée mais
     * n'est pas encore considérée comme totalement validée.
     * Un rôle temporaire (ex: {@code ROLE_MFA_REQUIRED}) est ajouté afin de contrôler
     * précisément les endpoints accessibles pendant la phase MFA.
     *
     * @param authentication l'authentification primaire déjà validée
     * @param authority le rôle temporaire accordé pendant le processus MFA
     */
    public MfaAuthentication(Authentication authentication, String authority) {
        super("anonymous", "anonymousUser", createAuthorityList("ROLE_ANONYMOUS", authority));
        this.primaryAuthentication = authentication;
    }

    /**
     * Retourne le principal réel issu de l’authentification primaire.
     * Cela permet à Spring Security de conserver l’identité de l’utilisateur
     * (username, userId, etc.) même pendant la phase MFA.
     *
     * @return le principal authentifié (souvent un UserDetails ou JWT subject)
     */
    @Override
    public Object getPrincipal() {
        return this.primaryAuthentication.getPrincipal();
    }
}
