package com.openclassrooms.notesservice.exception;

import com.openclassrooms.notesservice.domain.Response;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleExceptionTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private WebRequest webRequest;

    @InjectMocks
    private HandleException handleException;

    @BeforeEach
    void setUp() {
        // Configuration commune
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    // Tests pour handleExceptionInternal
    @Test
    void handleExceptionInternal_shouldReturnResponseEntityWithErrorResponse() {
        // Given
        Exception exception = new RuntimeException("Test exception");
        HttpHeaders headers = new HttpHeaders();
        HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;

        // When
        ResponseEntity<Object> responseEntity = handleException.handleExceptionInternal(
                exception, null, headers, statusCode, webRequest
        );

        // Then
        Assertions.assertNotNull(responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).isInstanceOf(Response.class);

        Response response = (Response) responseEntity.getBody();
        assertThat(response.path()).isEqualTo("/api/test");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    // Tests pour apiException
    @Test
    void apiException_shouldReturnBadRequest() {
        // Given
        ApiException exception = new ApiException("API error occurred");

        // When
        ResponseEntity<Response> response = handleException.apiException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusCode()).isEqualTo(400);
    }

    // Tests pour emptyResultDataAccessException
    @Test
    void emptyResultDataAccessException_shouldReturnBadRequest() {
        // Given
        EmptyResultDataAccessException exception = new EmptyResultDataAccessException(1);

        // When
        ResponseEntity<Response> response = handleException.emptyResultDataAccessException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusCode()).isEqualTo(400);
    }

    // Tests pour duplicateKeyException
    @Test
    void duplicateKeyException_shouldReturnBadRequest() {
        // Given
        DuplicateKeyException exception = new DuplicateKeyException("Duplicate key violation");

        // When
        ResponseEntity<Response> response = handleException.duplicateKeyException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusCode()).isEqualTo(400);
    }

    // Tests pour dataIntegrityViolationException
    @Test
    void dataIntegrityViolationException_shouldReturnBadRequest() {
        // Given
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Data integrity violation");

        // When
        ResponseEntity<Response> response = handleException.dataIntegrityViolationException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusCode()).isEqualTo(400);
    }

    // Tests pour accessDeniedException
    @Test
    void accessDeniedException_shouldReturnForbidden() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        // When
        ResponseEntity<Response> response = handleException.accessDeniedException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusCode()).isEqualTo(403);
        assertThat(response.getBody().message()).contains("Accès refusé");
    }

    // Tests pour exception générique
    @Test
    void exception_shouldReturnInternalServerError() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<Response> response = handleException.exception(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusCode()).isEqualTo(500);
    }

    // Tests paramétrés pour processErrorMessage (testés indirectement via les handlers)
    @ParameterizedTest
    @MethodSource("provideExceptionTestCases")
    void exceptionHandlers_shouldProcessMessagesCorrectly(Exception exception, String expectedMessagePart) {
        // When
        ResponseEntity<Response> response = handleException.exception(exception);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains(expectedMessagePart);
    }

    private static Stream<Arguments> provideExceptionTestCases() {
        return Stream.of(
                // Cas AccountVerifications
                Arguments.of(
                        new RuntimeException("duplicate key error collection: test.AccountVerifications"),
                        "Vous avez déjà vérifié votre compte."
                ),
                // Cas ResetPasswordVerifications
                Arguments.of(
                        new RuntimeException("duplicate key error collection: test.ResetPasswordVerifications"),
                        "Nous vous avons déjà envoyé un email pour réinitialiser votre mot de passe."
                ),
                // Cas email duplicate
                Arguments.of(
                        new RuntimeException("duplicate key value violates unique constraint \"Key (email)\""),
                        "Cette adresse e-mail existe déjà."
                ),
                // Cas duplicate générique
                Arguments.of(
                        new RuntimeException("duplicate key value violates unique constraint"),
                        "Il a eu une duplication"
                ),
                // Cas générique sans message spécifique
                Arguments.of(
                        new RuntimeException("Some other error"),
                        "Une erreur s'est produite"
                )
        );
    }

    // Test spécifique pour ApiException
    @Test
    void apiExceptionHandler_shouldUseExceptionMessage() {
        // Given
        ApiException exception = new ApiException("Specific API error");

        // When
        ResponseEntity<Response> response = handleException.apiException(exception);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Specific API error");
    }

    // Test pour vérifier que les handlers retournent le bon status HTTP
    @Test
    void exceptionHandlers_shouldReturnCorrectHttpStatus() {
        // Test ApiException → BAD_REQUEST
        ResponseEntity<Response> apiResponse = handleException.apiException(new ApiException("test"));
        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test AccessDeniedException → FORBIDDEN
        ResponseEntity<Response> accessResponse = handleException.accessDeniedException(
                new AccessDeniedException("test")
        );
        assertThat(accessResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Test Exception générique → INTERNAL_SERVER_ERROR
        ResponseEntity<Response> genericResponse = handleException.exception(new RuntimeException("test"));
        assertThat(genericResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Test pour EmptyResultDataAccessException avec message spécifique
    @Test
    void emptyResultDataAccessException_shouldIncludeExceptionMessage() {
        // Given
        EmptyResultDataAccessException exception = new EmptyResultDataAccessException("No record found", 1);

        // When
        ResponseEntity<Response> response = handleException.emptyResultDataAccessException(exception);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("No record found");
    }

    // Test pour vérifier que les méthodes délèguent correctement à RequestUtils
    @Test
    void duplicateKeyException_shouldProcessErrorMessage() {
        // Given
        DuplicateKeyException exception = new DuplicateKeyException("duplicate key error collection: test.AccountVerifications");

        // When
        ResponseEntity<Response> response = handleException.duplicateKeyException(exception);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Vous avez déjà vérifié votre compte.");
    }

    // Test pour DataIntegrityViolationException avec message de duplication
    @Test
    void dataIntegrityViolationException_withDuplicateMessage_shouldProcessMessage() {
        // Given
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"Key (email)\""
        );

        // When
        ResponseEntity<Response> response = handleException.dataIntegrityViolationException(exception);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Cette adresse e-mail existe déjà");
    }
}