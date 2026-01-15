package com.openclassrooms.authorizationserverservice.security;

import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * Représente une authentification OAuth2 spécifique pour le **refresh token** d’un client.
 * <p>
 * Cette classe étend {@link OAuth2ClientAuthenticationToken} et est utilisée lorsqu’un client
 * OAuth2 souhaite obtenir un nouveau token d’accès via un **refresh token**.
 * Le mode d’authentification du client est défini sur {@link ClientAuthenticationMethod#NONE},
 * ce qui signifie que le client n’a pas besoin d’envoyer un secret pour cette authentification.
 * </p>
 *
 * <p>
 * Elle fournit deux constructeurs :
 * <ul>
 *     <li>{@link #ClientRefreshTokenAuthentication(String)} : créé une instance à partir d’un clientId.</li>
 *     <li>{@link #ClientRefreshTokenAuthentication(RegisteredClient)} : créé une instance à partir d’un {@link RegisteredClient} existant.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Utilisation typique : dans le {@link ClientAuthenticationProvider} pour valider les clients qui
 * utilisent des refresh tokens afin de générer de nouveaux access tokens.
 * </p>
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-05-01
 */

public final class ClientRefreshTokenAuthentication extends OAuth2ClientAuthenticationToken {

    /**
     * Crée une instance de {@link ClientRefreshTokenAuthentication} pour un client OAuth2
     * à utiliser avec le flux de refresh token.
     *
     * <p>
     * Ce constructeur initialise l'authentification avec un {@code clientId} uniquement.
     * La méthode d'authentification est définie à {@link ClientAuthenticationMethod#NONE},
     * et aucun token ni principal utilisateur n'est fourni à ce stade.
     * </p>
     *
     * @param clientId l'identifiant du client OAuth2 qui souhaite rafraîchir un token
     */
    public ClientRefreshTokenAuthentication(String clientId) {
        super(clientId, ClientAuthenticationMethod.NONE, null, null); // USER NOT TOKEN
    }

    /**
     * Crée une instance de {@link ClientRefreshTokenAuthentication} à partir d'un
     * {@link RegisteredClient}.
     *
     * <p>
     * Utilisé lorsque le client OAuth2 est déjà enregistré dans le { RegisteredClientRepository}.
     * La méthode d'authentification est définie à {@link ClientAuthenticationMethod#NONE}.
     * </p>
     *
     * @param registeredClient le client OAuth2 enregistré associé à cette authentification
     */
    public ClientRefreshTokenAuthentication(RegisteredClient registeredClient) {
        super(registeredClient, ClientAuthenticationMethod.NONE, null);
    }
}
