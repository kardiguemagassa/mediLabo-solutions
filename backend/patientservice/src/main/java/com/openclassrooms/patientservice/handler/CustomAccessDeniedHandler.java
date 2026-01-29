package com.openclassrooms.patientservice.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

import static com.openclassrooms.patientservice.util.RequestUtils.handleErrorResponse;

/**
 * Handler personnalisé pour les erreurs 403 Forbidden.
 * Appelé quand un utilisateur authentifié n'a pas les permissions requises.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("Access denied for {} {} - User lacks required permissions", request.getMethod(), request.getRequestURI());
        handleErrorResponse(request, response, accessDeniedException);
    }
}
