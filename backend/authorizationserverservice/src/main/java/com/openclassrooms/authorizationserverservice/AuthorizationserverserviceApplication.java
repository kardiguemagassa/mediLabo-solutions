package com.openclassrooms.authorizationserverservice;

import lombok.extern.slf4j.Slf4j;
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

@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
public class AuthorizationserverserviceApplication {

	@Value("${ui.app.url}")
	private String redirectUri;

	@Value("${auth.token.access-token-ttl}")
	private long accessTokenTtlMinutes;

	@Value("${auth.token.refresh-token-ttl}")
	private long refreshTokenTtlDays;

	public static void main(String[] args) {
		SpringApplication.run(AuthorizationserverserviceApplication.class, args);
	}

	@Bean
	public ApplicationRunner applicationRunner(RegisteredClientRepository registeredClientRepository) {
		return args -> {
			try {
				log.info("Checking/Updating OAuth2 client configuration...");

				// Récupération du client existant pour conserver son ID technique s'il existe
				RegisteredClient existingClient = registeredClientRepository.findByClientId("client");
				String id = (existingClient != null) ? existingClient.getId() : randomUUID().toString();

				// Configuration alignée sur le .env
				RegisteredClient registeredClient = RegisteredClient.withId(id)
						.clientId("client")
						.clientSecret("{noop}secret") // {noop} pour le PasswordEncoder NoOp en dev
						.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
						.authorizationGrantTypes(types -> {
							types.add(AuthorizationGrantType.AUTHORIZATION_CODE);
							types.add(AuthorizationGrantType.REFRESH_TOKEN);
						})
						.scopes(scopes -> {
							scopes.add(OidcScopes.OPENID);
							scopes.add(OidcScopes.PROFILE);
							scopes.add(OidcScopes.EMAIL);
							scopes.add(OidcScopes.PHONE);
							scopes.add(OidcScopes.ADDRESS);
						})
						.redirectUri(redirectUri)
						.postLogoutRedirectUri(redirectUri) // On redirige  vers Angular au logout
						.clientSettings(ClientSettings.builder()
								.requireAuthorizationConsent(true)
								.build())
						.tokenSettings(TokenSettings.builder()
								.accessTokenTimeToLive(Duration.ofMinutes(accessTokenTtlMinutes))
								.refreshTokenTimeToLive(Duration.ofDays(refreshTokenTtlDays))
								.reuseRefreshTokens(true)
								.build())
						.build();

				// Sauvegarde (si ID existe, JDBC fera un update, sinon un insert)
				registeredClientRepository.save(registeredClient);

				log.info("OAuth2 client 'client' is ready. AccessTokenTTL: {}m, RefreshTokenTTL: {}d",
						accessTokenTtlMinutes, refreshTokenTtlDays);

			} catch (Exception exception) {
				log.error("Error during client initialization: {}", exception.getMessage());
				throw exception;
			}
		};
	}
}