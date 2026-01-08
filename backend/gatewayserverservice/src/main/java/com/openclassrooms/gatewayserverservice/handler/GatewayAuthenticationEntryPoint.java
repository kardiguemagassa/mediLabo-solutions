package com.openclassrooms.gatewayserverservice.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.openclassrooms.gatewayserverservice.utils.RequestUtils.handleErrorResponse;

/**
 * Point d'entrée pour les erreurs d'authentification (401 Unauthorized) - Version REACTIVE.
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Slf4j
@Component
public class GatewayAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        log.warn("Authentification requise pour l'URI: {} - Raison: {}",
                exchange.getRequest().getPath().value(),
                exception.getMessage());

        return handleErrorResponse(exchange, exception);
    }
}
