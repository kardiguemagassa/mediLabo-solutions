package com.openclassrooms.userservice.event;

import com.openclassrooms.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import lombok.AllArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;

import static com.openclassrooms.userservice.util.UserUtils.getUser;

/**
 * <p>
 * Listener des événements d'authentification Spring Security.
 * </p>
 *
 * <p>
 * Cette classe intercepte les événements publiés par Spring Security lors des
 * tentatives d’authentification afin de centraliser la gestion de la sécurité,
 * de la traçabilité et de la protection contre les attaques.
 * </p>
 *
 * <p>
 * Elle permet notamment de :
 * </p>
 * <ul>
 *   <li>Enregistrer la date de dernière connexion de l'utilisateur</li>
 *   <li>Réinitialiser les tentatives de connexion après un succès</li>
 *   <li>Mettre à jour le nombre d’échecs de connexion</li>
 *   <li>Identifier l’appareil, le navigateur et l’adresse IP utilisés</li>
 *   <li>Renforcer la sécurité (détection de fraude, MFA, blocage de compte)</li>
 * </ul>
 *
 * <p>
 * Les informations extraites de {@link jakarta.servlet.http.HttpServletRequest}
 * permettent d’identifier précisément l’environnement de connexion
 * (client, appareil et adresse IP).
 * </p>
 *
 * <p>
 * Ce composant joue un rôle clé dans la sécurité globale du système
 * d’authentification.
 * </p>
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-05-01
 */

@Slf4j
@Component
@AllArgsConstructor
public class ApiAuthenticationEventListener {
    private final UserService userService;
    private final HttpServletRequest request;

    /**
     * <p>
     * Traite un événement d'authentification réussie.
     * </p>
     *
     * <p>
     * Cette méthode est déclenchée automatiquement par Spring Security
     * lorsqu’un utilisateur s’authentifie avec succès.
     * </p>
     *
     * <p>
     * Elle permet de :
     * </p>
     * <ul>
     *   <li>Mettre à jour la date de dernière connexion de l'utilisateur</li>
     *   <li>Réinitialiser le compteur de tentatives de connexion</li>
     *   <li>Enregistrer l’appareil, le client et l’adresse IP utilisés</li>
     * </ul>
     *
     * <p>
     * Ces informations sont essentielles pour :
     * </p>
     * <ul>
     *   <li>La traçabilité des connexions</li>
     *   <li>La détection d’activités suspectes</li>
     *   <li>Le renforcement de la sécurité (MFA, alertes, audit)</li>
     * </ul>
     *
     * @param event événement Spring Security représentant une authentification réussie
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        log.info("AuthenticationSuccess - {}", event);
        if (event.getAuthentication().getPrincipal() instanceof UsernamePasswordAuthenticationToken) {
            var user = getUser(event.getAuthentication());
            userService.setLastLogin(user.getUserId());
            userService.resetLoginAttempts(user.getUserUuid());
            userService.addLoginDevice(user.getUserId(), "Unknown", "Unknown", request.getRemoteAddr());
        }
    }

    /**
     * <p>
     * Traite un événement d’échec d’authentification.
     * </p>
     *
     * <p>
     * Cette méthode est appelée lorsqu'une tentative de connexion échoue,
     * par exemple à cause d'un mot de passe incorrect.
     * </p>
     *
     * <p>
     *  Elle permet de :
     * </p>
     * <ul>
     *   <li>Incrémenter le nombre de tentatives de connexion échouées</li>
     *   <li>Détecter les attaques par force brute</li>
     *   <li>Déclencher des mécanismes de sécurité (blocage, CAPTCHA, MFA)</li>
     * </ul>
     *
     * <p>
     * Seuls les échecs liés à {@link org.springframework.security.authentication.BadCredentialsException}
     * sont pris en compte pour la gestion des tentatives.
     * </p>
     *
     * @param event événement Spring Security représentant un échec d’authentification
     */
    // Cette fonction ne sera pas déclenchée car il faut gérer explicitement l'événement d'échec d'authentification dans la classe UserAuthenticationProvider.java. J'utilise donc une condition else dans cette classe.
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        log.info("AuthenticationFailure - {}", event);
        if (event.getException() instanceof BadCredentialsException) {
            var email = (String) event.getAuthentication().getPrincipal();
            userService.updateLoginAttempts(email);
        }
    }
}
