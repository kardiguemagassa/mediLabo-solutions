package com.openclassrooms.gatewayserverservice.handler;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import static com.openclassrooms.gatewayserverservice.utils.RequestUtils.handleErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.openclassrooms.gatewayserverservice.utils.RequestUtils.handleErrorResponse;

/**
 * Gestionnaire des erreurs d'accès refusé (403 Forbidden) - Version REACTIVE.
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Slf4j
@Component
public class GatewayAccessDeniedHandler implements ServerAccessDeniedHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException exception) {
        log.warn("Accès refusé pour l'URI: {} - Raison: {}",
                exchange.getRequest().getPath().value(),
                exception.getMessage());

        return handleErrorResponse(exchange, exception);
    }
}

