package com.openclassrooms.authorizationserverservice.controller;

import com.openclassrooms.authorizationserverservice.exception.ApiException;
import com.openclassrooms.authorizationserverservice.model.User;
import com.openclassrooms.authorizationserverservice.security.MfaAuthentication;
import com.openclassrooms.authorizationserverservice.service.UserService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

import static com.openclassrooms.authorizationserverservice.util.RequestUtils.getMessage;
import static com.openclassrooms.authorizationserverservice.util.UserUtils.getUser;

/**
 * Contrôleur pour la gestion de l'authentification utilisateur.
 *
 * <p>
 * Cette classe gère plusieurs aspects de la sécurité et de l'expérience utilisateur lors
 * de la connexion :
 * <ul>
 *     <li>Affichage de la page de login standard (/login)</li>
 *     <li>Multi-Factor Authentication (MFA) avec validation du code (/mfa)</li>
 *     <li>Gestion des erreurs liées à l'authentification (/error)</li>
 *     <li>Page de logout (/logout)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Fonctionnalités principales :
 * <ul>
 *     <li>Utilisation de {@link SecurityContextRepository} pour gérer le contexte de sécurité dans la session HTTP.</li>
 *     <li>Handlers personnalisés pour le succès et l'échec d'authentification MFA :</li>
 *     <ul>
 *         <li>{@link AuthenticationSuccessHandler} : authentification réussie</li>
 *         <li>{@link AuthenticationFailureHandler} : gestion des erreurs MFA</li>
 *     </ul>
 *     <li>Validation du code MFA via le {@link UserService} qui encapsule la logique métier.</li>
 *     <li>Remplacement temporaire de l'authentification MFA par l'authentification principale pour finaliser la session.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Gestion des erreurs :
 * <ul>
 *     <li>Redirection vers la page login si l'erreur provient d'une authentification invalide (ex: {@link ApiException}, {@link BadCredentialsException})</li>
 *     <li>Affichage d'une page d'erreur générique pour les autres exceptions</li>
 * </ul>
 * </p>
 *
 * <p>
 * Notes pédagogiques :
 * <ul>
 *     <li>L’usage de {@link @CurrentSecurityContext} permet d’accéder facilement à l’authentification courante.</li>
 *     <li>La MFA est gérée de façon sécurisée sans exposer le code principal de l’utilisateur.</li>
 *     <li>La séparation login/MFA/logout suit les bonnes pratiques MVC avec Thymeleaf ou tout moteur de template.</li>
 *     <li>Les logs via {@link lombok.extern.slf4j.Slf4j} permettent de tracer les tentatives et succès d’authentification pour audit.</li>
 * </ul>
 * </p>
 *
 * Auteur : Kardigué MAGASSA
 * Version : 1.0
 * Date : 2026-05-01
 */

@Controller
@RequiredArgsConstructor
@Slf4j
public class LoginController {
    /**
     * Gère la sauvegarde et récupération du contexte de sécurité dans la session HTTP
     */
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    /**
     * Handler pour les échecs d'authentification MFA
     */
    private final AuthenticationFailureHandler authenticatorFailureHandler = new SimpleUrlAuthenticationFailureHandler("/mfa?error");
    /**
     * Handler pour les succès d'authentification
     */
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    /**
     * Service métier pour les utilisateurs (vérification QR Code, gestion MFA)
     */
    private final UserService userService;

    /**
     * GET /login
     * Retourne la page de login standard
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * GET /mfa
     * Affiche la page de saisie du code MFA
     * Ajoute l'email de l'utilisateur authentifié dans le modèle pour l'affichage
     */
    @GetMapping("/mfa")
    public String mfa(Model model, @CurrentSecurityContext SecurityContext context) {
        model.addAttribute("email", getAuthenticatedUser(context.getAuthentication()));
        return "mfa";
    }

    /**
     * POST /mfa
     * Valide le code MFA saisi par l'utilisateur
     * - Si le code est correct, l'utilisateur est pleinement authentifié
     * - Sinon, renvoie un échec d'authentification
     */
    @PostMapping("/mfa")
    public void validateCode(@RequestParam("code") String code, HttpServletResponse response, HttpServletRequest request, @CurrentSecurityContext SecurityContext context) throws ServletException, IOException {
        var user = getUser(context.getAuthentication()); // Récupère l'utilisateur courant
        // Vérifie le code MFA via le service
        if (userService.verifyQrCode(user.getUserUuid(), code)) {
            // Code valide → authentification réussie
            this.authenticationSuccessHandler.onAuthenticationSuccess(request, response, getAuthentication(request, response));
            return;
        }
        // Code invalide → déclenche échec MFA
        this.authenticatorFailureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("Le code est invalide. Veuillez réessayer."));
    }

    /**
     * Permet de remplacer temporairement l'authentification MFA par
     * l'authentification principale dans le SecurityContext
     */
    private Authentication getAuthentication(HttpServletRequest request, HttpServletResponse response) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        MfaAuthentication mfaAuthentication = (MfaAuthentication) securityContext.getAuthentication();
        // Remplace la MFA par l'authentification principale
        securityContext.setAuthentication(mfaAuthentication.getPrimaryAuthentication());
        SecurityContextHolder.setContext(securityContext);
        // Sauvegarde le contexte dans la session
        securityContextRepository.saveContext(securityContext, request, response);
        return mfaAuthentication.getPrimaryAuthentication();
    }

    /**
     * GET /error
     * Gestion globale des erreurs
     * - Redirige vers la page login si c'est une erreur d'authentification
     * - Sinon, affiche une page générique d'erreur
     */
    @GetMapping("/error")
    public String handleError(HttpServletRequest request, HttpServletResponse response, Model model, Exception exception) {
        var errorException = (Exception)request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if(errorException instanceof ApiException || errorException instanceof BadCredentialsException) {
            request.getSession().setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, errorException);
            return "login";
        }
        model.addAttribute("message", getMessage(request));
        return "error";
    }

    /**
     * GET /logout
     * Retourne la page de logout
     */
    @GetMapping("/logout")
    public String logout() {
        return "logout";
    }

    /**
     * Récupère l'email de l'utilisateur actuellement authentifié
     */
    private String getAuthenticatedUser(Authentication authentication) {
        return ((User) authentication.getPrincipal()).getEmail();
    }
}