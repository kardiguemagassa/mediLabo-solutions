package com.openclassrooms.assessmentservice.exception;

import com.openclassrooms.assessmentservice.domain.Response;
import com.openclassrooms.assessmentservice.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.WebRequest;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
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
    private HttpHeaders httpHeaders;

    @InjectMocks
    private HandleException handleException;

    private Exception testException;
    private ApiException apiException;
    private AccessDeniedException accessDeniedException;
    private ServiceUnavailableException serviceUnavailableException;

    @BeforeEach
    void setUp() {
        testException = new Exception("Test exception message");
        apiException = new ApiException("API exception message");
        accessDeniedException = new AccessDeniedException("Access denied");
        serviceUnavailableException = new ServiceUnavailableException("Service unavailable");
    }

    @Test
    void handleExceptionInternal_ShouldReturnResponseEntityWithErrorResponse() {
        // Arrange
        HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;
        String expectedMessage = "Test exception message";

        Response mockResponse = Response.builder()
                .message(expectedMessage)
                .build();

        try (var mockedStatic = mockStatic(RequestUtils.class)) {
            mockedStatic.when(() -> RequestUtils.handleErrorResponse(
                    eq(expectedMessage),
                    anyString(),
                    eq(request),
                    eq(statusCode)
            )).thenReturn(mockResponse);

            // Act
            ResponseEntity<Object> response = handleException.handleExceptionInternal(
                    testException, null, httpHeaders, statusCode, webRequest
            );

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(mockResponse, response.getBody());
        }
    }

    @Test
    void apiException_ShouldReturnBadRequestResponse() {
        // Arrange
        Response mockResponse = Response.builder()
                .message("API exception message")
                .build();

        try (var mockedStatic = mockStatic(RequestUtils.class)) {
            mockedStatic.when(() -> RequestUtils.handleErrorResponse(
                    eq("API exception message"),
                    anyString(),
                    eq(request),
                    eq(HttpStatus.BAD_REQUEST)
            )).thenReturn(mockResponse);

            // Act
            ResponseEntity<Response> response = handleException.apiException(apiException);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(mockResponse, response.getBody());
        }
    }

    @Test
    void accessDeniedException_ShouldReturnForbiddenResponse() {
        // Arrange
        Response mockResponse = Response.builder()
                .message("Accès refusé")
                .build();

        try (var mockedStatic = mockStatic(RequestUtils.class)) {
            mockedStatic.when(() -> RequestUtils.handleErrorResponse(
                    eq("Accès refusé. Vous n'avez pas accès."),
                    anyString(),
                    eq(request),
                    eq(HttpStatus.FORBIDDEN)
            )).thenReturn(mockResponse);

            // Act
            ResponseEntity<Response> response = handleException.accessDeniedException(accessDeniedException);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Test
    void exception_ShouldReturnInternalServerErrorResponse() {
        // Arrange
        Response mockResponse = Response.builder()
                .message("Internal server error")
                .build();

        try (var mockedStatic = mockStatic(RequestUtils.class)) {
            mockedStatic.when(() -> RequestUtils.handleErrorResponse(
                    anyString(),
                    anyString(),
                    eq(request),
                    eq(HttpStatus.INTERNAL_SERVER_ERROR)
            )).thenReturn(mockResponse);

            // Act
            ResponseEntity<Response> response = handleException.exception(testException);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Test
    void handleServiceUnavailable_ShouldReturnServiceUnavailableResponse() {
        // Arrange
        Response mockResponse = Response.builder()
                .message("Service unavailable")
                .build();

        try (var mockedStatic = mockStatic(RequestUtils.class)) {
            mockedStatic.when(() -> RequestUtils.handleErrorResponse(
                    eq("Service temporairement indisponible. Veuillez réessayer plus tard."),
                    anyString(),
                    eq(request),
                    eq(HttpStatus.SERVICE_UNAVAILABLE)
            )).thenReturn(mockResponse);

            // Act
            ResponseEntity<Response> response = handleException.handleServiceUnavailable(serviceUnavailableException);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        }
    }

    @Test
    void testProcessErrorMessage_WithNullMessage() throws Exception {
        // Arrange
        Exception exception = new Exception((String) null);

        // Utiliser la réflexion pour accéder à la méthode privée
        var method = HandleException.class.getDeclaredMethod("processErrorMessage", Exception.class);
        method.setAccessible(true);

        // Act
        String result = (String) method.invoke(handleException, exception);

        // Assert
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", result);
    }

    @Test
    void testProcessErrorMessage_WithEmptyMessage() throws Exception {
        // Arrange
        Exception exception = new Exception("");

        // Utiliser la réflexion pour accéder à la méthode privée
        var method = HandleException.class.getDeclaredMethod("processErrorMessage", Exception.class);
        method.setAccessible(true);

        // Act
        String result = (String) method.invoke(handleException, exception);

        // Assert
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", result);
    }

    @Test
    void exception_WithEmailDuplicateMessage_ShouldProcessCorrectly() {
        // Arrange
        Exception emailDuplicateException = new Exception(
                "duplicate key value violates unique constraint \"Key (email)\""
        );

        Response mockResponse = Response.builder()
                .message("Email duplicate error")
                .build();

        try (var mockedStatic = mockStatic(RequestUtils.class)) {
            mockedStatic.when(() -> RequestUtils.handleErrorResponse(
                    eq("Cette adresse e-mail existe déjà. Veuillez utiliser une autre adresse e-mail et réessayer."),
                    anyString(),
                    eq(request),
                    eq(HttpStatus.INTERNAL_SERVER_ERROR)
            )).thenReturn(mockResponse);

            // Act
            ResponseEntity<Response> response = handleException.exception(emailDuplicateException);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Test
    void handleExceptionInternal_ShouldCallGetRootCauseMessage() {
        // Arrange
        HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;

        try (var exceptionUtilsMock = mockStatic(org.apache.commons.lang3.exception.ExceptionUtils.class)) {
            exceptionUtilsMock.when(() -> getRootCauseMessage(any(Exception.class)))
                    .thenReturn("Root cause message");

            Response mockResponse = Response.builder()
                    .message("Test")
                    .build();

            try (var requestUtilsMock = mockStatic(RequestUtils.class)) {
                requestUtilsMock.when(() -> RequestUtils.handleErrorResponse(
                        anyString(),
                        anyString(),
                        any(HttpServletRequest.class),
                        any(HttpStatusCode.class)
                )).thenReturn(mockResponse);

                // Act
                ResponseEntity<Object> response = handleException.handleExceptionInternal(
                        testException, null, httpHeaders, statusCode, webRequest
                );

                // Assert
                assertNotNull(response);
                exceptionUtilsMock.verify(() -> getRootCauseMessage(testException), times(1));
            }
        }
    }
}