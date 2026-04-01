package com.openclassrooms.authorizationserverservice.service.impl;

import com.openclassrooms.authorizationserverservice.model.User;
import com.openclassrooms.authorizationserverservice.repository.UserRepository;
import com.openclassrooms.authorizationserverservice.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import static com.openclassrooms.authorizationserverservice.util.UserUtils.verifyCode;

/**
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    /**
     * @param email adresse e-mail de l'utilisateur
     * @return utilisateur correspondant à l'adresse e-mail
     */
    @Override
    public User getUserByEmail(String email) {
        return userRepository.getUserByEmail(email);
    }

    /**
     * Réinitialise le compteur de tentatives de connexion d’un utilisateur.
     * Cette opération est déclenchée après : une authentification réussie un déverrouillage de compte
     * @param userId identifiant unique de l'utilisateur
     */
    @Override
    public void resetLoginAttempts(String userId) {
        userRepository.resetLoginAttempts(userId);
    }

    /**
     * Incrémente le nombre de tentatives de connexion échouées.
     * Cette information est utilisée pour : détecter les attaques par force brute verrouiller automatiquement un compte
     * @param email adresse e-mail utilisée pour la tentative
     */
    @Override
    public void updateLoginAttempts(String email) {
        userRepository.updateLoginAttempts(email);
    }

    /**
     * Met à jour la date de dernière connexion de l’utilisateur.
     * Cette information est utilisée pour : le suivi d’activité les audits de sécurité
     * @param userId identifiant unique de l’utilisateur
     */
    @Override
    public void setLastLogin(Long userId) {
        userRepository.setLastLogin(userId);
    }

    /**
     * Enregistre un appareil utilisé lors d’une connexion.
     * Ces informations permettent :
     * de détecter les connexions suspectes
     * d'afficher l’historique des appareils
     * @param userId identifiant de l’utilisateur
     * @param deviceName nom de l’appareil
     * @param client navigateur ou application
     * @param ipAddress adresse IP utilisée
     */
    @Override
    public void addLoginDevice(Long userId, String deviceName, String client, String ipAddress) {
        userRepository.addLoginDevice(userId, deviceName, client, ipAddress);
    }

    /**
     * Valide un code MFA généré par une application d’authentification.
     * Le code est comparé au secret MFA stocké pour l’utilisateur.
     * @param userId identifiant unique de l’utilisateur
     * @param code code à usage unique fourni par l’utilisateur
     * @return {@code true} si le code est valide, sinon {@code false}
     */
    @Override
    public boolean verifyQrCode(String userId, String code) {
        var user = userRepository.getUserByUuid(userId);
        return verifyCode(user.getQrCodeSecret(), code);
    }

}
