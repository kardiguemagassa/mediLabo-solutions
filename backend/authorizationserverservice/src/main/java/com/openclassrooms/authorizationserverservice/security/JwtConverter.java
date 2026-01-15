package com.openclassrooms.authorizationserverservice.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import static org.springframework.security.core.authority.AuthorityUtils.commaSeparatedStringToAuthorityList;

/**
 * Convertisseur JWT → Spring Security Authentication.
 *
 * Sans elle :
 * JWT serait valide
 * mais Spring ne saurait pas qui est l’utilisateur ni ce qu’il a le droit de faire.
 *
 * <p>
 * Cette classe est utilisée par le Resource Server pour transformer
 * un token JWT valide en un objet {@link JwtAuthenticationToken}
 * exploitable par Spring Security.
 * </p>
 *
 * <h2>Rôle principal</h2>
 * <ul>
 *   <li>Lire les claims du JWT</li>
 *   <li>Extraire les rôles / autorités stockés dans le token</li>
 *   <li>Construire l’objet d’authentification Spring</li>
 * </ul>
 *
 * <p>
 * Le JWT contient un claim personnalisé appelé <b>"authorities"</b>
 * ajouté lors de la génération du token dans {@code AuthorizationServerConfig}.
 * Ce claim contient une liste de rôles séparés par des virgules, par exemple :
 * </p>
 *
 * <pre>
 * "authorities": "ROLE_USER,ROLE_ADMIN,MFA_REQUIRED"
 * </pre>
 *
 * <p>
 * Ce convertisseur lit cette valeur et la transforme en une liste de
 * {@link org.springframework.security.core.GrantedAuthority} utilisée
 * par Spring Security pour l'autorisation des endpoints.
 * </p>
 *
 * <p>
 * Cette classe est automatiquement utilisée grâce à :
 * </p>
 *
 * <pre>
 * .oauth2ResourceServer(oauth2 ->
 *     oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtConverter()))
 * )
 * </pre>
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Component
public class JwtConverter implements Converter<Jwt, JwtAuthenticationToken> {
    /** Nom du claim JWT contenant les rôles de l’utilisateur.*/
    private static final String AUTHORITY_KEY = "authorities";

    /**
     * Convertit un JWT valide en objet {@link JwtAuthenticationToken}.
     *
     * <p>
     * Cette méthode est appelée automatiquement par Spring Security
     * lorsqu'une requête HTTP arrive avec un header :
     * </p>
     *
     * <pre>
     * Authorization: Bearer &lt;jwt&gt;
     * </pre>
     *
     * <p>
     * Étapes :
     * </p>
     * <ol>
     *   <li>Lire le claim <b>authorities</b> depuis le JWT</li>
     *   <li>Convertir la chaîne "ROLE_USER,ROLE_ADMIN" en objets GrantedAuthority</li>
     *   <li>Créer un {@link JwtAuthenticationToken}</li>
     * </ol>
     *
     * @param jwt token JWT validé et décodé
     * @return authentification Spring contenant l’utilisateur et ses rôles
     */
    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        var claims = (String) jwt.getClaims().get(AUTHORITY_KEY);
        var authorities = commaSeparatedStringToAuthorityList(claims);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
