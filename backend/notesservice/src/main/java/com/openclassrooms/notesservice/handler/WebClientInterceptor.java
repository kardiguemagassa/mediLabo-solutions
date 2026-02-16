package com.openclassrooms.notesservice.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

@Slf4j
public class WebClientInterceptor {

    private WebClientInterceptor() {}

    /**
     * Propage automatiquement le JWT de manière RÉACTIVE.
     */
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