package com.openclassrooms.authorizationserverservice.security;

import com.openclassrooms.authorizationserverservice.model.User;
import com.openclassrooms.authorizationserverservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated;
import static org.springframework.security.core.authority.AuthorityUtils.commaSeparatedStringToAuthorityList;

/**
 * Implémentation personnalisée de {@link AuthenticationProvider} chargée
 * de l’authentification des utilisateurs via email et mot de passe.
 *
 * <p>
 * Cette classe constitue le cœur du processus d’authentification utilisateur
 * dans l’Authorization Server. Elle valide :
 * </p>
 * <ul>
 *     <li>l’existence de l’utilisateur</li>
 *     <li>l’état du compte (verrouillé, expiré, désactivé)</li>
 *     <li>la correspondance du mot de passe</li>
 *     <li>les droits et rôles de l’utilisateur</li>
 * </ul>
 *
 * <p>
 * En cas de succès, elle produit un {@link UsernamePasswordAuthenticationToken}
 * authentifié, utilisé ensuite par Spring Security et OAuth2 pour générer
 * les tokens JWT.
 * </p>
 *
 * <p>
 * En cas d’échec, elle déclenche des exceptions spécifiques
 * ({@link BadCredentialsException}, {@link LockedException}, {@link DisabledException})
 * permettant à l’UI de comprendre précisément la cause du refus.
 * </p>
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-05-01
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authentifie un utilisateur à partir de son email et de son mot de passe.
     *
     * <p>
     * Le processus est le suivant :
     * </p>
     * <ol>
     *     <li>Recherche de l’utilisateur via {@link UserService}</li>
     *     <li>Vérification de l’état du compte (verrouillé, expiré, désactivé)</li>
     *     <li>Vérification du mot de passe avec {@link PasswordEncoder}</li>
     *     <li>Construction d’un {@link UsernamePasswordAuthenticationToken} authentifié</li>
     * </ol>
     *
     * @param authentication contient l’email (principal) et le mot de passe (credentials)
     * @return une authentification valide si les informations sont correctes
     * @throws AuthenticationException si l’authentification échoue
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
            var user = userService.getUserByEmail((String) authentication.getPrincipal());
            validateUser.accept(user);
            if (passwordEncoder.matches((String) authentication.getCredentials(), user.getPassword())) {
                log.info("Authentication successful for: {}", user.getEmail());
                return authenticated(user, "[PROTECTED]", commaSeparatedStringToAuthorityList(user.getRole() + "," + user.getAuthorities()));
            } else {
                userService.updateLoginAttempts(user.getEmail());
                log.warn("Incorrect password or email for: {}", user.getEmail());
                throw new BadCredentialsException("Email ou mot de passe incorrect. Veuillez réessayer.");
            }
        } catch (BadCredentialsException | LockedException | DisabledException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Authentication error: {}", exception.getMessage());
            throw new BadCredentialsException(exception.getMessage());
        }
    }

    /**
     * Indique à Spring Security quels types d’objets {@link Authentication}
     * ce provider est capable de traiter.
     *
     * <p>
     * Cette méthode permet au moteur d’authentification de Spring Security
     * de savoir si ce {@link AuthenticationProvider} peut gérer un type
     * spécifique d’authentification.
     * </p>
     *
     * <p>
     * Ici, on indique que ce provider gère les authentifications de type
     * {@link UsernamePasswordAuthenticationToken}, c’est-à-dire les connexions
     * basées sur un email (ou username) et un mot de passe.
     * </p>
     *
     * <p>
     * Si cette méthode retourne {@code false}, Spring Security ignore ce provider
     * et passe au provider suivant dans la chaîne.
     * </p>
     *
     * @param authenticationType la classe du token d’authentification à tester
     * @return {@code true} si ce provider peut traiter ce type d’authentification,
     *         {@code false} sinon
     */
    @Override
    public boolean supports(Class<?> authenticationType) {
        return authenticationType.isAssignableFrom(UsernamePasswordAuthenticationToken.class);
    }

    /**
     * Valide l’état de sécurité du compte utilisateur avant toute vérification du mot de passe.
     *
     * <p>
     * Vérifie que :
     * </p>
     * <ul>
     *     <li>le compte n’est pas verrouillé</li>
     *     <li>le nombre de tentatives de connexion n’a pas dépassé la limite</li>
     *     <li>le compte est activé</li>
     *     <li>le compte n’a pas expiré</li>
     * </ul>
     *
     * @throws LockedException si le compte est verrouillé
     * @throws DisabledException si le compte est désactivé ou expiré
     */
    private final Consumer<User> validateUser = user -> {
        if (!user.isAccountNonLocked() || user.getLoginAttempts() >= 5) {
            String message = user.getLoginAttempts() > 0
                    ? String.format("Compte verrouillé après %d tentatives échouées.", user.getLoginAttempts())
                    : "Compte actuellement verrouillé.";
            throw new LockedException(message);
        }
        
        if (!user.isEnabled()) {
            throw new DisabledException("Votre compte est désactivé.");
        }
        
        if (!user.isAccountNonExpired()) {
            throw new DisabledException("Votre compte a expiré. Veuillez contacter l'administrateur.");
        }
    };
}
