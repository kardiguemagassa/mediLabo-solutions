package com.openclassrooms.notesservice.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import static com.openclassrooms.notesservice.util.RequestUtils.handleErrorResponse;

/**
 * Gestionnaire personnalisé pour les erreurs 403 Forbidden
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-02
 */

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        handleErrorResponse(request, response, exception);
    }
}