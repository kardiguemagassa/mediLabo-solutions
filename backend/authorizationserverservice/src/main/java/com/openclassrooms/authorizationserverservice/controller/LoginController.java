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

 * Cette classe gère plusieurs aspects de la sécurité et de l'expérience utilisateur lors
 * de la connexion :

 * Affichage de la page de login standard (/login)
 * Multi-Factor Authentication (MFA) avec validation du code (/mfa)
 * Gestion des erreurs liées à l'authentification (/error)
 * Page de logout (/logout)
 * Fonctionnalités principales :
 * Utilisation de {@link SecurityContextRepository} pour gérer le contexte de sécurité dans la session HTTP
 * Handlers personnalisés pour le succès et l'échec d'authentification MFA

 * {@link AuthenticationSuccessHandler} : authentification réussie
 *  {@link AuthenticationFailureHandler} : gestion des erreurs MFA
 * Validation du code MFA via le {@link UserService} qui encapsule la logique métier
 * Remplacement temporaire de l'authentification MFA par l'authentification principale pour finaliser la session

 * Gestion des erreurs :
 * Redirection vers la page login si l'erreur provient d'une authentification invalide (ex: {@link ApiException}, {@link BadCredentialsException})
 *Affichage d'une page d'erreur générique pour les autres exceptions

 * Auteur : Kardigué MAGASSA
 * Version : 1.0
 * Date : 2026-05-01
 */

@Controller
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final AuthenticationFailureHandler authenticatorFailureHandler = new SimpleUrlAuthenticationFailureHandler("/mfa?error");
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/mfa")
    public String mfa(Model model, @CurrentSecurityContext SecurityContext context) {
        model.addAttribute("email", getAuthenticatedUser(context.getAuthentication()));
        return "mfa";
    }

    @PostMapping("/mfa")
    public void validateCode(@RequestParam("code") String code, HttpServletResponse response, HttpServletRequest request, @CurrentSecurityContext SecurityContext context) throws ServletException, IOException {
        var user = getUser(context.getAuthentication());
        if (userService.verifyQrCode(user.getUserUuid(), code)) {
            this.authenticationSuccessHandler.onAuthenticationSuccess(request, response, getAuthentication(request, response));
            return;
        }
        this.authenticatorFailureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("Le code est invalide. Veuillez réessayer."));
    }

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

    @GetMapping("/logout")
    public String logout() {
        return "logout";
    }

    private String getAuthenticatedUser(Authentication authentication) {
        return ((User) authentication.getPrincipal()).getEmail();
    }
}