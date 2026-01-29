package com.openclassrooms.patientservice.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Intercepteur WebClient pour la propagation automatique du token JWT.
 *
 * Récupère le token depuis le SecurityContext et l'ajoute
 * à toutes les requêtes sortantes vers les autres microservices.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
@Slf4j
@Component
public class WebClientInterceptor {

    /**
     * Filter pour propager automatiquement le token JWT.
     */
    public static ExchangeFilterFunction jwtAuthorizationFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String token = jwtAuth.getToken().getTokenValue();
                log.debug("Propagating JWT token to: {}", request.url());

                ClientRequest newRequest = ClientRequest.from(request)
                        .header(AUTHORIZATION, "Bearer " + token)
                        .build();

                return Mono.just(newRequest);
            }

            log.warn("No JWT token found in SecurityContext for request: {}", request.url());
            return Mono.just(request);
        });
    }

    /**
     * Filter pour logger les requêtes sortantes.
     */
    public static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug(">>> {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    /**
     * Filter pour logger les réponses.
     */
    public static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("<<< Status: {}", response.statusCode());
            return Mono.just(response);
        });
    }

    /**
     * Filter pour logger les erreurs.
     */
    public static ExchangeFilterFunction handleError() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                log.error("Error response: {} from service", response.statusCode());
            }
            return Mono.just(response);
        });
    }
}