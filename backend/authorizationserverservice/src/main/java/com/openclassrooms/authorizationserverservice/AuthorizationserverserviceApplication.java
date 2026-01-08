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
 * AuthorizationserverserviceApplication
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

	@Bean
	public ApplicationRunner applicationRunner(RegisteredClientRepository registeredClientRepository) {
		return args -> {
			try{
				if(registeredClientRepository.findByClientId("client") == null) {
					var registeredClient = RegisteredClient.withId(randomUUID().toString())
							.clientId("client").clientSecret("secret")
							.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
							//.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
							//.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
							.authorizationGrantTypes(types -> {
								types.add(AuthorizationGrantType.AUTHORIZATION_CODE);
								//types.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
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
							.postLogoutRedirectUri("http://127.0.0.1:9000")
							.clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
							.tokenSettings(TokenSettings.builder().refreshTokenTimeToLive(Duration.ofDays(900))
									//.accessTokenTimeToLive(Duration.ofDays(300)).build()).build();
									.accessTokenTimeToLive(Duration.ofMinutes(1)).build()).build();
					registeredClientRepository.save(registeredClient);
				}
			} catch (Exception exception) {
				System.out.println(exception.getMessage());
				throw exception;
			}
		};
	}

}
