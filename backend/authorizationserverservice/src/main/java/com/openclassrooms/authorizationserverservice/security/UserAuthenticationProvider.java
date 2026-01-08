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
 * Provider d'authentification personnalisé
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

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

    @Override
    public boolean supports(Class<?> authenticationType) {
        return authenticationType.isAssignableFrom(UsernamePasswordAuthenticationToken.class);
    }

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
