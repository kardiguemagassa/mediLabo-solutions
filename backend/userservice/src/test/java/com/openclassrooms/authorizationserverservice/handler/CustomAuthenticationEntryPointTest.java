package com.openclassrooms.authorizationserverservice.handler;

import com.openclassrooms.authorizationserverservice.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import java.io.IOException;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationEntryPointTest {

    @InjectMocks
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Test
    @DisplayName("EntryPoint : Devrait déléguer à RequestUtils lors d'une tentative non authentifiée")
    void commence_ShouldCallHandleErrorResponse() throws IOException, ServletException {
        // GIVEN : On crée une exception d'authentification (ex: BadCredentials ou InsufficientAuthentication)
        AuthenticationException authException = new org.springframework.security.authentication.BadCredentialsException("Not authenticated");

        // Utilisation de MockedStatic pour intercepter l'appel à la méthode utilitaire statique
        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {

            // WHEN : On appelle la méthode commence
            customAuthenticationEntryPoint.commence(request, response, authException);

            // THEN : On vérifie que RequestUtils.handleErrorResponse a été appelé exactement une fois
            mockedRequestUtils.verify(() ->
                    RequestUtils.handleErrorResponse(request, response, authException)
            );
        }
    }
}