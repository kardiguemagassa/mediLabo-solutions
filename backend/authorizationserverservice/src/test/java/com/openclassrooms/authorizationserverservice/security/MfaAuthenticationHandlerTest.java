package com.openclassrooms.authorizationserverservice.security;

import com.openclassrooms.authorizationserverservice.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaAuthenticationHandlerTest {

    private MfaAuthenticationHandler handler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private final String successUrl = "/mfa-verify";
    private final String authority = "ROLE_MFA_REQUIRED";

    @BeforeEach
    void setUp() {
        handler = new MfaAuthenticationHandler(successUrl, authority);
    }

    @Test
    @DisplayName("MFA : Si désactivé, devrait déléguer au handler de succès standard")
    void onAuthenticationSuccess_WhenMfaDisabled_ShouldProceedNormally() throws IOException, ServletException {
        // GIVEN
        User user = new User();
        user.setMfa(false); // MFA désactivé
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);

        // WHEN
        handler.onAuthenticationSuccess(request, response, auth);

        // THEN
        // On vérifie qu'on n'a pas déclenché de redirection MFA (le comportement par défaut de SavedRequestAware)
        verify(response, never()).sendRedirect(successUrl);
    }

    @Test
    @DisplayName("MFA : Si activé, devrait rediriger vers l'URL MFA")
    void onAuthenticationSuccess_WhenMfaEnabled_ShouldRedirectToMfa() throws IOException, ServletException {
        // 1. GIVEN
        when(request.getContextPath()).thenReturn("");

        // IMPORTANT : On simule le comportement de l'encodage d'URL
        // Sinon, Spring passe 'null' au redirect car le mock renvoie null par défaut
        when(response.encodeRedirectURL(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        User user = new User();
        user.setMfa(true);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);

        // 2. WHEN
        handler.onAuthenticationSuccess(request, response, auth);

        // 3. THEN
        // Maintenant, sendRedirect recevra bien "/mfa-verify" au lieu de null
        verify(response).sendRedirect("/mfa-verify");
    }

    @Test
    @DisplayName("Edge Case : Si pas UsernamePasswordToken, redirection MFA par défaut")
    void onAuthenticationSuccess_WhenNotUsernamePasswordToken_ShouldForceMfa() throws IOException, ServletException {
        // 1. GIVEN
        when(request.getContextPath()).thenReturn("");

        // ligne pour empêcher le redirect(null)
        when(response.encodeRedirectURL(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        Authentication otherAuth = mock(Authentication.class);

        // 2. WHEN
        handler.onAuthenticationSuccess(request, response, otherAuth);

        // 3. THEN
        // Utilise eq() ou directement la String pour être précis
        verify(response).sendRedirect("/mfa-verify");
    }

}