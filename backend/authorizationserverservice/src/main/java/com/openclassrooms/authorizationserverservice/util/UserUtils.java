package com.openclassrooms.authorizationserverservice.util;

import com.openclassrooms.authorizationserverservice.model.User;
import dev.samstevens.totp.code.*;

import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;

/**
 * Utilitaires pour la gestion des utilisateurs, incluant la génération et vérification de QR codes TOTP,
 * TOTP (Time-based One-Time Password, c’est-à-dire mot de passe à usage unique basé sur le temps.)
 * la génération d'UUID et de Member IDs, ainsi que l'extraction d'utilisateurs depuis l'authentification.

 * Fournit des méthodes et lambdas pour :
 * Vérifier les codes TOTP
 * Extraire un utilisateur d'un objet Authentication
 * Générer des identifiants uniques et secrets TOTP
 * Générer des QR codes pour MFA
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

public class UserUtils {

    /**
     * Vérifie qu'un code TOTP correspond au secret de l'utilisateur.
     *
     * @param secret le secret TOTP de l'utilisateur
     * @param code le code saisi par l'utilisateur
     * @return true si le code est valide, false sinon
     */
    public static boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isEmpty()) {
            return false;
        }
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(secret, code);
    }

    /**
     * Extrait un objet {@link User} à partir d'un objet {@link Authentication}.
     * Supporte les authentifications OAuth2 ainsi que les authentifications classiques.
     *
     * @param authentication objet d'authentification Spring Security
     * @return l'utilisateur extrait
     */
    public static User getUser(Authentication authentication) {
        if (authentication instanceof OAuth2AuthorizationCodeRequestAuthenticationToken) {
            var usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) authentication.getPrincipal();
            return (User) usernamePasswordAuthenticationToken.getPrincipal();
        }
        return (User) authentication.getPrincipal();
    }
}
