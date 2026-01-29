package com.openclassrooms.patientservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openclassrooms.patientservice.domain.Response;
import com.openclassrooms.patientservice.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static java.time.LocalTime.now;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Utilitaire centralisant la création et l'écriture des réponses HTTP
 * standardisées de l'API (succès et erreurs).
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-01-09
 */
public final class RequestUtils {

    // ObjectMapper singleton configuré (thread-safe)
    private static final ObjectMapper MAPPER = createObjectMapper();

    private RequestUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Crée et configure l'ObjectMapper avec support Java 8 Time.
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Écrit un objet {@link Response} dans la sortie HTTP au format JSON.
     */
    private static final BiConsumer<HttpServletResponse, Response> writeResponse = (servletResponse, response) -> {
        try {
            var outputStream = servletResponse.getOutputStream();
            MAPPER.writeValue(outputStream, response);
            outputStream.flush();
        } catch (Exception exception) {
            throw new ApiException(exception.getMessage());
        }
    };

    /**
     * Gère les exceptions et envoie une réponse JSON adaptée au client.
     */
    public static void handleErrorResponse(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        HttpStatus status = mapExceptionToStatus(exception);
        Response apiResponse = getErrorResponse(request, response, exception, status);
        writeResponse.accept(response, apiResponse);
    }

    /**
     * Mappe une exception vers le code HTTP approprié.
     */
    private static HttpStatus mapExceptionToStatus(Exception exception) {
        return switch (exception) {
            case AccessDeniedException ignored -> FORBIDDEN;
            case InvalidBearerTokenException ignored -> UNAUTHORIZED;
            case InsufficientAuthenticationException ignored -> UNAUTHORIZED;
            case MismatchedInputException ignored -> BAD_REQUEST;
            case DisabledException ignored -> BAD_REQUEST;
            case LockedException ignored -> BAD_REQUEST;
            case BadCredentialsException ignored -> BAD_REQUEST;
            case CredentialsExpiredException ignored -> BAD_REQUEST;
            case ApiException ignored -> BAD_REQUEST;
            default -> INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Construit une réponse API standard pour un traitement réussi.
     */
    public static Response getResponse(HttpServletRequest request, Map<?, ?> data, String message, HttpStatus status) {
        return new Response(now().toString(), status.value(), request.getRequestURI(), status, message, EMPTY, data);
    }

    /**
     * Détermine le message utilisateur en fonction du type d'erreur.
     */
    public static final BiFunction<Exception, HttpStatus, String> errorReason = (exception, httpStatus) -> {
        if (httpStatus.isSameCodeAs(FORBIDDEN)) {
            return "Vous n'avez pas suffisamment d'autorisations";
        }
        if (httpStatus.isSameCodeAs(UNAUTHORIZED)) {
            String message = exception.getMessage();
            if (message != null && message.contains("expired")) {
                return "Votre session a expiré";
            }
            return "Vous n'êtes pas connecté";
        }
        if (exception instanceof DisabledException ||
                exception instanceof LockedException ||
                exception instanceof BadCredentialsException ||
                exception instanceof CredentialsExpiredException ||
                exception instanceof ApiException) {
            return exception.getMessage();
        }
        if (httpStatus.is5xxServerError()) {
            return "Une erreur interne du serveur s'est produite";
        }
        return "Une erreur s'est produite. Veuillez réessayer.";
    };

    /**
     * Convertit une donnée de la Response vers un objet typé.
     */
    public static <T> T convertResponse(Response response, Class<T> type, String keyName) {
        return MAPPER.convertValue(response.data().get(keyName), type);
    }

    /**
     * Convertit une liste d'éléments de la Response vers une liste typée.
     */
    public static <T> List<T> convertResponseList(Response response, Class<T> type, String keyName) {
        return MAPPER.convertValue(
                response.data().get(keyName),
                MAPPER.getTypeFactory().constructCollectionType(List.class, type)
        );
    }

    /**
     * Construit une réponse d'erreur standardisée.
     */
    private static Response getErrorResponse(HttpServletRequest request, HttpServletResponse response,
                                             Exception exception, HttpStatus status) {
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(status.value());
        return new Response(
                now().toString(),
                status.value(),
                request.getRequestURI(),
                status,
                errorReason.apply(exception, status),
                getRootCauseMessage(exception),
                emptyMap()
        );
    }
}