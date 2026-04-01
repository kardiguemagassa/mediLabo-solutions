package com.openclassrooms.userservice.handler;

import com.openclassrooms.userservice.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.openclassrooms.userservice.util.RequestUtils.handleErrorResponse;

/**
 * Point d’entrée personnalisé pour Spring Security lorsqu’un utilisateur non authentifié tente d’accéder à une ressource protégée.
 * Fonctionnalités principales :
 * Intercepte les tentatives d’accès non authentifiées
 * Délègue le traitement à {@link RequestUtils#handleErrorResponse(HttpServletRequest, HttpServletResponse, Exception)}
 *        pour générer une réponse cohérente (JSON ou page d’erreur)
 * Permet de centraliser la gestion des erreurs d’authentification dans l’application
 * Exemple d’utilisation : configuré dans Spring Security via
 * {@code http.exceptionHandling().authenticationEntryPoint(customAuthenticationEntryPoint);}

 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 11/14/25
 */

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Méthode appelée lorsqu’un utilisateur non authentifié tente d’accéder à une ressource protégée.
     * Cette méthode délègue le traitement à {@link RequestUtils#handleErrorResponse(HttpServletRequest, HttpServletResponse, Exception)},
     * qui renvoie une réponse appropriée à l’utilisateur.
     * @param request   la requête HTTP
     * @param response  la réponse HTTP
     * @param exception l’exception d’authentification déclenchée
     * @throws IOException      si une erreur d’entrée/sortie se produit
     * @throws ServletException si une erreur de servlet se produit
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        handleErrorResponse(request, response, exception);
    }
}