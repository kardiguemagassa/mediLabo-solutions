package com.openclassrooms.gatewayserverservice.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.gatewayserverservice.domain.Response;
import com.openclassrooms.gatewayserverservice.exception.ApiException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;

import static java.time.LocalTime.now;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.springframework.http.HttpStatus.*;

/**
 * Utilitaires pour gérer les erreurs dans le Gateway (version REACTIVE).
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

public class RequestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gère les réponses d'erreur de manière réactive.
     *
     * @param exchange L'échange web serveur (requête + réponse)
     * @param exception L'exception à traiter
     * @return Mono<Void> pour la programmation réactive
     */
    public static Mono<Void> handleErrorResponse(ServerWebExchange exchange, Exception exception) {
        HttpStatus status = determineHttpStatus(exception);
        Response apiResponse = getErrorResponse(exchange, exception, status);

        return writeResponse(exchange, apiResponse, status);
    }

    /**
     * Détermine le code HTTP selon le type d'exception.
     */
    private static HttpStatus determineHttpStatus(Exception exception) {
        if (exception instanceof AccessDeniedException) {
            return FORBIDDEN;
        } else if (exception instanceof InvalidBearerTokenException ||
                exception instanceof InsufficientAuthenticationException) {
            return UNAUTHORIZED;
        } else if (exception instanceof DisabledException ||
                exception instanceof LockedException ||
                exception instanceof BadCredentialsException ||
                exception instanceof CredentialsExpiredException ||
                exception instanceof ApiException) {
            return BAD_REQUEST;
        } else {
            return INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * Écrit la réponse de manière réactive.
     */
    private static Mono<Void> writeResponse(ServerWebExchange exchange, Response response, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(new ApiException("Erreur lors de l'écriture de la réponse: " + e.getMessage()));
        }
    }

    /**
     * Fonction pour déterminer le message d'erreur selon le statut HTTP.
     */
    private static final BiFunction<Exception, HttpStatus, String> errorReason = (exception, httpStatus) -> {
        if (httpStatus.isSameCodeAs(FORBIDDEN)) {
            return "Vous n'avez pas suffisamment d'autorisation";
        }
        if (httpStatus.isSameCodeAs(UNAUTHORIZED)) {
            return exception.getMessage() != null && exception.getMessage().contains("Jwt expired at")
                    ? "Votre session a expiré."
                    : "Vous n'êtes pas connecté.";
        }
        if (exception instanceof DisabledException ||
                exception instanceof LockedException ||
                exception instanceof BadCredentialsException ||
                exception instanceof CredentialsExpiredException ||
                exception instanceof ApiException) {
            return exception.getMessage();
        }
        if (httpStatus.is5xxServerError()) {
            return "Une erreur interne du serveur s'est produite.";
        } else {
            return "Une erreur s'est produite. Veuillez réessayer.";
        }
    };

    /**
     * Crée un objet Response pour l'erreur.
     */
    private static Response getErrorResponse(ServerWebExchange exchange, Exception exception, HttpStatus status) {
        return new Response(
                now().toString(),
                status.value(),
                exchange.getRequest().getPath().value(),
                status,
                errorReason.apply(exception, status),
                getRootCauseMessage(exception),
                emptyMap()
        );
    }
}
