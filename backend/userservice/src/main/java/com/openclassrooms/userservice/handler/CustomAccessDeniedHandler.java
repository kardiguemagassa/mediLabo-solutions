package com.openclassrooms.userservice.handler;

import com.openclassrooms.userservice.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.openclassrooms.userservice.util.RequestUtils.handleErrorResponse;

/**
 * Gestionnaire personnalisé des accès refusés (Access Denied) pour Spring Security.
 * Cette classe est déclenchée lorsqu'un utilisateur authentifié tente d'accéder
 * à une ressource pour laquelle il n'a pas les autorisations nécessaires.
 * Fonctionnalités principales :
 *  Intercepte les exceptions de type {@link AccessDeniedException}
 * Utilise la méthode {@link RequestUtils#handleErrorResponse(HttpServletRequest, HttpServletResponse, Exception)}
 *         pour générer une réponse structurée adaptée (JSON ou page d'erreur)
 * Permet une personnalisation centralisée des messages d'erreur pour les accès interdits
 * Exemple d’utilisation : intégré automatiquement dans la configuration Spring Security
 * via l’appel à {@code http.exceptionHandling().accessDeniedHandler(customAccessDeniedHandler);}
 *
 * @author : Kardigué MAGASSA
 * @version 1.0
 * @Since : 2026-05-01
 */

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * Méthode appelée lorsqu'un utilisateur tente d'accéder à une ressource protégée
     * sans disposer des autorisations nécessaires.
     * Cette méthode délègue le traitement à {@link RequestUtils#handleErrorResponse(HttpServletRequest, HttpServletResponse, Exception)},
     * afin de renvoyer une réponse cohérente à l'utilisateur.
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