package com.openclassrooms.authorizationserverservice.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.openclassrooms.authorizationserverservice.util.RequestUtils.handleErrorResponse;

/**
 * <p>
 * Gestionnaire personnalisé des accès refusés (Access Denied) pour Spring Security.
 * Cette classe est déclenchée lorsqu'un utilisateur authentifié tente d'accéder
 * à une ressource pour laquelle il n'a pas les autorisations nécessaires.
 * </p>
 *
 * <p>
 * Fonctionnalités principales :
 * </p>
 * <ul>
 *     <li>Intercepte les exceptions de type {@link AccessDeniedException}</li>
 *     <li>Utilise la méthode {@link com.openclassrooms.authorizationserverservice.util.RequestUtils#handleErrorResponse(HttpServletRequest, HttpServletResponse, Exception)}
 *         pour générer une réponse structurée adaptée (JSON ou page d'erreur)</li>
 *     <li>Permet une personnalisation centralisée des messages d'erreur pour les accès interdits</li>
 * </ul>
 *
 * <p>
 * Exemple d’utilisation : intégré automatiquement dans la configuration Spring Security
 * via l’appel à {@code http.exceptionHandling().accessDeniedHandler(customAccessDeniedHandler);}
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
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * <p>
     * Méthode appelée lorsqu'un utilisateur tente d'accéder à une ressource protégée
     * sans disposer des autorisations nécessaires.
     * </p>
     *
     * <p>
     * Cette méthode délègue le traitement à {@link com.openclassrooms.authorizationserverservice.util.RequestUtils#handleErrorResponse(HttpServletRequest, HttpServletResponse, Exception)},
     * afin de renvoyer une réponse cohérente à l'utilisateur.
     * </p>
     *
     * @param request   la requête HTTP
     * @param response  la réponse HTTP
     * @param exception l'exception AccessDeniedException déclenchée
     * @throws IOException      si une erreur d’entrée/sortie se produit
     * @throws ServletException si une erreur de servlet se produit
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException, ServletException {
        handleErrorResponse(request, response, exception);
    }
}