package com.openclassrooms.authorizationserverservice.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Configuration des composants JWT pour l'Authorization Server.
 *
 * <p>
 * Cette classe fournit :
 * <ul>
 *     <li>Un {@link JwtDecoder} pour décoder et valider les JWT émis par le serveur.</li>
 *     <li>Un {@link JWKSource} exposant la clé publique au format JWK pour permettre
 *     aux clients et ressources de vérifier la signature des tokens JWT.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Les clés RSA sont générées et fournies via {@link KeyUtils}.
 * </p>
 *
 * <p>
 * Auteur : FirstName LastName
 * Version : 1.0
 * Email : magassa***REMOVED_USER***@gmail.com
 * Depuis : 2026-05-01
 * </p>
 */

@Configuration
@RequiredArgsConstructor
public class JwtConfiguration {
    private final KeyUtils keyUtils;

    /**
     * Crée un {@link JwtDecoder} utilisant la clé publique RSA pour valider les JWT.
     *
     * <p>
     * Le {@link JwtDecoder} est utilisé par Spring Security pour décoder et vérifier
     * l'authenticité et l'intégrité des tokens d'accès JWT.
     * </p>
     *
     * @return {@link JwtDecoder} configuré avec la clé publique RSA
     * @throws JOSEException si la récupération de la clé publique échoue
     */
    @Bean
    public JwtDecoder jwtDecoder() throws JOSEException {
        return NimbusJwtDecoder.withPublicKey(keyUtils.getRSAKeyPair().toRSAPublicKey()).build();
    }

    /**
     * Crée un {@link JWKSource} exposant la clé publique RSA sous forme de JWK.
     *
     * <p>
     * Ce bean est utilisé par l'Authorization Server pour fournir le JWK Set endpoint
     * afin que les clients OAuth2 puissent récupérer la clé publique et vérifier
     * les signatures des JWT émis.
     * </p>
     *
     * @return {@link JWKSource} configuré avec la clé RSA
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = keyUtils.getRSAKeyPair();
        JWKSet set = new JWKSet(rsaKey);
        return (j, sc) -> j.select(set);
    }
}