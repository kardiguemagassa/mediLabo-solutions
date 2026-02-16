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
import org.springframework.security.access.AccessDeniedException;
import java.io.IOException;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class CustomAccessDeniedHandlerTest {

    @InjectMocks
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Test
    @DisplayName("Handler : Devrait appeler RequestUtils.handleErrorResponse lors d'un accès refusé")
    void handle_ShouldCallHandleErrorResponse() throws IOException, ServletException {
        // 1. GIVEN
        AccessDeniedException accessDeniedException = new AccessDeniedException("Access Denied");

        // 2. Utilisation de MockedStatic pour RequestUtils
        try (MockedStatic<RequestUtils> mockedRequestUtils = mockStatic(RequestUtils.class)) {

            // 3. WHEN
            customAccessDeniedHandler.handle(request, response, accessDeniedException);

            // 4. THEN
            // On vérifie que la méthode statique a bien été appelée avec les bons arguments
            mockedRequestUtils.verify(() ->
                    RequestUtils.handleErrorResponse(request, response, accessDeniedException)
            );
        }
    }
}