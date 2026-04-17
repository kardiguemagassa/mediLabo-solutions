package com.openclassrooms.patientservice.util;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.openclassrooms.patientservice.domain.Response;
import com.openclassrooms.patientservice.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour la classe {@link RequestUtils}.
 */
@ExtendWith(MockitoExtension.class)
class RequestUtilsTest {

    @Mock
    private HttpServletRequest mockRequest;

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        when(mockRequest.getRequestURI()).thenReturn("/api/v1/patients");
    }

    // Helper methods

    private HttpServletResponse createMockServletResponse() throws IOException {
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        var servletOutputStream = new jakarta.servlet.ServletOutputStream() {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(jakarta.servlet.WriteListener writeListener) {}
            @Override public void write(int b) { outputStream.write(b); }
        };
        when(mockResponse.getOutputStream()).thenReturn(servletOutputStream);
        return mockResponse;
    }

    // Tests pour la gestion des erreurs

    @Test
    void handleErrorResponse_shouldReturnForbidden_forAccessDeniedException() throws IOException {
        // Given
        Exception exception = new AccessDeniedException("Access denied");
        HttpServletResponse mockResponse = createMockServletResponse();

        // When
        RequestUtils.handleErrorResponse(mockRequest, mockResponse, exception);

        // Then
        verify(mockResponse).setStatus(HttpStatus.FORBIDDEN.value());
        verify(mockResponse).setContentType("application/json");

        String jsonResponse = outputStream.toString();
        assertThat(jsonResponse).contains("\"code\":403");
    }

    @Test
    void handleErrorResponse_shouldReturnUnauthorized_forInvalidBearerTokenException() throws IOException {
        // Given
        Exception exception = new InvalidBearerTokenException("Invalid token");
        HttpServletResponse mockResponse = createMockServletResponse();

        // When
        RequestUtils.handleErrorResponse(mockRequest, mockResponse, exception);

        // Then
        verify(mockResponse).setStatus(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void handleErrorResponse_shouldReturnBadRequest_forAuthenticationExceptions() throws IOException {
        // Given
        Exception exception = new DisabledException("Account disabled");
        HttpServletResponse mockResponse = createMockServletResponse();

        // When
        RequestUtils.handleErrorResponse(mockRequest, mockResponse, exception);

        // Then
        verify(mockResponse).setStatus(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void handleErrorResponse_shouldReturnBadRequest_forMismatchedInputException() throws IOException {
        // Given
        Exception exception = mock(MismatchedInputException.class);
        HttpServletResponse mockResponse = createMockServletResponse();

        // When
        RequestUtils.handleErrorResponse(mockRequest, mockResponse, exception);

        // Then
        verify(mockResponse).setStatus(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void handleErrorResponse_shouldReturnBadRequest_forApiException() throws IOException {
        // Given
        Exception exception = new ApiException("API error");
        HttpServletResponse mockResponse = createMockServletResponse();

        // When
        RequestUtils.handleErrorResponse(mockRequest, mockResponse, exception);

        // Then
        verify(mockResponse).setStatus(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void handleErrorResponse_shouldReturnInternalServerError_forGenericException() throws IOException {
        // Given
        Exception exception = new RuntimeException("Generic error");
        HttpServletResponse mockResponse = createMockServletResponse();

        // When
        RequestUtils.handleErrorResponse(mockRequest, mockResponse, exception);

        // Then
        verify(mockResponse).setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    // Tests pour getResponse

    @Test
    void getResponse_shouldCreateValidResponse() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("id", 123);
        data.put("name", "John Doe");
        String message = "Patient retrieved successfully";
        HttpStatus status = HttpStatus.OK;

        // When
        Response response = RequestUtils.getResponse(mockRequest, data, message, status);

        // Then
        // Note: Le record Response a 'code' (int) et 'status' (HttpStatus) séparés
        assertThat(response.code()).isEqualTo(200);  // 'code' est un int
        assertThat(response.status()).isEqualTo(HttpStatus.OK);  // 'status' est un HttpStatus
        assertThat(response.message()).isEqualTo(message);
        assertThat(response.path()).isEqualTo("/api/v1/patients");

        // Pour Map<?, ?>
        Map<?, ?> responseData = response.data();

        assertThat(responseData.get("id")).isEqualTo(123);
        assertThat(responseData.get("name")).isEqualTo("John Doe");
    }


    @Test
    void getResponse_shouldHandleEmptyData() {
        // Given
        Map<String, Object> emptyData = new HashMap<>();
        String message = "No data";
        HttpStatus status = HttpStatus.NO_CONTENT;

        // When
        Response response = RequestUtils.getResponse(mockRequest, emptyData, message, status);

        // Then
        assertThat(response.code()).isEqualTo(204);
        assertThat(response.status()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.data()).isEmpty();
    }

    @Test
    void handleErrorResponse_shouldHandleIOException() throws IOException {
        // Given
        Exception exception = new RuntimeException("Test error");
        HttpServletResponse brokenResponse = mock(HttpServletResponse.class);

        // Simuler une IOException
        when(brokenResponse.getOutputStream()).thenThrow(new IOException("Stream closed"));

        // When & Then
        assertThatThrownBy(() ->
                RequestUtils.handleErrorResponse(mockRequest, brokenResponse, exception)
        ).isInstanceOf(ApiException.class)
                .hasMessageContaining("Stream closed");
    }

    // Test pour vérifier le format JSON

    @Test
    void handleErrorResponse_shouldReturnValidJson() throws IOException {
        // Given
        Exception exception = new RuntimeException("Test error");
        HttpServletResponse mockResponse = createMockServletResponse();
        outputStream.reset();

        // When
        RequestUtils.handleErrorResponse(mockRequest, mockResponse, exception);

        // Then
        String jsonResponse = outputStream.toString();

        // Vérifie que c'est du JSON valide avec les champs attendus
        assertThat(jsonResponse)
                .contains("\"time\"")
                .contains("\"code\"")
                .contains("\"path\"")
                .contains("\"status\"")
                .contains("\"message\"")
                .contains("\"exception\"");

        // Vérifie le code d'erreur
        assertThat(jsonResponse).contains("\"code\":500");
    }

    // Tests supplémentaires

    @Test
    void getResponse_shouldIncludeTimeInResponse() {
        // Given
        Map<String, Object> data = new HashMap<>();
        String message = "Test";
        HttpStatus status = HttpStatus.OK;

        // When
        Response response = RequestUtils.getResponse(mockRequest, data, message, status);

        // Then
        assertThat(response.time()).isNotNull();
        assertThat(response.time()).isNotEmpty();
    }

    @Test
    void getResponse_shouldHandleSuccessWithoutException() {
        // Given
        Map<String, Object> data = new HashMap<>();
        String message = "Success";
        HttpStatus status = HttpStatus.OK;

        // When
        Response response = RequestUtils.getResponse(mockRequest, data, message, status);

        // Then
        assertThat(response.exception()).isEmpty();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.status()).isEqualTo(HttpStatus.OK);
    }

}