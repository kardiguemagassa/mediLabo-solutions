package com.openclassrooms.authorizationserverservice.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.stereotype.Component;

/**
 * UserAuthenticationManager
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Component
@AllArgsConstructor
public class UserAuthenticationManager {
    private final UserAuthenticationProvider userAuthenticationProvider;

    @Bean
    public AuthenticationManager authenticationManager () {
        return new ProviderManager(userAuthenticationProvider);
    }
}
