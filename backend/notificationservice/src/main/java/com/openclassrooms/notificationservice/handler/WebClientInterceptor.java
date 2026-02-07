package com.openclassrooms.notificationservice.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Intercepteurs pour WebClient.
 *
 * - Propagation automatique du JWT
 * - Logging des requêtes/réponses
 * - Gestion des erreurs
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Slf4j
public class WebClientInterceptor {

    private WebClientInterceptor() {
        // Classe utilitaire
    }

    /**
     * Propage automatiquement le JWT vers les services appelés.
     */
    public static ExchangeFilterFunction jwtAuthorizationFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                String token = jwt.getTokenValue();
                log.debug("Propagating JWT token to: {}", request.url());

                return Mono.just(ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
            }

            log.debug("No JWT token found in SecurityContext");
            return Mono.just(request);
        });
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

    /**
     * Gestion centralisée des erreurs HTTP.
     */
    public static ExchangeFilterFunction handleError() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("Error response: {} - {}", response.statusCode(), body);
                            return Mono.error(new WebClientResponseException(
                                    response.statusCode().value(),
                                    response.statusCode().toString(),
                                    response.headers().asHttpHeaders(),
                                    body.getBytes(),
                                    null
                            ));
                        });
            }
            return Mono.just(response);
        });
    }
}