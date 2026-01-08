package com.openclassrooms.authorizationserverservice.security;

import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * ClientRefreshTokenAuthentication
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */

public final class ClientRefreshTokenAuthentication extends OAuth2ClientAuthenticationToken {

    public ClientRefreshTokenAuthentication(String clientId) {
        super(clientId, ClientAuthenticationMethod.NONE, null, null); // USER NOT TOKEN
    }

    public ClientRefreshTokenAuthentication(RegisteredClient registeredClient) {
        super(registeredClient, ClientAuthenticationMethod.NONE, null);
    }
}
