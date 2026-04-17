package com.openclassrooms.authorizationserverservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.List;
import java.util.stream.Collectors;
//import static com.openclassrooms.authorizationserverservice.util.RequestUtils.handleErrorResponse;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;


@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class HandleException extends ResponseEntityExceptionHandler implements ErrorController {
    private final HttpServletRequest request;

//    @Override
//    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, @NonNull HttpHeaders headers, @NonNull HttpStatusCode statusCode, @NonNull WebRequest webRequest) {
//        log.error(String.format("handleExceptionInternal: %s", exception.getMessage()));
//        return new ResponseEntity<>(handleErrorResponse(exception.getMessage(), getRootCauseMessage(exception), request, statusCode), statusCode);
//    }
//
//    @Override
//    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, @NonNull HttpHeaders headers, @NonNull HttpStatusCode statusCode, @NonNull WebRequest webRequest) {
//        log.error(String.format("MethodArgumentNotValidException: %s", exception.getMessage()));
//        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
//        String fieldsMessage = fieldErrors.stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));
//        return new ResponseEntity<>(handleErrorResponse(fieldsMessage, getRootCauseMessage(exception), request, statusCode), statusCode);
//    }
//
//    private String processErrorMessage(Exception exception) {
//        if(exception instanceof ApiException) { return exception.getMessage(); }
//        //if(exception instanceof TransactionSystemException) { return getRootCauseMessage(exception).split(":")[1]; }
//        if (exception.getMessage() != null) {
//            if (exception.getMessage().contains("duplicate") && exception.getMessage().contains("AccountVerifications")) {
//                return "Vous avez déjà vérifié votre compte.";
//            }
//            if (exception.getMessage().contains("duplicate") && exception.getMessage().contains("ResetPasswordVerifications")) {
//                return "Nous vous avons déjà envoyé un email pour réinitialiser votre mot de passe.";
//            }
//            if (exception.getMessage().contains("duplicate") && exception.getMessage().contains("Key (email)")) {
//                return "Cette adresse e-mail existe déjà. Veuillez utiliser une autre adresse e-mail et réessayer.";
//            }
//            if (exception.getMessage().contains("duplicate")) {
//                return "Il a eu une duplication. Veuillez réessayer.";
//            }
//        }
//        return "Une erreur s'est produite. Veuillez réessayer.";
//    }

}