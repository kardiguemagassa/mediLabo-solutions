package com.openclassrooms.authorizationserverservice.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.openclassrooms.authorizationserverservice.util.RequestUtils.handleErrorResponse;

/**
 * <p>
 * Point d’entrée personnalisé pour Spring Security lorsqu’un utilisateur non authentifié
 * tente d’accéder à une ressource protégée.
 * </p>
 *
 * <p>
 * Fonctionnalités principales :
 * </p>
 * <ul>
 *     <li>Intercepte les tentatives d’accès non authentifiées</li>
 *     <li>Délègue le traitement à {@link com.openclassrooms.authorizationserverservice.util.RequestUtils#handleErrorResponse(HttpServletRequest, HttpServletResponse, Exception)}
 *         pour générer une réponse cohérente (JSON ou page d’erreur)</li>
 *     <li>Permet de centraliser la gestion des erreurs d’authentification dans l’application</li>
 * </ul>
 *
 * <p>
 * Exemple d’utilisation : configuré dans Spring Security via
 * {@code http.exceptionHandling().authenticationEntryPoint(customAuthenticationEntryPoint);}
 * </p>
 *
 * <p>
 * Auteur : Kardigué MAGASSA<br>
 * Version : 1.0<br>
 * Email : magassakara@gmail.com<br>
 * Since : 2026-05-01
 * </p>
 */

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * <p>
     * Méthode appelée lorsqu’un utilisateur non authentifié tente d’accéder à une ressource protégée.
     * </p>
     *
     * <p>
     * Cette méthode délègue le traitement à {@link com.openclassrooms.authorizationserverservice.util.RequestUtils#handleErrorResponse(HttpServletRequest, HttpServletResponse, Exception)},
     * qui renvoie une réponse appropriée à l’utilisateur.
     * </p>
     *
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