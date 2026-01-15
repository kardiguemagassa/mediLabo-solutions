package com.openclassrooms.authorizationserverservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Configuration du mécanisme de hachage des mots de passe.
 *
 * <p>
 * Cette classe fournit un {@link BCryptPasswordEncoder} utilisé par Spring Security
 * pour :
 * <ul>
 *     <li>Encoder les mots de passe lors de l’inscription</li>
 *     <li>Vérifier les mots de passe lors de l’authentification</li>
 * </ul>
 * </p>
 *
 * <p>
 * BCrypt est un algorithme de hachage adaptatif conçu pour résister aux attaques
 * par force brute et par GPU grâce à un coût de calcul configurable.
 * </p>
 *
 * <p>
 * Le paramètre {@code STRENGTH} définit le nombre d’itérations (2^STRENGTH),
 * ce qui permet d’augmenter la difficulté de cassage des mots de passe.
 * </p>
 *
 * <h2>Sécurité</h2>
 * <p>
 * Utiliser BCrypt garantit que :
 * <ul>
 *     <li>Les mots de passe ne sont jamais stockés en clair</li>
 *     <li>Deux mots de passe identiques n’ont jamais le même hash</li>
 *     <li>Les attaques par rainbow tables sont inutiles</li>
 * </ul>
 * </p>
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Configuration
public class PasswordEncoderConfig {
    private static final int STRENGTH = 12;

    /**
     * Fournit l’encodeur de mots de passe utilisé par Spring Security.
     *
     * <p>
     * Le {@link BCryptPasswordEncoder} est automatiquement injecté dans
     * les composants d’authentification (AuthenticationProvider, UserDetailsService, etc.)
     * pour vérifier les mots de passe.
     * </p>
     *
     * @return un encodeur BCrypt configuré avec un facteur de coût élevé
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(STRENGTH);
    }
}