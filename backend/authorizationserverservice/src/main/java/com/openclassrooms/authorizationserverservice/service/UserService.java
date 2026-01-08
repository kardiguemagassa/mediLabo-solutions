package com.openclassrooms.authorizationserverservice.service;

import com.openclassrooms.authorizationserverservice.model.User;

/**
 * Service pour l'authentification des utilisateurs
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

public interface UserService {
    User getUserByEmail(String email);
    void resetLoginAttempts(String userUuid);
    void updateLoginAttempts(String email);
    boolean verifyQrCode(String userId, String code);
    void setLastLogin(Long userId);
    void addLoginDevice(Long userId, String deviceName, String client, String ipAddress);
}
