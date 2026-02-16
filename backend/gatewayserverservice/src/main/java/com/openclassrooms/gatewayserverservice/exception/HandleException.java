package com.openclassrooms.gatewayserverservice.exception;

import com.openclassrooms.gatewayserverservice.domain.Response;
import com.openclassrooms.gatewayserverservice.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static java.time.LocalTime.now;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
public class HandleException {

    @ExceptionHandler(ApiException.class)
    public Mono<ResponseEntity<Response>> apiException(ApiException exception, ServerWebExchange exchange) {
        return createResponse(BAD_REQUEST, exception.getMessage(), exception, exchange);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Mono<ResponseEntity<Response>> badCredentialsException(BadCredentialsException exception, ServerWebExchange exchange) {
        return createResponse(UNAUTHORIZED, "Identifiants incorrects.", exception, exchange);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<Response>> accessDeniedException(AccessDeniedException exception, ServerWebExchange exchange) {
        return createResponse(FORBIDDEN, "Accès refusé. Vous n'avez pas les permissions nécessaires.", exception, exchange);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Response>> globalException(Exception exception, ServerWebExchange exchange) {
        log.error("Exception non gérée : ", exception);
        return createResponse(INTERNAL_SERVER_ERROR, "Une erreur interne s'est produite au niveau du Gateway.", exception, exchange);
    }

    /**
     * Méthode pivot pour uniformiser la création de ResponseEntity<Response>
     */
    private Mono<ResponseEntity<Response>> createResponse(HttpStatus status, String message, Exception ex, ServerWebExchange exchange) {
        Response response = new Response(
                now().toString(),
                status.value(),
                exchange.getRequest().getPath().value(),
                status,
                message,
                getRootCauseMessage(ex),
                emptyMap()
        );
        return Mono.just(new ResponseEntity<>(response, status));
    }
}