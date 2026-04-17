package com.openclassrooms.notificationservice.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.openclassrooms.notificationservice.utils.RequestUtils.handleErrorResponse;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException, ServletException {
        handleErrorResponse(request, response, exception);
    }
}