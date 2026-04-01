package com.openclassrooms.patientservice.exception;

import com.openclassrooms.patientservice.domain.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Gestionnaire global des exceptions.
 * Centralise la gestion des erreurs pour des réponses cohérentes.
 *
 * @author  Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
@Slf4j
@RestControllerAdvice
public class HandleException {

    /**Gère les exceptions métier (ApiException)*/
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Response> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("API Exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createResponse(request, HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    /** Gère les erreurs de validation (@Valid) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });

        log.warn("Validation errors: {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(
                        LocalTime.now().toString(),
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI(),
                        HttpStatus.BAD_REQUEST,
                        "Erreur de validation",
                        EMPTY,
                        Map.of("errors", errors)
                ));
    }

    /**Gère les erreurs d'accès refusé */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {} for path {}", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createResponse(request, HttpStatus.FORBIDDEN, "Accès refusé"));
    }

    /** ère les exceptions non prévues*/
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue s'est produite"));
    }

    /** Crée une réponse d'erreur standardisée*/
    private Response createResponse(HttpServletRequest request, HttpStatus status, String message) {
        return new Response(LocalTime.now().toString(), status.value(), request.getRequestURI(), status, message, EMPTY, Map.of());
    }
}