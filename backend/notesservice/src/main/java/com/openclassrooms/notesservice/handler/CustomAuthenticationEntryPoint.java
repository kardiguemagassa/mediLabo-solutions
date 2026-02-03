package com.openclassrooms.notesservice.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import java.io.IOException;
import static com.openclassrooms.notesservice.util.RequestUtils.handleErrorResponse;

/**
 * Gestionnaire personnalisé pour les erreurs 401 Unauthorized
 * @author Kardigué MAGASSA
 *  @version 1.0
 * @since 2026-02-02
 *
 */

@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        handleErrorResponse(request, response, exception);
    }
}