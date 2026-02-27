package com.openclassrooms.notificationservice.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.openclassrooms.notificationservice.domain.Response;
import com.openclassrooms.notificationservice.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleExceptionTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private WebRequest webRequest;

    @Mock
    private HttpHeaders headers;

    private HandleException handleException;

    @BeforeEach
    void setUp() {
        handleException = new HandleException(request);
    }

    @Test
    void handleExceptionInternal_ShouldReturnErrorResponse() {
        // Given
        Exception exception = new Exception("Test exception");
        HttpStatusCode statusCode = HttpStatusCode.valueOf(500);
        Response mockResponse = new Response("2026-02-27T10:00:00", 500, "/test",
                HttpStatus.INTERNAL_SERVER_ERROR, "Test exception", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("Test exception"), anyString(), eq(request), eq(statusCode)))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Object> response = handleException.handleExceptionInternal(
                    exception, null, headers, statusCode, webRequest);

            // Then
            assertNotNull(response);
            assertEquals(statusCode, response.getStatusCode());
            Response body = (Response) response.getBody();
            assertNotNull(body);
            assertEquals("Test exception", body.message());
            assertEquals(500, body.code());
        }
    }

    @Test
    void handleMethodArgumentNotValid_ShouldReturnFieldErrors() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", "field1", "Error message 1"));
        bindingResult.addError(new FieldError("object", "field2", "Error message 2"));

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(exception.getMessage()).thenReturn("Validation failed");
        HttpStatusCode statusCode = HttpStatusCode.valueOf(400);

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Error message 1, Error message 2", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(anyString(), anyString(), eq(request), eq(statusCode)))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Object> response = handleException.handleMethodArgumentNotValid(
                    exception, headers, statusCode, webRequest);

            // Then
            assertNotNull(response);
            assertEquals(statusCode, response.getStatusCode());
            Response body = (Response) response.getBody();
            assertNotNull(body);
            assertEquals("Error message 1, Error message 2", body.message());
        }
    }

    @Test
    void sQLIntegrityConstraintViolationException_WithDuplicateEntry_ShouldReturnBadRequest() {
        // Given
        SQLIntegrityConstraintViolationException exception =
                new SQLIntegrityConstraintViolationException("Duplicate entry 'test@test.com' for key 'email'");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "L'information existe déjà.", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("L'information existe déjà."), anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.sQLIntegrityConstraintViolationException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("L'information existe déjà.", body.message());
            assertEquals(400, body.code());
        }
    }

    @Test
    void sQLIntegrityConstraintViolationException_WithOtherError_ShouldReturnOriginalMessage() {
        // Given
        SQLIntegrityConstraintViolationException exception =
                new SQLIntegrityConstraintViolationException("Other SQL error");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Other SQL error", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("Other SQL error"), anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.sQLIntegrityConstraintViolationException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Other SQL error", body.message());
        }
    }

    @Test
    void badCredentialsException_ShouldReturnBadRequest() {
        // Given
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Bad credentials: Adresse e-mail ou mot de passe incorrect", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("Bad credentials: Adresse e-mail ou mot de passe incorrect"),
                    anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.badCredentialsException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Bad credentials: Adresse e-mail ou mot de passe incorrect", body.message());
        }
    }

    @Test
    void apiException_ShouldReturnBadRequest() {
        // Given
        ApiException exception = new ApiException("API error");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "API error", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("API error"), anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.apiException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("API error", body.message());
        }
    }

    @Test
    void unrecognizedPropertyException_ShouldReturnBadRequest() {
        // Given
        UnrecognizedPropertyException exception = mock(UnrecognizedPropertyException.class);
        when(exception.getMessage()).thenReturn("Unrecognized field");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Unrecognized field", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("Unrecognized field"), anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.unrecognizedPropertyException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Unrecognized field", body.message());
        }
    }

    @Test
    void accessDeniedException_ShouldReturnForbidden() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        Response mockResponse = new Response("2026-02-27T10:00:00", 403, "/test",
                HttpStatus.FORBIDDEN, "Accès refusé. Vous n'avez pas accès.", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("Accès refusé. Vous n'avez pas accès."),
                    anyString(), eq(request), eq(HttpStatusCode.valueOf(403))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.accessDeniedException(exception);

            // Then
            assertNotNull(response);
            assertEquals(403, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Accès refusé. Vous n'avez pas accès.", body.message());
            assertEquals(403, body.code());
        }
    }

    @Test
    void exception_ShouldReturnInternalServerError() {
        // Given
        Exception exception = new Exception("General error");

        Response mockResponse = new Response("2026-02-27T10:00:00", 500, "/test",
                HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur s'est produite. Veuillez réessayer.", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("Une erreur s'est produite. Veuillez réessayer."),
                    anyString(), eq(request), eq(HttpStatusCode.valueOf(500))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.exception(exception);

            // Then
            assertNotNull(response);
            assertEquals(500, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Une erreur s'est produite. Veuillez réessayer.", body.message());
            assertEquals(500, body.code());
        }
    }

    @Test
    void transactionSystemException_ShouldReturnInternalServerError() {
        // Given
        TransactionSystemException exception = new TransactionSystemException("Transaction error");

        Response mockResponse = new Response("2026-02-27T10:00:00", 500, "/test",
                HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur s'est produite. Veuillez réessayer.", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("Une erreur s'est produite. Veuillez réessayer."),
                    anyString(), eq(request), eq(HttpStatusCode.valueOf(500))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.transactionSystemException(exception);

            // Then
            assertNotNull(response);
            assertEquals(500, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Une erreur s'est produite. Veuillez réessayer.", body.message());
        }
    }

    @Test
    void emptyResultDataAccessException_ShouldReturnBadRequest() {
        // Given
        EmptyResultDataAccessException exception = new EmptyResultDataAccessException(1);

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Empty result", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(anyString(), anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.emptyResultDataAccessException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
        }
    }

    @Test
    void credentialsExpiredException_ShouldReturnBadRequest() {
        // Given
        CredentialsExpiredException exception = new CredentialsExpiredException("Credentials expired");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Credentials expired", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(anyString(), anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.credentialsExpiredException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
        }
    }

    @Test
    void disabledException_ShouldReturnBadRequest() {
        // Given
        DisabledException exception = new DisabledException("Account disabled");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Le compte utilisateur est actuellement désactivé", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("Le compte utilisateur est actuellement désactivé"),
                    anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.disabledException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Le compte utilisateur est actuellement désactivé", body.message());
        }
    }

    @Test
    void lockedException_ShouldReturnBadRequest() {
        // Given
        LockedException exception = new LockedException("Account locked");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Account locked", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(anyString(), anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.lockedException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
        }
    }

    @Test
    void duplicateKeyException_WithEmailDuplication_ShouldReturnSpecificMessage() {
        // Given
        DuplicateKeyException exception = new DuplicateKeyException("duplicate key value violates unique constraint - Key (email)");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Cette adresse e-mail existe déjà. Veuillez utiliser une autre adresse e-mail et réessayer.",
                "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(
                    eq("Cette adresse e-mail existe déjà. Veuillez utiliser une autre adresse e-mail et réessayer."),
                    anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.duplicateKeyException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Cette adresse e-mail existe déjà. Veuillez utiliser une autre adresse e-mail et réessayer.",
                    body.message());
        }
    }

    @Test
    void duplicateKeyException_WithAccountVerification_ShouldReturnSpecificMessage() {
        // Given
        DuplicateKeyException exception = new DuplicateKeyException("duplicate key value - AccountVerifications");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Vous avez déjà vérifié votre compte.", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("Vous avez déjà vérifié votre compte."),
                    anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.duplicateKeyException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Vous avez déjà vérifié votre compte.", body.message());
        }
    }

    @Test
    void duplicateKeyException_WithResetPasswordVerification_ShouldReturnSpecificMessage() {
        // Given
        DuplicateKeyException exception = new DuplicateKeyException("duplicate key value - ResetPasswordVerifications");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Nous vous avons déjà envoyé un email pour réinitialiser votre mot de passe.",
                "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(
                    eq("Nous vous avons déjà envoyé un email pour réinitialiser votre mot de passe."),
                    anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.duplicateKeyException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Nous vous avons déjà envoyé un email pour réinitialiser votre mot de passe.",
                    body.message());
        }
    }

    @Test
    void duplicateKeyException_WithGenericDuplicate_ShouldReturnGenericMessage() {
        // Given
        DuplicateKeyException exception = new DuplicateKeyException("duplicate key value");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Il a eu une duplication. Veuillez réessayer.", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("Il a eu une duplication. Veuillez réessayer."),
                    anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.duplicateKeyException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Il a eu une duplication. Veuillez réessayer.", body.message());
        }
    }

    @Test
    void dataIntegrityViolationException_ShouldProcessErrorMessage() {
        // Given
        DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate key value - Key (email)");

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Cette adresse e-mail existe déjà. Veuillez utiliser une autre adresse e-mail et réessayer.",
                "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(
                    eq("Cette adresse e-mail existe déjà. Veuillez utiliser une autre adresse e-mail et réessayer."),
                    anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.dataIntegrityViolationException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Cette adresse e-mail existe déjà. Veuillez utiliser une autre adresse e-mail et réessayer.",
                    body.message());
        }
    }

    @Test
    void dataAccessException_ShouldProcessErrorMessage() {
        // Given
        DataAccessException exception = new DataAccessException("duplicate key value") {};

        Response mockResponse = new Response("2026-02-27T10:00:00", 400, "/test",
                HttpStatus.BAD_REQUEST, "Il a eu une duplication. Veuillez réessayer.", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("Il a eu une duplication. Veuillez réessayer."),
                    anyString(), eq(request), eq(HttpStatusCode.valueOf(400))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.dataAccessException(exception);

            // Then
            assertNotNull(response);
            assertEquals(400, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Il a eu une duplication. Veuillez réessayer.", body.message());
        }
    }

    @Test
    void processErrorMessage_WithNullMessage_ShouldReturnDefaultMessage() {
        // Given
        Exception exception = new Exception();

        Response mockResponse = new Response("2026-02-27T10:00:00", 500, "/test",
                HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur s'est produite. Veuillez réessayer.", "root cause", null);

        try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
            when(RequestUtils.handleErrorResponse(eq("Une erreur s'est produite. Veuillez réessayer."),
                    anyString(), eq(request), eq(HttpStatusCode.valueOf(500))))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<Response> response = handleException.exception(exception);

            // Then
            assertNotNull(response);
            assertEquals(500, response.getStatusCode().value());
            Response body = response.getBody();
            assertNotNull(body);
            assertEquals("Une erreur s'est produite. Veuillez réessayer.", body.message());
        }
    }
}