package com.openclassrooms.assessmentservice.exception;

import com.openclassrooms.assessmentservice.domain.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import static com.openclassrooms.assessmentservice.util.RequestUtils.handleErrorResponse;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Gestionnaire global des exceptions.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class HandleException extends ResponseEntityExceptionHandler implements ErrorController {

    private final HttpServletRequest request;

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, @NonNull HttpHeaders headers, @NonNull HttpStatusCode statusCode, @NonNull WebRequest webRequest) {
        log.error("handleExceptionInternal: {}", exception.getMessage());
        return new ResponseEntity<>(handleErrorResponse(exception.getMessage(), getRootCauseMessage(exception), request, statusCode), statusCode);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Response> apiException(ApiException exception) {
        log.error("ApiException: {}", exception.getMessage());
        return new ResponseEntity<>(handleErrorResponse(exception.getMessage(), getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Response> duplicateKeyException(DuplicateKeyException exception) {
        log.error("DuplicateKeyException: {}", exception.getMessage());
        return new ResponseEntity<>(handleErrorResponse(processErrorMessage(exception), getRootCauseMessage(exception), request, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response> accessDeniedException(AccessDeniedException exception) {
        log.error("AccessDeniedException: {}", exception.getMessage());
        return new ResponseEntity<>(handleErrorResponse("Accès refusé. Vous n'avez pas accès.", getRootCauseMessage(exception), request, FORBIDDEN), FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> exception(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
        return new ResponseEntity<>(handleErrorResponse(processErrorMessage(exception), getRootCauseMessage(exception), request, INTERNAL_SERVER_ERROR), INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Response> handleServiceUnavailable(ServiceUnavailableException exception) {
        log.error("Service unavailable: {}", exception.getMessage());
        return new ResponseEntity<>(handleErrorResponse("Service temporairement indisponible. Veuillez réessayer plus tard.", getRootCauseMessage(exception), request, SERVICE_UNAVAILABLE), SERVICE_UNAVAILABLE);
    }

    private String processErrorMessage(Exception exception) {
        if(exception instanceof ApiException) { return exception.getMessage(); }
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