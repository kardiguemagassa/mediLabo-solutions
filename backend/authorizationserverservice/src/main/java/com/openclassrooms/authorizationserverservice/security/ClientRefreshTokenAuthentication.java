package com.openclassrooms.authorizationserverservice.security;

import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * Représente une authentification OAuth2 spécifique pour le **refresh token** d’un client.
 * Cette classe étend {@link OAuth2ClientAuthenticationToken} et est utilisée lorsqu’un client
 * OAuth2 souhaite obtenir un nouveau token d’accès via un **refresh token**.
 * Le mode d’authentification du client est défini sur {@link ClientAuthenticationMethod#NONE},
 * ce qui signifie que le client n’a pas besoin d’envoyer un secret pour cette authentification.
 * Elle fournit deux constructeurs :
 * {@link #ClientRefreshTokenAuthentication(String)} : créé une instance à partir d’un clientId
 * {@link #ClientRefreshTokenAuthentication(RegisteredClient)} : créé une instance à partir d’un {@link RegisteredClient} existant
 * Utilisation typique : dans le {@link ClientAuthenticationProvider} pour valider les clients qui
 * utilisent des refresh tokens afin de générer de nouveaux access tokens.
 * </p>
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

public final class ClientRefreshTokenAuthentication extends OAuth2ClientAuthenticationToken {

    /**
     * Crée une instance de {@link ClientRefreshTokenAuthentication} pour un client OAuth2
     * à utiliser avec le flux de refresh token.
     * Ce constructeur initialise l'authentification avec un {@code clientId} uniquement
     * La méthode d'authentification est définie à {@link ClientAuthenticationMethod#NONE}
     * et aucun token ni principal utilisateur n'est fourni à ce stade.
     * @param clientId l'identifiant du client OAuth2 qui souhaite rafraîchir un token
     */
    public ClientRefreshTokenAuthentication(String clientId) {
        super(clientId, ClientAuthenticationMethod.NONE, null, null); // USER NOT TOKEN
    }

    /**
     * Crée une instance de {@link ClientRefreshTokenAuthentication} à partir d'un {@link RegisteredClient}
     * Utilisé lorsque le client OAuth2 est déjà enregistré dans le { RegisteredClientRepository}.
     * La méthode d'authentification est définie à {@link ClientAuthenticationMethod#NONE}.
     * @param registeredClient le client OAuth2 enregistré associé à cette authentification
     */
    public ClientRefreshTokenAuthentication(RegisteredClient registeredClient) {
        super(registeredClient, ClientAuthenticationMethod.NONE, null);
    }
}
