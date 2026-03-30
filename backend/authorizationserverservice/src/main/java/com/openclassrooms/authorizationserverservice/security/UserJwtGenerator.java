package com.openclassrooms.authorizationserverservice.security;

import org.springframework.lang.Nullable;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static com.openclassrooms.authorizationserverservice.util.UserUtils.getUser;

/**
 * Générateur personnalisé de JSON Web Token (JWT) pour les utilisateurs.
 *
 * <p>
 * Cette classe implémente {@link OAuth2TokenGenerator} pour produire des tokens JWT
 * adaptés aux besoins de l'application, incluant :
 * <ul>
 *     <li>Les tokens d'accès OAuth2 (Access Tokens)</li>
 *     <li>Les tokens d'identité OpenID Connect (ID Tokens)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Le générateur configure dynamiquement :
 * <ul>
 *     <li>Les informations de l'utilisateur (UUID, subject)</li>
 *     <li>Le client destinataire (audience)</li>
 *     <li>La durée de validité du token (issuedAt, expiresAt)</li>
 *     <li>Les scopes autorisés</li>
 *     <li>Les claims OIDC (nonce, sid, auth_time) si applicable</li>
 * </ul>
 * </p>
 *
 * <p>
 * La signature du JWT est réalisée via {@link JwtEncoder} avec l'algorithme RSA
 * (par défaut RS256), et peut être personnalisée grâce à un {@link OAuth2TokenCustomizer}.
 * </p>
 *
 * <h2>Utilisation</h2>
 * <pre>{@code
 * JwtEncoder encoder = ...;
 * UserJwtGenerator generator = UserJwtGenerator.init(encoder);
 * generator.setJwtCustomizer(customizer); // optionnel
 * Jwt jwt = generator.generate(context);
 * }</pre>
 *
 * <h2>Remarques</h2>
 * <ul>
 *     <li>La classe est finale et ne peut pas être héritée.</li>
 *     <li>Ne gère que les tokens de type {@link OAuth2TokenType#ACCESS_TOKEN}
 *         et les ID Tokens OIDC.</li>
 *     <li>Intègre la logique pour gérer les contextes de refresh token et d'authorization code.</li>
 * </ul>
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-05-01
 */

public final class UserJwtGenerator implements OAuth2TokenGenerator<Jwt> {
    private final JwtEncoder jwtEncoder;
    private OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer;

    private UserJwtGenerator(JwtEncoder jwtEncoder) {
        Assert.notNull(jwtEncoder, "jwtEncoder cannot be null");
        this.jwtEncoder = jwtEncoder;
    }

    /**
     * Génère un JWT à partir du contexte OAuth2 fourni.
     *
     * <p>
     * La méthode :
     * <ul>
     *     <li>Retourne {@code null} si le type de token n’est pas Access Token ou ID Token OIDC.</li>
     *     <li>Configure dynamiquement le {@link JwtClaimsSet} avec les informations utilisateur, client et scopes.</li>
     *     <li>Gère les claims OIDC spécifiques comme <code>nonce</code>, <code>sid</code>, <code>auth_time</code>.</li>
     *     <li>Applique le {@link OAuth2TokenCustomizer} si défini.</li>
     *     <li>Encode le JWT via le {@link JwtEncoder} et retourne le token final.</li>
     * </ul>
     * </p>
     *
     * @param context le contexte du token contenant le client, le principal, les scopes, etc.
     * @return le {@link Jwt} généré ou {@code null} si le type de token n’est pas pris en charge
     */
    @Nullable
    @Override
    public Jwt generate(OAuth2TokenContext context) {
        if (context.getTokenType() == null || (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType()) && !OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue()))) {
            return null;
        }
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType()) &&
                !OAuth2TokenFormat.SELF_CONTAINED.equals(context.getRegisteredClient().getTokenSettings().getAccessTokenFormat())) {
            return null;
        }
        String issuer = null;
        if (context.getAuthorizationServerContext() != null) {
            issuer = context.getAuthorizationServerContext().getIssuer();
        }
        RegisteredClient registeredClient = context.getRegisteredClient();
        Instant issuedAt = Instant.now();
        Instant expiresAt;
        JwsAlgorithm jwsAlgorithm = SignatureAlgorithm.RS256;
        if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
            // TODO Allow configuration for ID Token time-to-live
            expiresAt = issuedAt.plus(30, ChronoUnit.MINUTES);
            if (registeredClient.getTokenSettings().getIdTokenSignatureAlgorithm() != null) {
                jwsAlgorithm = registeredClient.getTokenSettings().getIdTokenSignatureAlgorithm();
            }
        }
        else {
            expiresAt = issuedAt.plus(registeredClient.getTokenSettings().getAccessTokenTimeToLive());
        }
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();
        if (StringUtils.hasText(issuer)) {
            claimsBuilder.issuer(issuer);
        }
        // RETRIEVE USER UUID
        claimsBuilder
                .subject(getUser(context.getPrincipal()).getUserUuid())
                .audience(Collections.singletonList(registeredClient.getClientId()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString());
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            claimsBuilder.notBefore(issuedAt);
            if (!CollectionUtils.isEmpty(context.getAuthorizedScopes())) {
                claimsBuilder.claim(OAuth2ParameterNames.SCOPE, context.getAuthorizedScopes());
            }
        } else if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
            claimsBuilder.claim(IdTokenClaimNames.AZP, registeredClient.getClientId());
            if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(context.getAuthorizationGrantType())) {
                OAuth2AuthorizationRequest authorizationRequest = context.getAuthorization().getAttribute(
                        OAuth2AuthorizationRequest.class.getName());
                String nonce = (String) authorizationRequest.getAdditionalParameters().get(OidcParameterNames.NONCE);
                if (StringUtils.hasText(nonce)) {
                    claimsBuilder.claim(IdTokenClaimNames.NONCE, nonce);
                }
                SessionInformation sessionInformation = context.get(SessionInformation.class);
                if (sessionInformation != null) {
                    claimsBuilder.claim("sid", sessionInformation.getSessionId());
                    claimsBuilder.claim(IdTokenClaimNames.AUTH_TIME, sessionInformation.getLastRequest());
                }
            } else if (AuthorizationGrantType.REFRESH_TOKEN.equals(context.getAuthorizationGrantType())) {
                OidcIdToken currentIdToken = context.getAuthorization().getToken(OidcIdToken.class).getToken();
                if (currentIdToken.hasClaim("sid")) {
                    claimsBuilder.claim("sid", currentIdToken.getClaim("sid"));
                }
                if (currentIdToken.hasClaim(IdTokenClaimNames.AUTH_TIME)) {
                    claimsBuilder.claim(IdTokenClaimNames.AUTH_TIME, currentIdToken.<Date>getClaim(IdTokenClaimNames.AUTH_TIME));
                }
            }
        }
        JwsHeader.Builder jwsHeaderBuilder = JwsHeader.with(jwsAlgorithm);
        if (this.jwtCustomizer != null) {
            JwtEncodingContext.Builder jwtContextBuilder = JwtEncodingContext.with(jwsHeaderBuilder, claimsBuilder)
                    .registeredClient(context.getRegisteredClient())
                    .principal(context.getPrincipal())
                    .authorizationServerContext(context.getAuthorizationServerContext())
                    .authorizedScopes(context.getAuthorizedScopes())
                    .tokenType(context.getTokenType())
                    .authorizationGrantType(context.getAuthorizationGrantType());
            if (context.getAuthorization() != null) {
                jwtContextBuilder.authorization(context.getAuthorization());
            }
            if (context.getAuthorizationGrant() != null) {
                jwtContextBuilder.authorizationGrant(context.getAuthorizationGrant());
            }
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                SessionInformation sessionInformation = context.get(SessionInformation.class);
                if (sessionInformation != null) {
                    jwtContextBuilder.put(SessionInformation.class, sessionInformation);
                }
            }
            JwtEncodingContext jwtContext = jwtContextBuilder.build();
            this.jwtCustomizer.customize(jwtContext);
        }
        JwsHeader jwsHeader = jwsHeaderBuilder.build();
        JwtClaimsSet claims = claimsBuilder.build();
        Jwt jwt = this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims));
        return jwt;
    }

    /**
     * Définit un customizer pour personnaliser le JWT avant l’encodage.
     *
     * <p>
     * Utile pour ajouter des claims spécifiques à l’application (ex : rôles, permissions, métadonnées).
     * </p>
     *
     * @param jwtCustomizer le {@link OAuth2TokenCustomizer} à appliquer
     * @throws IllegalArgumentException si {@code jwtCustomizer} est null
     */
    public void setJwtCustomizer(OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {
        Assert.notNull(jwtCustomizer, "jwtCustomizer cannot be null");
        this.jwtCustomizer = jwtCustomizer;
    }

    /**
     * Initialise une instance de {@link UserJwtGenerator} avec le {@link JwtEncoder} fourni.
     *
     * <p>
     * Cette méthode statique facilite la création d'instance et assure que le {@link JwtEncoder}
     * n’est jamais null.
     * </p>
     *
     * @param jwtEncoder le composant {@link JwtEncoder} utilisé pour signer les JWT
     * @return une nouvelle instance de {@link UserJwtGenerator}
     */
    public static UserJwtGenerator init(JwtEncoder jwtEncoder) {
        return new UserJwtGenerator(jwtEncoder);
    }
}
