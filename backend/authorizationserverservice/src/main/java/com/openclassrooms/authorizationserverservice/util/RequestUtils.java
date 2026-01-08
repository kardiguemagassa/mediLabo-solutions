package com.openclassrooms.authorizationserverservice.util;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

/**
 * Utilitaires pour les requêtes HTTP
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

public class RequestUtils {

    public static String getMessage(HttpServletRequest request) {
        var status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return String.format("%s - Page non trouvée", statusCode);
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return String.format("%s - Erreur interne du serveur", statusCode);
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return String.format("%s - Accès interdit", statusCode);
            }
        }
        return "Une erreur est survenue";
    }
}
