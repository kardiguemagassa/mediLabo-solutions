package com.openclassrooms.discoveryserverservice.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryAccessDeniedHandlerTest {

    private final DiscoveryAccessDeniedHandler accessDeniedHandler = new DiscoveryAccessDeniedHandler();

    @Test
    void handle_ShouldSetForbiddenStatus() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException exception = new AccessDeniedException("Test message");

        // Act
        accessDeniedHandler.handle(request, response, exception);

        // Assert
        assertThat(response.getStatus()).isEqualTo(403);
        //Vérifier que le message commence par le texte attendu
        assertThat(response.getContentAsString())
                .startsWith("HTTP Status 403 ->  Accès refusé: Test message");
    }

    @Test
    void handle_ShouldHandleNullExceptionMessage() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException exception = new AccessDeniedException(null);

        // Act
        accessDeniedHandler.handle(request, response, exception);

        // Assert
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .startsWith("HTTP Status 403 ->  Accès refusé: null");
    }

    @Test
    void handle_ShouldWorkWithNullRequest() throws IOException, ServletException {
        // Arrange
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException exception = new AccessDeniedException("Test");

        // Act
        accessDeniedHandler.handle(null, response, exception);

        // Assert
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .startsWith("HTTP Status 403 ->  Accès refusé: Test");
    }

    @Test
    void handle_ShouldAddNewlineAtEnd() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException exception = new AccessDeniedException("Test");

        // Act
        accessDeniedHandler.handle(request, response, exception);

        // Assert
        assertThat(response.getContentAsString())
                .endsWith("\n");
    }
}