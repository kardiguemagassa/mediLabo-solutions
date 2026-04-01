package com.openclassrooms.authorizationserverservice.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

/**
 * AuthenticationProvider personnalisé pour l'authentification des clients OAuth2
 * utilisant la méthode {@link ClientAuthenticationMethod#NONE}.
 * Cette classe est responsable de :
 * Vérifier si un client existe dans le {@link RegisteredClientRepository}
 * Valider que la méthode d'authentification utilisée est bien enregistrée pour ce client
 * Lancer les exceptions OAuth2 standards en cas d'erreurs d'authentification
 * Cette implémentation est principalement utilisée pour les clients OAuth2 qui
 * n'utilisent pas de secret (refresh token flow, clients publics, etc.).
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Component
public class ClientAuthenticationProvider implements AuthenticationProvider {

    /** Repository contenant les clients OAuth2 enregistrés. */
    private final RegisteredClientRepository registeredClientRepository;

    /**
     * Constructeur avec injection du {@link RegisteredClientRepository}.
     *
     * @param registeredClientRepository repository pour récupérer les clients enregistrés
     */
    public ClientAuthenticationProvider(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    /**
     * Authentifie un client OAuth2.
     * Vérifie si le client existe et si sa méthode d'authentification
     * correspond à celle attendue. Lève une {@link OAuth2AuthenticationException}
     * avec le code standard {@link OAuth2ErrorCodes#INVALID_CLIENT} en cas d'erreur.
     *
     * @param authentication objet d'authentification contenant le clientId et la méthode
     * @return un {@link ClientRefreshTokenAuthentication} valide si l'authentification réussit
     * @throws AuthenticationException si le client est invalide ou la méthode non enregistrée
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ClientRefreshTokenAuthentication clientRefreshTokenAuthentication = (ClientRefreshTokenAuthentication) authentication;
        if(!ClientAuthenticationMethod.NONE.equals(clientRefreshTokenAuthentication.getClientAuthenticationMethod())) {
            return null;
        }
        String clientId = clientRefreshTokenAuthentication.getPrincipal().toString();
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if(registeredClient == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_CLIENT,
                    "Le client n'est pas valide",
                    null
            ));
        }
        if(!registeredClient.getClientAuthenticationMethods().contains(clientRefreshTokenAuthentication.getClientAuthenticationMethod())) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_CLIENT,
                    "La méthode d'authentification n'est pas enregistrée auprès du client.",
                    null
            ));
        }
        return new ClientRefreshTokenAuthentication(registeredClient);
    }

    /**
     * Indique si ce {@link AuthenticationProvider} supporte le type d'authentification fourni.
     *
     * @param authentication classe d'authentification à vérifier
     * @return true si le type est {@link ClientRefreshTokenAuthentication}, false sinon
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return ClientRefreshTokenAuthentication.class.isAssignableFrom(authentication);
    }
}
