package com.openclassrooms.authorizationserverservice.util;

import com.openclassrooms.authorizationserverservice.domain.Response;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


class RequestUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        outputStream = new ByteArrayOutputStream();

        // On simule le comportement du flux de sortie de la réponse
        when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(outputStream));
    }

    @Test
    @DisplayName("Doit retourner un message 403 personnalisé lors d'une AccessDeniedException")
    void handleErrorResponse_AccessDenied_ShouldReturnForbiddenResponse() {
        // GIVEN
        AccessDeniedException exception = new AccessDeniedException("Access Denied");
        when(request.getRequestURI()).thenReturn("/api/test");

        // WHEN
        RequestUtils.handleErrorResponse(request, response, exception);

        // THEN
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());

        String jsonResponse = outputStream.toString();
        assertThat(jsonResponse).contains("403");
        assertThat(jsonResponse).contains("Vous n'avez pas suffisamment d'autorisation");
    }

    @Test
    @DisplayName("Doit générer un objet Response correct via getResponse")
    void getResponse_ShouldReturnValidResponseObject() {
        // GIVEN
        when(request.getRequestURI()).thenReturn("/api/success");
        String message = "Opération réussie";

        // WHEN
        Response result = RequestUtils.getResponse(request, Map.of(), message, HttpStatus.OK);

        // THEN
        // Utilisation des méthodes du Record (pas de préfixe 'get')
        assertThat(result.code()).isEqualTo(200);
        assertThat(result.message()).isEqualTo(message);
        assertThat(result.path()).isEqualTo("/api/success");
    }

    @Test
    @DisplayName("getMessage doit retourner 'Page non trouvée' pour un code 404")
    void getMessage_ShouldReturnNotFoundMessage() {
        // GIVEN
        // On simule l'attribut que RequestDispatcher injecte lors d'une erreur
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(404);

        // WHEN
        String message = RequestUtils.getMessage(request);

        // THEN
        assertThat(message).isEqualTo("404 - Page non trouvée");
    }

    @Test
    @DisplayName("getMessage doit retourner 'Erreur interne' pour un code 500")
    void getMessage_ShouldReturnInternalErrorMessage() {
        // GIVEN
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(500);

        // WHEN
        String message = RequestUtils.getMessage(request);

        // THEN
        assertThat(message).isEqualTo("500 - Erreur interne du serveur");
    }

    @Test
    @DisplayName("getMessage doit retourner 'Erreur interne du serveur' pour un code 500")
    void getMessage_ShouldReturnInternalServerErrorMessage() {
        // GIVEN
        // RequestDispatcher.ERROR_STATUS_CODE vaut "jakarta.servlet.error.status_code"
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(500);

        // WHEN
        String message = RequestUtils.getMessage(request);

        // THEN
        assertThat(message).isEqualTo("500 - Erreur interne du serveur");
    }

    @Test
    @DisplayName("getMessage doit retourner 'Accès interdit' pour un code 403")
    void getMessage_ShouldReturnForbiddenMessage() {
        // GIVEN
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(403);

        // WHEN
        String message = RequestUtils.getMessage(request);

        // THEN
        assertThat(message).isEqualTo("403 - Accès interdit");
    }

    @ParameterizedTest
    @CsvSource({
            "404, 404 - Page non trouvée",
            "500, 500 - Erreur interne du serveur",
            "403, 403 - Accès interdit"
    })
    void getMessage_ShouldReturnCorrectMessageForStatus(int statusCode, String expectedMessage) {
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(statusCode);

        String message = RequestUtils.getMessage(request);

        assertThat(message).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("handleErrorResponse (manuel) doit construire un objet Response structuré")
    void handleErrorResponse_Manual_ShouldReturnCorrectResponse() {
        // GIVEN
        String customMessage = "Erreur de validation";
        String exceptionMessage = "InvalidFieldException";
        HttpStatusCode status = HttpStatus.BAD_REQUEST;
        when(request.getRequestURI()).thenReturn("/api/register");

        // WHEN
        Response responseResult = RequestUtils.handleErrorResponse(
                customMessage,
                exceptionMessage,
                request,
                status
        );

        // THEN
        assertThat(responseResult.code()).isEqualTo(400);
        assertThat(responseResult.message()).isEqualTo(customMessage);
        assertThat(responseResult.exception()).isEqualTo(exceptionMessage);
        assertThat(responseResult.path()).isEqualTo("/api/register");
        assertThat(responseResult.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseResult.data()).isEmpty();
        assertThat(responseResult.time()).isNotNull();
    }

    @Test
    @DisplayName("Doit gérer InvalidBearerTokenException avec un statut 401")
    void handleErrorResponse_InvalidToken_ShouldReturnUnauthorized() {
        // GIVEN
        InvalidBearerTokenException exception = new InvalidBearerTokenException("Token expired");
        when(request.getRequestURI()).thenReturn("/api/secure");

        // WHEN
        RequestUtils.handleErrorResponse(request, response, exception);

        // THEN
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        assertThat(outputStream.toString()).contains("401");
    }

    @Test
    @DisplayName("Doit gérer InsufficientAuthenticationException avec un statut 401")
    void handleErrorResponse_InsufficientAuth_ShouldReturnUnauthorized() {
        // GIVEN
        InsufficientAuthenticationException exception = new InsufficientAuthenticationException("Not logged in");

        // WHEN
        RequestUtils.handleErrorResponse(request, response, exception);

        // THEN
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Doit gérer les exceptions de type BAD_REQUEST (Disabled, Locked, BadCredentials, etc.)")
    void handleErrorResponse_SecurityExceptions_ShouldReturnBadRequest() {
        // Test avec BadCredentialsException
        BadCredentialsException exception = new BadCredentialsException("Identifiants invalides");

        // WHEN
        RequestUtils.handleErrorResponse(request, response, exception);

        // THEN
        verify(response, atLeastOnce()).setStatus(HttpStatus.BAD_REQUEST.value());
        assertThat(outputStream.toString()).contains("Identifiants invalides");
    }

    @Test
    @DisplayName("Doit gérer toute autre exception comme une 500 Internal Server Error")
    void handleErrorResponse_OtherException_ShouldReturnInternalServerError() {
        // GIVEN
        RuntimeException exception = new RuntimeException("Bug imprévu");

        // WHEN
        RequestUtils.handleErrorResponse(request, response, exception);

        // THEN
        verify(response).setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(outputStream.toString()).contains("Une erreur s'est produite");
    }

    @Test
    @DisplayName("getMessage doit retourner le message générique si l'attribut status est null")
    void getMessage_ShouldReturnGenericMessageWhenStatusIsNull() {
        // GIVEN
        when(request.getAttribute(anyString())).thenReturn(null);

        // WHEN
        String message = RequestUtils.getMessage(request);

        // THEN
        assertThat(message).isEqualTo("Une erreur est survenue");
    }

    // Helper pour capturer les données écrites dans le flux
    private static class DelegatingServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream target;
        public DelegatingServletOutputStream(ByteArrayOutputStream target) { this.target = target; }
        @Override public void write(int b) { target.write(b); }
        @Override public boolean isReady() { return true; }
        @Override public void setWriteListener(WriteListener writeListener) {}
    }
}