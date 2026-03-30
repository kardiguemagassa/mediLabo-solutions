package com.openclassrooms.authorizationserverservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.openclassrooms.authorizationserverservice.domain.Response;
import com.openclassrooms.authorizationserverservice.exception.ApiException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

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
 * Utilitaires pour gérer les requêtes HTTP et formater les réponses API.
 * Permet de générer des messages d'erreur, d'écrire les réponses JSON et de centraliser la gestion des exceptions.
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-05-01
 * @email magassa***REMOVED_USER***@gmail.com
 */

public class RequestUtils {

    /**
     * Retourne un message utilisateur basé sur le code d'erreur HTTP dans la requête.
     *
     * @param request requête HTTP
     * @return message correspondant au code d'erreur ou un message générique
     */
    public static String getMessage(HttpServletRequest request) {
        var status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return String.format("%s - Page non trouvée", statusCode);
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return String.format("%s - Erreur interne du serveur", statusCode);
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return String.format("%s - Accès interdit", statusCode);
            }
        }
        return "Une erreur est survenue";
    }

    /**
     * Lambda pour écrire un objet {@link Response} dans la réponse HTTP au format JSON.
     *
     * Cette fonction prend en entrée :
     *  - un {@link HttpServletResponse} qui représente la réponse HTTP envoyée au client,
     *  - un objet {@link Response} qui contient les données à retourner.
     *
     * Fonctionnement :
     *  1. Récupère le flux de sortie (`OutputStream`) de la réponse HTTP.
     *  2. Sérialise l'objet {@link Response} en JSON via Jackson ({@link ObjectMapper}).
     *  3. Écrit le JSON dans le flux de sortie.
     *  4. Vide le flux (`flush`) pour s'assurer que les données sont envoyées immédiatement.
     *
     * Gestion des erreurs :
     *  - Si une exception se produit lors de la sérialisation ou de l'écriture, elle est convertie en {@link ApiException}.
     *
     * Cette lambda permet de centraliser l'écriture JSON pour toutes les réponses d'erreur et d'éviter de répéter le code dans chaque exception handler.
     */
    private static final BiConsumer<HttpServletResponse, Response> writeResponse = (servletResponse, response) -> {
        try {
            var outputStream = servletResponse.getOutputStream();
            new ObjectMapper().writeValue(outputStream, response);
            outputStream.flush();
        } catch (Exception exception) {
            throw new ApiException(exception.getMessage());
        }
    };

    /**
     * Crée un objet {@link Response} pour les erreurs personnalisées.
     * Objectif : Créer un objet Response simplement avec des infos passées en paramètres.
     * Quand l’utiliser : Quand tu veux créer manuellement un Response pour une erreur personnalisée,
     * par exemple dans un service ou un contrôleur, sans gérer toutes les exceptions globales.
     * Caractéristique : Tu fournis toi-même le message et l’exception.
     *
     * @param message message d'erreur à afficher
     * @param exception message de l'exception
     * @param request requête HTTP
     * @param status code HTTP à retourner
     * @return objet {@link Response} construit
     */
    public static Response handleErrorResponse(String message, String exception, HttpServletRequest request, HttpStatusCode status) {
        return new Response(now().toString(), status.value(), request.getRequestURI(), HttpStatus.valueOf(status.value()), message, exception, emptyMap());
    }

    /**
     * Objectif : Gérer toutes les exceptions possibles globalement (Spring Security, JWT, ApiException, etc.) et renvoyer une réponse JSON correcte directement au client.
     * Quand l’utiliser : Dans un exception handler global (@ControllerAdvice ou filtre Spring Security), pour centraliser la gestion d’erreurs.
     * Caractéristique : Elle décide automatiquement du code HTTP et du message à renvoyer selon le type de l’exception.
     * @param request requête HTTP
     * @param response réponse HTTP
     * @param exception exception levée
     */
    public static void handleErrorResponse(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        if (exception instanceof AccessDeniedException) {
            var apiResponse = getErrorResponse(request, response, exception, FORBIDDEN);
            writeResponse.accept(response, apiResponse);
        } else if (exception instanceof InvalidBearerTokenException) {
            var apiResponse = getErrorResponse(request, response, exception, UNAUTHORIZED);
            writeResponse.accept(response, apiResponse);
        } else if (exception instanceof InsufficientAuthenticationException) {
            var apiResponse = getErrorResponse(request, response, exception, UNAUTHORIZED);
            writeResponse.accept(response, apiResponse);
        } else if (exception instanceof MismatchedInputException) {
            var apiResponse = getErrorResponse(request, response, exception, BAD_REQUEST);
            writeResponse.accept(response, apiResponse);
        } else if (exception instanceof DisabledException || exception instanceof LockedException || exception instanceof BadCredentialsException || exception instanceof CredentialsExpiredException || exception instanceof ApiException) {
            var apiResponse = getErrorResponse(request, response, exception, BAD_REQUEST);
            writeResponse.accept(response, apiResponse);
        } else {
            var apiResponse = getErrorResponse(request, response, exception, INTERNAL_SERVER_ERROR);
            writeResponse.accept(response, apiResponse);
        }
    }

    /**
     * Crée une réponse standardisée pour une requête réussie.
     *
     * @param request requête HTTP
     * @param data données à inclure dans la réponse
     * @param message message à afficher
     * @param status code HTTP
     * @return objet {@link Response} construit
     */
    public static Response getResponse(HttpServletRequest request, Map<?, ?> data, String message, HttpStatus status) {
        return new Response(now().toString(), status.value(), request.getRequestURI(), status, message, EMPTY, data);
    }

    /**
     * Lambda pour déterminer la raison d'une erreur à renvoyer à l'utilisateur final.
     *
     * Cette fonction prend en entrée :
     *  - une exception qui s'est produite,
     *  - un statut HTTP associé à la réponse.
     *
     * Elle retourne une chaîne de caractères compréhensible par l'utilisateur, en fonction :
     *  - du type de l'exception (ex: {@link DisabledException}, {@link LockedException}, {@link ApiException}),
     *  - ou du code HTTP (403, 401, 5xx, etc.).
     *
     * Cas traités :
     *  - 403 FORBIDDEN : "Vous n'avez pas suffisamment d'autorisation"
     *  - 401 UNAUTHORIZED : si le message contient "Jwt expired at" → "Votre session a expiré", sinon → "Vous n'êtes pas connecté."
     *  - Exceptions spécifiques comme DisabledException, LockedException, BadCredentialsException, CredentialsExpiredException, ApiException → renvoie le message de l'exception
     *  - Erreurs serveur 5xx → "Une erreur s'est produite. Veuillez réessayer."
     *  - Autres cas → chaîne vide.
     *
     * Cela permet de standardiser les messages d'erreur côté frontend tout en masquant les détails techniques inutiles.
     */
    private static final BiFunction<Exception, HttpStatus, String> errorReason = (exception, httpStatus) -> {
        if(httpStatus.isSameCodeAs(FORBIDDEN)) {
            return "Vous n'avez pas suffisamment d'autorisation";
        }
        if(httpStatus.isSameCodeAs(UNAUTHORIZED)) {
            return exception.getMessage().contains("Jwt expired at") ? "Votre session a expiré." : "Vous n'êtes pas connecté.";
        }
        if(exception instanceof DisabledException || exception instanceof LockedException || exception instanceof BadCredentialsException || exception instanceof CredentialsExpiredException || exception instanceof ApiException) {
            return exception.getMessage();
        }
        if(httpStatus.is5xxServerError()) {
            return "Une erreur s'est produite. Veuillez réessayer.";
        } else {
            return "";
        }
    };

    /**
     * Crée une réponse d'erreur structurée pour une exception.
     *
     * @param request requête HTTP
     * @param response réponse HTTP
     * @param exception exception levée
     * @param status code HTTP à retourner
     * @return objet {@link Response} construit
     */
    private static Response getErrorResponse(HttpServletRequest request, HttpServletResponse response, Exception exception, HttpStatus status) {
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(status.value());
        return new Response(now().toString(), status.value(), request.getRequestURI(), HttpStatus.valueOf(status.value()), errorReason.apply(exception, status), getRootCauseMessage(exception), emptyMap());
    }
}
