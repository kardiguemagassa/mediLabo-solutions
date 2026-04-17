package com.openclassrooms.authorizationserverservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Convertisseur d’authentification pour les clients OAuth2 utilisant un **refresh token**.
 * Cette classe implémente {@link AuthenticationConverter} et est responsable de la conversion
 * d’une requête HTTP entrante en un objet {@link ClientRefreshTokenAuthentication} lorsque
 * le client tente de rafraîchir son token d’accès.
 * Fonctionnement :
 * Vérifie que le paramètre `grant_type` de la requête est bien `refresh_token`
 * Récupère le `client_id` depuis les paramètres de la requête
 * Si toutes les conditions sont satisfaites, retourne un {@link ClientRefreshTokenAuthentication}
 * Sinon, retourne `null` pour indiquer que cette conversion ne s’applique pas
 * Utilisation typique : intégré dans le {@link ClientAuthenticationProvider} pour authentifier
 * un client lors de la génération d'un nouveau access token à partir d’un refresh token
 * Cette classe est annotée {@link Component} pour être automatiquement détectée par Spring
 * et injectée dans la configuration de l’Authorization Server.
 * @see ClientRefreshTokenAuthentication
 * @see AuthenticationConverter
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Component
public final class ClientRefreshTokenAuthenticationConverter implements AuthenticationConverter {

    /**
     * Convertit une requête HTTP entrante en une authentification OAuth2 pour refresh token.
     * Fonctionnement :
     * Vérifie si le paramètre `grant_type` est égal à {@link AuthorizationGrantType#REFRESH_TOKEN}
     * Récupère le paramètre `client_id` de la requête
     * Si `grant_type` n’est pas `refresh_token` ou si `client_id` est absent ou vide, retourne {@code null}
     * Sinon, crée et retourne un objet {@link ClientRefreshTokenAuthentication} avec le `client_id`
     *
     * @param request la requête HTTP contenant les paramètres d’authentification OAuth2
     * @return un {@link ClientRefreshTokenAuthentication} si la conversion est possible,
     *         sinon {@code null} si le grant type ou le client_id ne correspondent pas
     */
    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if(!grantType.equals(AuthorizationGrantType.REFRESH_TOKEN.getValue())) {
            return null;
        }

        String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
        if(!StringUtils.hasText(clientId)) {
            return null;
        }

        return new ClientRefreshTokenAuthentication(clientId);
    }
}
