package com.openclassrooms.gatewayserverservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Configuration de l'encodeur de mot de passe.
 * Note: BCrypt n'a pas besoin d'être réactif car c'est une opération CPU-bound pure.
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Configuration
public class PasswordEncoderConfig {

    /**
     * Force de l'algorithme BCrypt (12 = bon compromis sécurité/performance).
     */
    public static final int STRENGTH = 12;

    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder(STRENGTH);
    }
}
