package com.openclassrooms.authorizationserverservice.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.openclassrooms.authorizationserverservice.domain.Response;
import com.openclassrooms.authorizationserverservice.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleExceptionTest {

    @InjectMocks
    private HandleException handleException;

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("Exception : handleMethodArgumentNotValid devrait concaténer les erreurs de validation")
    void handleMethodArgumentNotValid_ShouldReturnFormattedErrors() {
        // 1. GIVEN : On prépare les erreurs de validation
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);

        FieldError error1 = new FieldError("user", "email", "L'email est requis");
        FieldError error2 = new FieldError("user", "password", "Le mot de passe est trop court");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        Response mockResponse = new Response(null, 400, null, HttpStatus.BAD_REQUEST, "L'email est requis, Le mot de passe est trop court", null, null);

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            // 2. WHEN : On appelle la méthode manuellement
            ResponseEntity<Object> response = handleException.handleMethodArgumentNotValid(
                    ex, new org.springframework.http.HttpHeaders(), HttpStatus.BAD_REQUEST, mock(org.springframework.web.context.request.WebRequest.class));

            // 3. THEN : On vérifie que les messages d'erreurs ont été joints par une virgule
            Assertions.assertNotNull(response);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("L'email est requis, Le mot de passe est trop court"),
                    any(), eq(request), eq(HttpStatus.BAD_REQUEST)
            ));
        }
    }

    @Test
    @DisplayName("Exception : handleExceptionInternal devrait déléguer correctement")
    void handleExceptionInternal_ShouldReturnResponseEntity() {
        // 1. GIVEN
        Exception ex = new Exception("Internal error");
        Response mockResponse = new Response(null, 500, null, HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", null, null);

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            // 2. WHEN
            ResponseEntity<Object> response = handleException.handleExceptionInternal(
                    ex, null, new org.springframework.http.HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, mock(org.springframework.web.context.request.WebRequest.class));

            // 3. THEN
            Assertions.assertNotNull(response);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Internal error"), any(), eq(request), eq(HttpStatus.INTERNAL_SERVER_ERROR)
            ));
        }
    }

    @Test
    @DisplayName("Exception : apiException devrait renvoyer 400 avec le message original")
    void apiException_ShouldReturnBadRequest() {
        ApiException ex = new ApiException("Erreur API personnalisée");
        Response mockResponse = createMockResponse();

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            ResponseEntity<Response> response = handleException.apiException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Erreur API personnalisée"), any(), eq(request), eq(HttpStatus.BAD_REQUEST)
            ));
        }
    }

    @Test
    @DisplayName("Exception : unrecognizedPropertyException devrait renvoyer 400")
    void unrecognizedPropertyException_ShouldReturnBadRequest() {
        // UnrecognizedPropertyException est une exception de Jackson
        UnrecognizedPropertyException ex = mock(UnrecognizedPropertyException.class);
        when(ex.getMessage()).thenReturn("Propriété inconnue");
        Response mockResponse = createMockResponse();

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            ResponseEntity<Response> response = handleException.unrecognizedPropertyException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Propriété inconnue"), any(), eq(request), eq(HttpStatus.BAD_REQUEST)
            ));
        }
    }

    @Test
    @DisplayName("Exception : accessDeniedException devrait renvoyer 403 avec message fixe")
    void accessDeniedException_ShouldReturnForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        Response mockResponse = createMockResponse();

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            ResponseEntity<Response> response = handleException.accessDeniedException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            // Ici on vérifie le message spécifique "Accès refusé. Vous n'avez pas accès."
            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Accès refusé. Vous n'avez pas accès."), any(), eq(request), eq(HttpStatus.FORBIDDEN)
            ));
        }
    }

    @Test
    @DisplayName("Exception : BadCredentials devrait renvoyer 400 avec message personnalisé")
    void badCredentialsException_ShouldReturnBadRequest() {
        BadCredentialsException ex = new BadCredentialsException("Invalid");

        // On crée une instance de Response valide avec le constructeur canonique du Record (7 paramètres)
        Response mockResponse = new Response(java.time.LocalDateTime.now().toString(), 400, "/test", HttpStatus.BAD_REQUEST, "Message", "Exception", java.util.Map.of());

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            // On mocke la signature à 4 arguments utilisée dans HandleException
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any())).thenReturn(mockResponse);

            ResponseEntity<Response> response = handleException.badCredentialsException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();

            // Vérification de l'appel à RequestUtils
            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Invalid: Adresse e-mail ou mot de passe incorrect"),
                    any(),
                    eq(request),
                    eq(HttpStatus.BAD_REQUEST)
            ));
        }
    }

    @Test
    @DisplayName("SQL : SQLIntegrity devrait détecter les entrées dupliquées")
    void sQLIntegrityConstraintViolationException_ShouldHandleDuplicateEntry() {
        SQLIntegrityConstraintViolationException ex = new SQLIntegrityConstraintViolationException("Duplicate entry 'test'");

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            handleException.sQLIntegrityConstraintViolationException(ex);

            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("L'information existe déjà."), any(), eq(request), eq(HttpStatus.BAD_REQUEST)
            ));
        }
    }

    @Test
    @DisplayName("Logic : processErrorMessage devrait gérer les doublons d'email")
    void processErrorMessage_ShouldHandleEmailDuplicate() {
        // On utilise l'exception générique pour déclencher processErrorMessage
        Exception ex = new Exception("duplicate Key (email)");

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            handleException.exception(ex);

            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Cette adresse e-mail existe déjà. Veuillez utiliser une autre adresse e-mail et réessayer."),
                    any(), any(), any())
            );
        }
    }

    @Test
    @DisplayName("Logic : processErrorMessage devrait gérer AccountVerifications")
    void processErrorMessage_ShouldHandleAccountVerifications() {
        Exception ex = new Exception("duplicate AccountVerifications");

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            handleException.exception(ex);

            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Vous avez déjà vérifié votre compte."), any(), any(), any())
            );
        }
    }

    @Test
    @DisplayName("Logic : processErrorMessage devrait renvoyer message par défaut si inconnu")
    void processErrorMessage_ShouldReturnDefaultMessage() {
        Exception ex = new Exception("Something went wrong");

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            handleException.exception(ex);

            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Une erreur s'est produite. Veuillez réessayer."), any(), any(), any())
            );
        }
    }

    @Test
    @DisplayName("Transaction : Devrait renvoyer 500 avec message traité")
    void transactionSystemException_ShouldReturnInternalError() {
        TransactionSystemException ex = new TransactionSystemException("Transaction failed");
        Response mockResponse = createMockResponse();

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            ResponseEntity<Response> response = handleException.transactionSystemException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            // Vérifie que processErrorMessage a renvoyé le message par défaut
            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Une erreur s'est produite. Veuillez réessayer."), any(), eq(request), eq(HttpStatus.INTERNAL_SERVER_ERROR)
            ));
        }
    }

    @Test
    @DisplayName("Data : EmptyResultDataAccessException devrait renvoyer 400")
    void emptyResultDataAccessException_ShouldReturnBadRequest() {
        EmptyResultDataAccessException ex = new EmptyResultDataAccessException("No entity found", 1);
        Response mockResponse = createMockResponse();

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            ResponseEntity<Response> response = handleException.emptyResultDataAccessException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("No entity found"), any(), eq(request), eq(HttpStatus.BAD_REQUEST)
            ));
        }
    }

    @Test
    @DisplayName("Security : DisabledException devrait renvoyer message fixe en français")
    void disabledException_ShouldReturnCustomMessage() {
        DisabledException ex = new DisabledException("Disabled");
        Response mockResponse = createMockResponse();

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            handleException.disabledException(ex);

            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Le compte utilisateur est actuellement désactivé."), any(), any(), any()
            ));
        }
    }

    @Test
    @DisplayName("Persistence : DataIntegrityViolation avec email dupliqué")
    void dataIntegrityViolationException_ShouldHandleEmailDuplicate() {
        // Simulation du message contenant "duplicate" et "Key (email)" pour tester processErrorMessage
        DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicate Key (email) error");
        Response mockResponse = createMockResponse();

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            handleException.dataIntegrityViolationException(ex);

            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Cette adresse e-mail existe déjà. Veuillez utiliser une autre adresse e-mail et réessayer."),
                    any(), eq(request), eq(HttpStatus.BAD_REQUEST)
            ));
        }
    }

    @Test
    @DisplayName("Persistence : DuplicateKeyException avec ResetPasswordVerifications")
    void duplicateKeyException_ShouldHandleResetPassword() {
        DuplicateKeyException ex = new DuplicateKeyException("duplicate ResetPasswordVerifications error");
        Response mockResponse = createMockResponse();

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            handleException.duplicateKeyException(ex);

            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Nous vous avons déjà envoyé un email pour réinitialiser votre mot de passe."),
                    any(), any(), any()
            ));
        }
    }

    @Test
    @DisplayName("Security : CredentialsExpiredException devrait renvoyer 400")
    void credentialsExpiredException_ShouldReturnBadRequest() {
        CredentialsExpiredException ex = new CredentialsExpiredException("Le mot de passe a expiré");
        Response mockResponse = createMockResponse();

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            ResponseEntity<Response> response = handleException.credentialsExpiredException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Le mot de passe a expiré"), any(), eq(request), eq(HttpStatus.BAD_REQUEST)
            ));
        }
    }

    @Test
    @DisplayName("Security : LockedException devrait renvoyer 400")
    void lockedException_ShouldReturnBadRequest() {
        LockedException ex = new LockedException("Compte verrouillé");
        Response mockResponse = createMockResponse();

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            ResponseEntity<Response> response = handleException.lockedException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Compte verrouillé"), any(), eq(request), eq(HttpStatus.BAD_REQUEST)
            ));
        }
    }

    @Test
    @DisplayName("Persistence : DataAccessException avec duplication générique")
    void dataAccessException_ShouldHandleGenericDuplicate() {
        // On crée une exception anonyme car DataAccessException est abstraite
        DataAccessException ex = new DataAccessException("Some generic duplicate error") {};
        Response mockResponse = createMockResponse();

        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {
            mockedRequestUtils.when(() -> RequestUtils.handleErrorResponse(any(), any(), any(), any()))
                    .thenReturn(mockResponse);

            ResponseEntity<Response> response = handleException.dataAccessException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            // Vérifie que processErrorMessage a renvoyé le message de duplication générique
            mockedRequestUtils.verify(() -> RequestUtils.handleErrorResponse(
                    eq("Il a eu une duplication. Veuillez réessayer."), any(), eq(request), eq(HttpStatus.BAD_REQUEST)
            ));
        }
    }

    private Response createMockResponse() {
        return new Response(null, 0, null, null, null, null, null);
    }
}