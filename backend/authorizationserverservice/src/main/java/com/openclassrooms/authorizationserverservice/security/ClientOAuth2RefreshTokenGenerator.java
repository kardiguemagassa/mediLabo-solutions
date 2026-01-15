package com.openclassrooms.authorizationserverservice.security;

import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;

/**
 * Générateur personnalisé de refresh tokens OAuth2 pour les clients.
 * <p>
 * Cette classe implémente {@link OAuth2TokenGenerator} pour produire des instances
 * de {@link OAuth2RefreshToken} lorsque le serveur OAuth2 émet des tokens de rafraîchissement.
 * Elle utilise un générateur de chaînes encodées en Base64 pour assurer des tokens sécurisés
 * et uniques.
 * </p>
 * <p>
 * Le token généré respecte la durée de vie configurée dans {@link org.springframework.security.oauth2.server.authorization.settings.TokenSettings}
 * du client enregistré.
 * </p>
 *
 * <strong>Exemple d'utilisation :</strong>
 * <pre>
 * OAuth2RefreshToken refreshToken = clientOAuth2RefreshTokenGenerator.generate(context);
 * </pre>
 *
 * <p>
 * Cette classe est utilisée par le serveur d'autorisation pour générer des refresh tokens
 * lors du flux OAuth2 (Authorization Code / Refresh Token).
 * </p>
 *
 * <strong>Sécurité :</strong> Les tokens générés sont aléatoires, encodés en Base64 URL-safe
 * et suffisamment longs (96 caractères) pour réduire les risques de collisions ou de prédiction.
 * </p>
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Component
public class ClientOAuth2RefreshTokenGenerator implements OAuth2TokenGenerator<OAuth2RefreshToken> {
    /** Générateur de clés aléatoires encodées en Base64 pour les refresh tokens.
     * */
    private final StringKeyGenerator refreshTokenGenerator = new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    public ClientOAuth2RefreshTokenGenerator() {}

    /**
     * Génère un refresh token OAuth2 si le contexte correspond à un type REFRESH_TOKEN.
     *
     * @param context le contexte du token OAuth2 contenant les informations du client et du token
     * @return {@link OAuth2RefreshToken} généré ou {@code null} si le type n'est pas REFRESH_TOKEN
     */
    public OAuth2RefreshToken generate(OAuth2TokenContext context) {
        if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
            return null;
        }  else {
            Instant issuedAt = Instant.now();
            Instant expiresAt = issuedAt.plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
            return new OAuth2RefreshToken(this.refreshTokenGenerator.generateKey(), issuedAt, expiresAt);
        }
    }
}
