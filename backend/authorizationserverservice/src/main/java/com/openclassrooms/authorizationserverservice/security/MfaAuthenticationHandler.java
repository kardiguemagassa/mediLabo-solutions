package com.openclassrooms.authorizationserverservice.security;

import com.openclassrooms.authorizationserverservice.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.io.IOException;

/**
 * Gestionnaire de succès d’authentification intégrant la logique de
 * Multi-Factor Authentication (MFA).
 *
 * <p>
 * Cette classe intercepte une authentification réussie (login + mot de passe)
 * afin de déterminer si l’utilisateur doit passer par une étape MFA
 * avant d’être totalement authentifié.
 * </p>
 *
 * <h2>Fonctionnement</h2>
 * <ul>
 *   <li>Si l’utilisateur n’a pas activé le MFA → connexion normale</li>
 *   <li>Si le MFA est activé → l’utilisateur est placé dans un état
 *       {@link MfaAuthentication}</li>
 * </ul>
 *
 * <p>
 * Cet état temporaire empêche l’accès aux ressources protégées tant que
 * le second facteur (TOTP, SMS, etc.) n’a pas été validé.
 * </p>
 *
 * <h2>Sécurité</h2>
 * <p>
 * La session HTTP contient une authentification partielle, garantissant
 * qu’un mot de passe seul ne suffit jamais lorsque le MFA est activé.
 * </p>
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-05-01
 */

public class MfaAuthenticationHandler implements AuthenticationSuccessHandler {
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final AuthenticationSuccessHandler mfaNotEnabled = new SavedRequestAwareAuthenticationSuccessHandler();
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final String authority;

    /**
     * Crée un gestionnaire MFA.
     *
     * @param successUrl l’URL vers laquelle l’utilisateur est redirigé
     *                   pour effectuer la validation MFA
     * @param authority le rôle temporaire accordé pendant la phase MFA
     *                  (ex: ROLE_MFA_REQUIRED)
     */
    public MfaAuthenticationHandler(String successUrl, String authority) {
        SimpleUrlAuthenticationSuccessHandler authenticationSuccessHandler = new SimpleUrlAuthenticationSuccessHandler(successUrl);
        authenticationSuccessHandler.setAlwaysUseDefaultTargetUrl(true);
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.authority = authority;
    }

    /**
     * Intercepte une authentification réussie afin d'appliquer la logique MFA.
     *
     * <p>
     * Si l’utilisateur n'a pas activé le MFA, la connexion est finalisée
     * immédiatement.
     * </p>
     *
     * <p>
     * Si le MFA est activé, l’authentification est remplacée par une
     * {@link MfaAuthentication} et stockée dans la session afin de forcer
     * l'étape de validation du second facteur.
     * </p>
     *
     * @param request la requête HTTP
     * @param response la réponse HTTP
     * @param authentication l’authentification réussie initiale
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            var user = (User) authentication.getPrincipal();
            if (!user.isMfa()) {
                mfaNotEnabled.onAuthenticationSuccess(request, response, authentication);
                return;
            }
        }
        /**
         * Cet utilisateur existe, mais il n'est pas encore digne de confiance
         */
        saveAuthentication(request, response, new MfaAuthentication(authentication, authority));
        authenticationSuccessHandler.onAuthenticationSuccess(request, response, authentication);
    }

    /**
     * Enregistre une authentification MFA dans le contexte de sécurité
     * et dans la session HTTP.
     *
     * <p>
     * Cela permet de conserver l’état MFA entre les requêtes jusqu’à
     * validation du second facteur.
     * </p>
     *
     * @param request la requête HTTP
     * @param response la réponse HTTP
     * @param authentication l’authentification MFA temporaire
     */
    private void saveAuthentication(HttpServletRequest request, HttpServletResponse response, MfaAuthentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
    }
}
