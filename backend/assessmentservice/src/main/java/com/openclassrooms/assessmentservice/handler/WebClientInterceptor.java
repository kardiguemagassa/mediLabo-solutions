package com.openclassrooms.assessmentservice.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

/**
 * Intercepteurs pour WebClient.
 *
 * - Propagation automatique du JWT
 * - Logging des requêtes/réponses
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Slf4j
public class WebClientInterceptor {

    private WebClientInterceptor() {

    }

    /**
     * Propage automatiquement le JWT vers les services appelés.
     */
    // À copier dans assessmentservice.handler.WebClientInterceptor
    public static ExchangeFilterFunction jwtAuthorizationFilter() {
        return (request, next) -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth.getPrincipal() instanceof Jwt)
                .map(auth -> (Jwt) auth.getPrincipal())
                .map(jwt -> {
                    log.debug("Propagating JWT to: {}", request.url());
                    return ClientRequest.from(request)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue())
                            .build();
                })
                .defaultIfEmpty(request)
                .flatMap(next::exchange);
    }

    /**
     * Log les requêtes sortantes.
     */
    public static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug(">>> {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    /**
     * Log les réponses reçues.
     */
    public static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("<<< Status: {}", response.statusCode());
            return Mono.just(response);
        });
    }
}