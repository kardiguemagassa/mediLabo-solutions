package com.openclassrooms.notesservice.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

@Slf4j
public class WebClientInterceptor {

    private WebClientInterceptor() {}

    /**
     * Propage automatiquement le JWT.
     * Supporte à la fois le contexte Servlet et Réactif.
     */
    public static ExchangeFilterFunction jwtAuthorizationFilter() {
        return (request, next) -> {
            // 1. Essayer d'abord le contexte Servlet (synchrone)
            Authentication servletAuth = SecurityContextHolder.getContext().getAuthentication();
            if (servletAuth != null && servletAuth.getPrincipal() instanceof Jwt jwt) {
                log.debug("Propagating JWT (Servlet context) to: {}", request.url());
                ClientRequest newRequest = ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue())
                        .build();
                return next.exchange(newRequest);
            }

            // 2. Sinon, essayer le contexte Réactif
            return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .filter(auth -> auth.getPrincipal() instanceof Jwt)
                    .map(auth -> (Jwt) auth.getPrincipal())
                    .map(jwt -> {
                        log.debug("Propagating JWT (Reactive context) to: {}", request.url());
                        return ClientRequest.from(request)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue())
                                .build();
                    })
                    .defaultIfEmpty(request)
                    .doOnNext(req -> {
                        if (req == request) {
                            log.warn("No JWT found in security context for: {}", request.url());
                        }
                    })
                    .flatMap(next::exchange);
        };
    }

    public static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.info(">>> API Request: {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    public static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("<<< API Response Status: {}", response.statusCode());
            return Mono.just(response);
        });
    }
}