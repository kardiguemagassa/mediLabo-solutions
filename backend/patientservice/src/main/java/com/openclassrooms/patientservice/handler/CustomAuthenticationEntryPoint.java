package com.openclassrooms.patientservice.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

import static com.openclassrooms.patientservice.util.RequestUtils.handleErrorResponse;

/**
 * Handler personnalisé pour les erreurs 401 Unauthorized.
 * Appelé quand une requête arrive sans token valide.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.warn("Authentication failed for {} {} - {}", request.getMethod(), request.getRequestURI(), authException.getMessage());
        handleErrorResponse(request, response, authException);
    }
}
