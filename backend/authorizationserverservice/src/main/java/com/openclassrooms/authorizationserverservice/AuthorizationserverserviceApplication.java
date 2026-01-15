package com.openclassrooms.authorizationserverservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;

import static java.util.UUID.randomUUID;

/**
 * Point d'entrée de l'application Authorization Server.
 * <p>
 * Cette classe configure également un client OAuth2 "client" à l'initialisation
 * si celui-ci n'existe pas déjà dans le {@link RegisteredClientRepository}.
 * </p>
 *
 * Fonctionnalités principales :
 * <ul>
 *     <li>Activer le service de découverte pour Spring Cloud avec {@link EnableDiscoveryClient}</li>
 *     <li>Configurer un client OAuth2 avec code d'autorisation et refresh token</li>
 *     <li>Définir les scopes OpenID et OIDC standard (email, profile, phone, address)</li>
 *     <li>Configurer les URLs de redirection et de post-logout</li>
 *     <li>Configurer les durées de vie des tokens (access token et refresh token)</li>
 * </ul>
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@SpringBootApplication
@EnableDiscoveryClient
public class AuthorizationserverserviceApplication {

	@Value("$UI_APP_URL")
	private String redirectUri;

	public static void main(String[] args) {
		SpringApplication.run(AuthorizationserverserviceApplication.class, args);
	}

	/**
	 * Bean d'initialisation permettant de créer automatiquement un client OAuth2
	 * dans le {@link RegisteredClientRepository} si celui-ci n'existe pas.
	 *
	 * @param registeredClientRepository repository pour enregistrer le client OAuth2
	 * @return {@link ApplicationRunner} exécuté au démarrage de l'application
	 */
	@Bean
	public ApplicationRunner applicationRunner(RegisteredClientRepository registeredClientRepository) {
		return args -> {
			try{
				// Vérifie si le client "client" existe déjà
				if(registeredClientRepository.findByClientId("client") == null) {
					// Création d'un nouveau client OAuth2
					var registeredClient = RegisteredClient.withId(randomUUID().toString())
							.clientId("client").clientSecret("secret")
							.clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // // Méthode d'authentification du client
							//.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
							//.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
							.authorizationGrantTypes(types -> { // Types de grants autorisés
								types.add(AuthorizationGrantType.AUTHORIZATION_CODE);
								//types.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
								types.add(AuthorizationGrantType.REFRESH_TOKEN);
							})
							.scopes(scopes -> {  // Scopes autorisés
								scopes.add(OidcScopes.OPENID);
								scopes.add(OidcScopes.PROFILE);
								scopes.add(OidcScopes.EMAIL);
								scopes.add(OidcScopes.PHONE);
								scopes.add(OidcScopes.ADDRESS);
							})
							.redirectUri(redirectUri)  // URL de redirection après auth
							.postLogoutRedirectUri("http://127.0.0.1:9001") // URL après déconnexion
							.clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())// Consentement requis pour ce client
							.tokenSettings(TokenSettings.builder().refreshTokenTimeToLive(Duration.ofDays(900)) // Durée de vie du refresh token
									//.accessTokenTimeToLive(Duration.ofDays(300)).build()).build();
									.accessTokenTimeToLive(Duration.ofMinutes(1)).build()).build(); // Durée de vie du token d'accès
					// Sauvegarde le client dans le repository
					registeredClientRepository.save(registeredClient);
				}
			} catch (Exception exception) {
				System.out.println(exception.getMessage());
				throw exception;
			}
		};
	}

}
