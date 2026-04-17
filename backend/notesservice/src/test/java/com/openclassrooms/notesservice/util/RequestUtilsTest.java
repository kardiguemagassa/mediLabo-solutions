package com.openclassrooms.notesservice.util;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.openclassrooms.notesservice.domain.Response;
import com.openclassrooms.notesservice.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private ByteArrayOutputStream outputStream;


    // Tests pour handleErrorResponse (méthode statique avec Exception)
    @Test
    void handleErrorResponse_withAccessDeniedException_shouldSetForbiddenStatus() throws IOException {
        // Given
        setupOutputStream();
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        when(request.getRequestURI()).thenReturn("/api/notes");

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());

        String jsonResponse = outputStream.toString();
        assertThat(jsonResponse).contains("\"statusCode\":403");
        assertThat(jsonResponse).contains("Vous n'avez pas suffisamment d'autorisations");
    }

    @Test
    void handleErrorResponse_withInvalidBearerTokenException_shouldSetUnauthorizedStatus() throws IOException {
        // Given
        setupOutputStream();
        InvalidBearerTokenException exception = new InvalidBearerTokenException("Invalid token");
        when(request.getRequestURI()).thenReturn("/api/notes");

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType("application/json");

        String jsonResponse = outputStream.toString();
        assertThat(jsonResponse).contains("\"statusCode\":401");
        assertThat(jsonResponse).contains("Vous n'êtes pas connecté");
    }

    @Test
    void handleErrorResponse_withExpiredJwtException_shouldReturnSessionExpiredMessage() throws IOException {
        // Given
        setupOutputStream();
        InvalidBearerTokenException exception = new InvalidBearerTokenException("Jwt expired at 2024-01-01");
        when(request.getRequestURI()).thenReturn("/api/notes");

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        String jsonResponse = outputStream.toString();
        assertThat(jsonResponse).contains("Votre session a expiré");
    }

    @Test
    void handleErrorResponse_withInsufficientAuthenticationException_shouldSetUnauthorizedStatus() throws IOException {
        // Given
        setupOutputStream();
        InsufficientAuthenticationException exception = new InsufficientAuthenticationException("Insufficient auth");
        when(request.getRequestURI()).thenReturn("/api/notes");

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void handleErrorResponse_withMismatchedInputException_shouldSetBadRequestStatus() throws IOException {
        // Given
        setupOutputStream();
        MismatchedInputException exception = mock(MismatchedInputException.class);
        when(exception.getMessage()).thenReturn("Invalid JSON");
        when(request.getRequestURI()).thenReturn("/api/notes");

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
    }

    @ParameterizedTest
    @MethodSource("provideAuthenticationExceptions")
    void handleErrorResponse_withAuthenticationExceptions_shouldSetBadRequestStatus(Exception exception) throws IOException {
        // Given
        setupOutputStream();
        when(request.getRequestURI()).thenReturn("/api/notes");

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
    }

    private static Stream<Arguments> provideAuthenticationExceptions() {
        return Stream.of(
                Arguments.of(new DisabledException("Account disabled")),
                Arguments.of(new LockedException("Account locked")),
                Arguments.of(new BadCredentialsException("Bad credentials")),
                Arguments.of(new CredentialsExpiredException("Credentials expired"))
        );
    }

    @Test
    void handleErrorResponse_withApiException_shouldSetBadRequestStatus() throws IOException {
        // Given
        setupOutputStream();
        ApiException exception = new ApiException("API error");
        when(request.getRequestURI()).thenReturn("/api/notes");

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());

        String jsonResponse = outputStream.toString();
        assertThat(jsonResponse).contains("API error");
    }

    @Test
    void handleErrorResponse_withGenericException_shouldSetInternalServerErrorStatus() throws IOException {
        // Given
        setupOutputStream();
        RuntimeException exception = new RuntimeException("Unexpected error");
        when(request.getRequestURI()).thenReturn("/api/notes");

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

        String jsonResponse = outputStream.toString();
        assertThat(jsonResponse).contains("Une erreur interne du serveur s'est produite");
    }

    @Test
    void handleErrorResponse_whenWriteResponseFails_shouldThrowApiException() throws IOException {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        when(request.getRequestURI()).thenReturn("/api/notes");

        // Simuler une IOException lors de l'écriture
        when(response.getOutputStream()).thenThrow(new IOException("Write failed"));

        // When & Then
        assertThatThrownBy(() -> RequestUtils.handleErrorResponse(request, response, exception))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Write failed");
    }

    @Test
    void getResponse_withEmptyData_shouldReturnResponseWithEmptyData() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/notes");
        Map<String, Object> emptyData = Map.of();
        String message = "No data found";
        HttpStatus status = HttpStatus.NO_CONTENT;

        // When
        Response result = RequestUtils.getResponse(request, emptyData, message, status);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.data()).isEmpty();
        assertThat(result.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    // Tests pour convertResponse
    @Test
    void convertResponse_shouldConvertDataSuccessfully() {
        // Given
        Map<String, Object> data = Map.of("user", Map.of(
                "id", 456,
                "username", "john.doe",
                "email", "john@example.com"
        ));

        Response response = new Response(
                LocalDateTime.now().toString(),
                200,
                "/api/users",
                HttpStatus.OK,
                "Success",
                "",
                data
        );

        // When
        Map<String, Object> result = RequestUtils.convertResponse(response, Map.class, "user");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("id")).isEqualTo(456);
        assertThat(result.get("username")).isEqualTo("john.doe");
        assertThat(result.get("email")).isEqualTo("john@example.com");
    }

    @Test
    void convertResponse_withComplexObject_shouldConvertSuccessfully() {
        // Given
        TestUser user = new TestUser(789, "jane.doe", "jane@example.com");
        Map<String, Object> data = Map.of("user", user);

        Response response = new Response(
                LocalDateTime.now().toString(),
                200,
                "/api/users",
                HttpStatus.OK,
                "Success",
                "",
                data
        );

        // When
        TestUser result = RequestUtils.convertResponse(response, TestUser.class, "user");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(789);
        assertThat(result.username()).isEqualTo("jane.doe");
        assertThat(result.email()).isEqualTo("jane@example.com");
    }

    @Test
    void convertResponse_whenKeyNotFound_shouldReturnNull() {
        // Given
        Map<String, Object> data = Map.of("otherKey", "value");
        Response response = new Response(
                LocalDateTime.now().toString(),
                200,
                "/api/test",
                HttpStatus.OK,
                "Success",
                "",
                data
        );

        // When
        Object result = RequestUtils.convertResponse(response, Object.class, "nonExistentKey");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void convertResponse_withNullData_shouldThrowNullPointerException() {
        // Given
        Response response = new Response(
                LocalDateTime.now().toString(),
                200,
                "/api/test",
                HttpStatus.OK,
                "Success",
                "",
                null
        );

        // When & Then
        assertThatThrownBy(() -> RequestUtils.convertResponse(response, Object.class, "anyKey"))
                .isInstanceOf(NullPointerException.class);
    }

    // Tests pour la méthode errorReason (testée indirectement)
    @Test
    void errorReason_for5xxServerError_shouldReturnServerErrorMessage() throws IOException {
        // Given
        setupOutputStream();
        RuntimeException exception = new RuntimeException("Database error");
        when(request.getRequestURI()).thenReturn("/api/notes");

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        String jsonResponse = outputStream.toString();
        assertThat(jsonResponse).contains("Une erreur interne du serveur s'est produite");
    }

    @Test
    void errorReason_forGenericError_shouldReturnGenericErrorMessage() throws IOException {
        // Given
        setupOutputStream();
        // Créer une exception qui ne correspond à aucune des catégories spécifiques
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
        when(request.getRequestURI()).thenReturn("/api/notes");

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        String jsonResponse = outputStream.toString();
        assertThat(jsonResponse).contains("Une erreur interne du serveur s'est produite");
    }

    // Test pour vérifier la configuration de l'ObjectMapper dans writeResponse
    @Test
    void writeResponse_shouldHandleJsonSerialization() throws IOException {
        // Given
        setupOutputStream();
        AccessDeniedException exception = new AccessDeniedException("test");
        when(request.getRequestURI()).thenReturn("/api/notes");

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        String jsonResponse = outputStream.toString();
        assertThat(jsonResponse).contains("\"statusCode\":403");
        assertThat(jsonResponse).contains("\"path\":\"/api/notes\"");
    }

    // Test pour vérifier les propriétés de l'ObjectMapper
    @Test
    void objectMapper_shouldHandleJavaTimeCorrectly() {
        // Ce test vérifie que l'ObjectMapper peut sérialiser le timestamp
        when(request.getRequestURI()).thenReturn("/api/notes");
        Response response = RequestUtils.getResponse(request, Map.of(), "Test", HttpStatus.OK);

        assertThat(response.timestamp()).isNotNull();
        // Le timestamp devrait être au format HH:mm:ss.nanoseconds
        assertThat(response.timestamp()).matches("\\d{2}:\\d{2}:\\d{2}\\.\\d+");
    }

    // Méthode utilitaire pour configurer le OutputStream
    private void setupOutputStream() throws IOException {
        outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new jakarta.servlet.ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
            }

            @Override
            public void write(int b) {
                outputStream.write(b);
            }
        });
    }

    // Classe de test pour la conversion
    private record TestUser(int id, String username, String email) {}
}