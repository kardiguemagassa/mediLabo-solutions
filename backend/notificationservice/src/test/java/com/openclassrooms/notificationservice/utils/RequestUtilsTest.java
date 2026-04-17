package com.openclassrooms.notificationservice.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.openclassrooms.notificationservice.domain.Response;
import com.openclassrooms.notificationservice.exception.ApiException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ServletOutputStream servletOutputStream;

    private ByteArrayOutputStream baos;
    private TestServletOutputStream testServletOutputStream;

    @BeforeEach
    void setUp() {
        baos = new ByteArrayOutputStream();
        testServletOutputStream = new TestServletOutputStream(baos);
    }

    @Test
    void handleErrorResponse_WithStringParams_ShouldReturnResponseWithCorrectFields() {
        // Given
        String message = "Error message";
        String exception = "Root cause";
        HttpStatusCode status = HttpStatus.BAD_REQUEST;
        String requestURI = "/api/test";

        when(request.getRequestURI()).thenReturn(requestURI);

        // When
        Response result = RequestUtils.handleErrorResponse(message, exception, request, status);

        // Then
        assertNotNull(result);
        assertEquals(status.value(), result.code());
        assertEquals(requestURI, result.path());
        assertEquals(HttpStatus.valueOf(status.value()), result.status());
        assertEquals(message, result.message());
        assertEquals(exception, result.exception());
        assertNotNull(result.time());
        assertTrue(result.data().isEmpty());
    }

    @Test
    void getResponse_ShouldReturnResponseWithData() {
        // Given
        String message = "Success message";
        HttpStatus status = HttpStatus.OK;
        String requestURI = "/api/test";
        Map<String, String> data = new HashMap<>();
        data.put("key", "value");

        when(request.getRequestURI()).thenReturn(requestURI);

        // When
        Response result = RequestUtils.getResponse(request, data, message, status);

        // Then
        assertNotNull(result);
        assertEquals(status.value(), result.code());
        assertEquals(requestURI, result.path());
        assertEquals(status, result.status());
        assertEquals(message, result.message());
        assertEquals("", result.exception());
        assertNotNull(result.time());
        assertEquals(data, result.data());
    }

    //  TESTS POUR handleErrorResponse(HttpServletRequest, HttpServletResponse, Exception)

    @Test
    void handleErrorResponse_WithRequestAndResponse_ShouldHandleAccessDeniedException() throws Exception {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getOutputStream()).thenReturn(testServletOutputStream);

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        verify(response).setContentType("application/json");

        String jsonResponse = baos.toString();
        assertTrue(jsonResponse.contains("403"));
        assertTrue(jsonResponse.contains("FORBIDDEN"));
        assertTrue(jsonResponse.contains("Vous n'avez pas suffisamment d'autorisations"));
    }

    @Test
    void handleErrorResponse_WithRequestAndResponse_ShouldHandleInvalidBearerTokenException() throws Exception {
        // Given
        InvalidBearerTokenException exception = new InvalidBearerTokenException("Invalid token");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getOutputStream()).thenReturn(testServletOutputStream);

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType("application/json");

        String jsonResponse = baos.toString();
        assertTrue(jsonResponse.contains("401"));
    }

    @Test
    void handleErrorResponse_WithRequestAndResponse_ShouldHandleInvalidBearerTokenException_WithExpiredJwt() throws Exception {
        // Given
        InvalidBearerTokenException exception = new InvalidBearerTokenException("Jwt expired at 2026-01-01");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getOutputStream()).thenReturn(testServletOutputStream);

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType("application/json");

        String jsonResponse = baos.toString();
        assertTrue(jsonResponse.contains("Votre session a expiré"));
    }

    @Test
    void handleErrorResponse_WithRequestAndResponse_ShouldHandleInsufficientAuthenticationException() throws Exception {
        // Given
        InsufficientAuthenticationException exception = new InsufficientAuthenticationException("Not authenticated");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getOutputStream()).thenReturn(testServletOutputStream);

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType("application/json");

        String jsonResponse = baos.toString();
        assertTrue(jsonResponse.contains("401"));
    }

    @Test
    void handleErrorResponse_WithRequestAndResponse_ShouldHandleMismatchedInputException() throws Exception {
        // Given
        MismatchedInputException exception = mock(MismatchedInputException.class);
        when(exception.getMessage()).thenReturn("Mismatched input");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getOutputStream()).thenReturn(testServletOutputStream);

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
        verify(response).setContentType("application/json");

        String jsonResponse = baos.toString();
        assertTrue(jsonResponse.contains("400"));
    }

    @ParameterizedTest
    @MethodSource("provideBadRequestExceptions")
    void handleErrorResponse_WithRequestAndResponse_ShouldHandleBadRequestExceptions(Exception exception) throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getOutputStream()).thenReturn(testServletOutputStream);

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
        verify(response).setContentType("application/json");

        String jsonResponse = baos.toString();
        assertTrue(jsonResponse.contains(exception.getMessage()));
    }

    private static Stream<Arguments> provideBadRequestExceptions() {
        return Stream.of(
                Arguments.of(new DisabledException("Compte désactivé")),
                Arguments.of(new LockedException("Compte verrouillé")),
                Arguments.of(new BadCredentialsException("Identifiants incorrects")),
                Arguments.of(new CredentialsExpiredException("Credentials expired")),
                Arguments.of(new ApiException("Erreur API"))
        );
    }

    @Test
    void handleErrorResponse_WithRequestAndResponse_ShouldHandleGenericException() throws Exception {
        // Given
        Exception exception = new Exception("Erreur générique");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getOutputStream()).thenReturn(testServletOutputStream);

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        verify(response).setContentType("application/json");

        String jsonResponse = baos.toString();
        assertTrue(jsonResponse.contains("Une erreur interne du serveur s'est produite"));
    }

    @Test
    void handleErrorResponse_WithRequestAndResponse_ShouldHandleNullPointerException() throws Exception {
        // Given
        NullPointerException exception = new NullPointerException("Null value");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getOutputStream()).thenReturn(testServletOutputStream);

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        verify(response).setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        verify(response).setContentType("application/json");
    }

    @Test
    void handleErrorResponse_WithRequestAndResponse_ShouldThrowApiException_WhenOutputStreamFails() throws Exception {
        // Given
        Exception exception = new Exception("Test");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getOutputStream()).thenThrow(new IOException("Stream error"));

        // When & Then
        assertThrows(ApiException.class, () -> {
            RequestUtils.handleErrorResponse(request, response, exception);
        });
    }

    @Test
    void handleErrorResponse_WithRequestAndResponse_ShouldWriteValidJson() throws Exception {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getOutputStream()).thenReturn(testServletOutputStream);

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        String jsonResponse = baos.toString();
        ObjectMapper mapper = new ObjectMapper();
        Response responseObj = mapper.readValue(jsonResponse, Response.class);
        assertNotNull(responseObj);
        assertEquals(403, responseObj.code());
        assertEquals("/api/test", responseObj.path());
    }

    @Test
    void handleErrorResponse_WithRequestAndResponse_ShouldCallGetErrorResponseWithCorrectStatus() throws Exception {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getOutputStream()).thenReturn(testServletOutputStream);

        // When
        RequestUtils.handleErrorResponse(request, response, exception);

        // Then
        String jsonResponse = baos.toString();
        assertTrue(jsonResponse.contains("403"));
        assertTrue(jsonResponse.contains("FORBIDDEN"));
    }

    // TESTS POUR LES FONCTIONS PRIVÉES

    @Test
    void errorReason_ShouldReturnForbiddenMessage() throws Exception {
        java.lang.reflect.Field field = RequestUtils.class.getDeclaredField("errorReason");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.function.BiFunction<Exception, HttpStatus, String> errorReason =
                (java.util.function.BiFunction<Exception, HttpStatus, String>) field.get(null);

        Exception exception = new AccessDeniedException("Access denied");
        String result = errorReason.apply(exception, HttpStatus.FORBIDDEN);

        assertEquals("Vous n'avez pas suffisamment d'autorisations", result);
    }

    @Test
    void errorReason_ShouldReturnUnauthorizedMessage_ForNonExpiredJwt() throws Exception {
        java.lang.reflect.Field field = RequestUtils.class.getDeclaredField("errorReason");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.function.BiFunction<Exception, HttpStatus, String> errorReason =
                (java.util.function.BiFunction<Exception, HttpStatus, String>) field.get(null);

        Exception exception = new InsufficientAuthenticationException("Not authenticated");
        String result = errorReason.apply(exception, HttpStatus.UNAUTHORIZED);

        assertEquals("Vous n'êtes pas connecté", result);
    }

    @Test
    void errorReason_ShouldReturnSessionExpiredMessage_ForExpiredJwt() throws Exception {
        java.lang.reflect.Field field = RequestUtils.class.getDeclaredField("errorReason");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.function.BiFunction<Exception, HttpStatus, String> errorReason =
                (java.util.function.BiFunction<Exception, HttpStatus, String>) field.get(null);

        Exception exception = new InvalidBearerTokenException("Jwt expired at 2026-01-01");
        String result = errorReason.apply(exception, HttpStatus.UNAUTHORIZED);

        assertEquals("Votre session a expiré", result);
    }

    @Test
    void errorReason_ShouldReturnExceptionMessage_ForBadRequestExceptions() throws Exception {
        java.lang.reflect.Field field = RequestUtils.class.getDeclaredField("errorReason");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.function.BiFunction<Exception, HttpStatus, String> errorReason =
                (java.util.function.BiFunction<Exception, HttpStatus, String>) field.get(null);

        Exception exception = new DisabledException("Compte désactivé");
        String result = errorReason.apply(exception, HttpStatus.BAD_REQUEST);

        assertEquals("Compte désactivé", result);
    }

    @Test
    void errorReason_ShouldReturnServerErrorMessage_For5xxErrors() throws Exception {
        java.lang.reflect.Field field = RequestUtils.class.getDeclaredField("errorReason");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.function.BiFunction<Exception, HttpStatus, String> errorReason =
                (java.util.function.BiFunction<Exception, HttpStatus, String>) field.get(null);

        Exception exception = new Exception("Generic error");
        String result = errorReason.apply(exception, HttpStatus.INTERNAL_SERVER_ERROR);

        assertEquals("Une erreur interne du serveur s'est produite", result);
    }

    @Test
    void errorReason_ShouldReturnGenericErrorMessage_ForOtherErrors() throws Exception {
        java.lang.reflect.Field field = RequestUtils.class.getDeclaredField("errorReason");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.function.BiFunction<Exception, HttpStatus, String> errorReason =
                (java.util.function.BiFunction<Exception, HttpStatus, String>) field.get(null);

        Exception exception = new Exception("Generic error");
        String result = errorReason.apply(exception, HttpStatus.NOT_FOUND);

        assertEquals("Une erreur s'est produite. Veuillez réessayer.", result);
    }

    @Test
    void convertResponse_ShouldConvertDataToTargetType() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("user", Map.of("id", 1, "name", "John"));

        Response responseObj = new Response(
                LocalTime.now().toString(),
                200,
                "/api/test",
                HttpStatus.OK,
                "Success",
                "",
                data
        );

        // When
        Map<String, Object> result = RequestUtils.convertResponse(responseObj, Map.class, "user");

        // Then
        assertNotNull(result);
        assertEquals(1, result.get("id"));
        assertEquals("John", result.get("name"));
    }

    @Test
    void writeResponse_ShouldWriteToOutputStream() throws Exception {
        java.lang.reflect.Field field = RequestUtils.class.getDeclaredField("writeResponse");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.function.BiConsumer<HttpServletResponse, Response> writeResponse =
                (java.util.function.BiConsumer<HttpServletResponse, Response>) field.get(null);

        Response testResponse = new Response(
                LocalTime.now().toString(),
                200,
                "/api/test",
                HttpStatus.OK,
                "Success",
                "",
                new HashMap<>()
        );

        when(response.getOutputStream()).thenReturn(testServletOutputStream);

        // When
        writeResponse.accept(response, testResponse);

        // Then
        verify(response).getOutputStream();
        assertTrue(baos.size() > 0);
    }

    @Test
    void writeResponse_ShouldThrowApiException_WhenIOExceptionOccurs() throws Exception {
        java.lang.reflect.Field field = RequestUtils.class.getDeclaredField("writeResponse");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.function.BiConsumer<HttpServletResponse, Response> writeResponse =
                (java.util.function.BiConsumer<HttpServletResponse, Response>) field.get(null);

        Response testResponse = new Response(
                LocalTime.now().toString(),
                200,
                "/api/test",
                HttpStatus.OK,
                "Success",
                "",
                new HashMap<>()
        );

        when(response.getOutputStream()).thenThrow(new IOException("Stream error"));

        // When & Then
        assertThrows(ApiException.class, () -> {
            writeResponse.accept(response, testResponse);
        });
    }

    // Classe interne pour simuler ServletOutputStream
    private static class TestServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream baos;

        public TestServletOutputStream(ByteArrayOutputStream baos) {
            this.baos = baos;
        }

        @Override
        public void write(int b) throws IOException {
            baos.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener listener) {
            // Non utilisé dans les tests
        }
    }
}