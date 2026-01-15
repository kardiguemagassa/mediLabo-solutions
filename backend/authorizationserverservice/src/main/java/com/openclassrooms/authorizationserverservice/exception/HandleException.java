package com.openclassrooms.authorizationserverservice.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.openclassrooms.authorizationserverservice.domain.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

import static com.openclassrooms.authorizationserverservice.util.RequestUtils.handleErrorResponse;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Gestionnaire centralisé des exceptions pour l'API.
 *
 * <p>
 * Cette classe implémente un mécanisme global de gestion des erreurs pour l'application Spring Boot
 * en utilisant {@link RestControllerAdvice} et {@link ResponseEntityExceptionHandler}.
 * Elle intercepte les exceptions levées par les contrôleurs et les services et
 * renvoie une réponse structurée de type {@link com.openclassrooms.authorizationserverservice.domain.Response}.
 * </p>
 *
 * <p>
 * Objectifs principaux :
 * <ul>
 *     <li>Uniformiser les réponses d'erreurs JSON pour le frontend (React, Angular, etc.).</li>
 *     <li>Logger toutes les exceptions pour faciliter le debug et la traçabilité.</li>
 *     <li>Gérer les exceptions spécifiques comme {@link BadCredentialsException}, {@link LockedException}, {@link DisabledException}, {@link DataAccessException}.</li>
 *     <li>Gérer les erreurs génériques et non prévues pour éviter des fuites d'informations sensibles.</li>
 * </ul>
 * </p>
 *
 * <h2>Fonctionnement</h2>
 * <ul>
 *     <li>Chaque méthode annotée {@link ExceptionHandler} capture un type spécifique d'exception et construit
 *         une {@link ResponseEntity} appropriée avec le code HTTP correspondant.</li>
 *     <li>Les exceptions de validation ({@link MethodArgumentNotValidException}) sont transformées en
 *         messages clairs pour le frontend.</li>
 *     <li>Les erreurs SQL ou de transaction sont détectées et traduites en messages lisibles.</li>
 *     <li>Les erreurs d'accès ou d'authentification renvoient des messages adaptés à la sécurité.</li>
 * </ul>
 *
 * <h2>Notes importantes</h2>
 * <ul>
 *     <li>La méthode {@link #processErrorMessage(Exception)} contient la logique de traduction des messages
 *         techniques en messages compréhensibles pour l’utilisateur.</li>
 *     <li>Le logger {@link lombok.extern.slf4j.Slf4j} permet de suivre toutes les exceptions interceptées.</li>
 *     <li>Le {@link HttpServletRequest} est utilisé pour inclure le contexte de la requête dans la réponse d'erreur.</li>
 * </ul>
 *
 * <h2>Exemple d'utilisation</h2>
 * <pre>{@code
 * // Si un utilisateur envoie un mauvais mot de passe
 * POST /login avec email/mot de passe incorrect
 * -> HandleException.badCredentialsException(BadCredentialsException) est appelé
 * -> Retour JSON : { "message": "Adresse e-mail ou mot de passe incorrect", "details": "..." }
 * }</pre>
 *
 * <p>
 * Cette classe est essentielle pour garantir :
 * <ul>
 *     <li>Une API REST robuste et sécurisée</li>
 *     <li>Des messages d’erreur clairs et centralisés</li>
 *     <li>La traçabilité complète des erreurs pour l’administration et le debug</li>
 * </ul>
 * </p>
 *
 * @author Kardigué
 * @version 1.0
 * @since 2026-05-01
 */

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class HandleException extends ResponseEntityExceptionHandler implements ErrorController {
    private final HttpServletRequest request;

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest webRequest) {
        log.error(String.format("handleExceptionInternal: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(exception.getMessage(), getRootCauseMessage(exception), request, statusCode), statusCode);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode statusCode, WebRequest webRequest) {
        log.error(String.format("MethodArgumentNotValidException: %s", exception.getMessage()));
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        String fieldsMessage = fieldErrors.stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));
        return new ResponseEntity<>(handleErrorResponse(fieldsMessage, getRootCauseMessage(exception), request, statusCode), statusCode);
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<Response> sQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException exception) {
        log.error(String.format("SQLIntegrityConstraintViolationException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(exception.getMessage().contains("Duplicate entry") ? "L'information existe déjà." : exception.getMessage(), getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Response> badCredentialsException(BadCredentialsException exception) {
        log.error(String.format("BadCredentialsException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(exception.getMessage() + ": Adresse e-mail ou mot de passe incorrect", getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Response> apiException(ApiException exception) {
        log.error(String.format("ApiException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(exception.getMessage(), getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(UnrecognizedPropertyException.class)
    public ResponseEntity<Response> unrecognizedPropertyException(UnrecognizedPropertyException exception) {
        log.error(String.format("UnrecognizedPropertyException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(exception.getMessage(), getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response> accessDeniedException(AccessDeniedException exception) {
        log.error(String.format("AccessDeniedException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse("Accès refusé. Vous n'avez pas accès.", getRootCauseMessage(exception), request, FORBIDDEN), FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> exception(Exception exception) {
        log.error(String.format("Exception: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(processErrorMessage(exception), getRootCauseMessage(exception), request, INTERNAL_SERVER_ERROR), INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<Response> transactionSystemException(TransactionSystemException exception) {
        log.error(String.format("TransactionSystemException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(processErrorMessage(exception), getRootCauseMessage(exception), request, INTERNAL_SERVER_ERROR), INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Response> emptyResultDataAccessException(EmptyResultDataAccessException exception) {
        log.error(String.format("EmptyResultDataAccessException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(exception.getMessage(), getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<Response> credentialsExpiredException(CredentialsExpiredException exception) {
        log.error(String.format("CredentialsExpiredException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(exception.getMessage(), getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Response> disabledException(DisabledException exception) {
        log.error(String.format("DisabledException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse("Le compte utilisateur est actuellement désactivé.", getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Response> lockedException(LockedException exception) {
        log.error(String.format("LockedException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(exception.getMessage(), getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Response> duplicateKeyException(DuplicateKeyException exception) {
        log.error(String.format("DuplicateKeyException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(processErrorMessage(exception), getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Response> dataIntegrityViolationException(DataIntegrityViolationException exception) {
        log.error(String.format("DataIntegrityViolationException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(processErrorMessage(exception), getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Response> dataAccessException(DataAccessException exception) {
        log.error(String.format("DataAccessException: %s", exception.getMessage()));
        return new ResponseEntity<>(handleErrorResponse(processErrorMessage(exception), getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    private String processErrorMessage(Exception exception) {
        if(exception instanceof ApiException) { return exception.getMessage(); }
        //if(exception instanceof TransactionSystemException) { return getRootCauseMessage(exception).split(":")[1]; }
        if (exception.getMessage() != null) {
            if (exception.getMessage().contains("duplicate") && exception.getMessage().contains("AccountVerifications")) {
                return "Vous avez déjà vérifié votre compte.";
            }
            if (exception.getMessage().contains("duplicate") && exception.getMessage().contains("ResetPasswordVerifications")) {
                return "Nous vous avons déjà envoyé un email pour réinitialiser votre mot de passe.";
            }
            if (exception.getMessage().contains("duplicate") && exception.getMessage().contains("Key (email)")) {
                return "Cette adresse e-mail existe déjà. Veuillez utiliser une autre adresse e-mail et réessayer.";
            }
            if (exception.getMessage().contains("duplicate")) {
                return "Il a eu une duplication. Veuillez réessayer.";
            }
        }
        return "Une erreur s'est produite. Veuillez réessayer.";
    }

}