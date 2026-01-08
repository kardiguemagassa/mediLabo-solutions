package com.openclassrooms.authorizationserverservice.repository;

import com.openclassrooms.authorizationserverservice.model.User;

/**
 * Repository pour les opérations utilisateur (authentification)
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

public interface UserRepository {
    User getUserByUuid(String userId);
    User getUserByEmail(String email);
    void resetLoginAttempts(String userUuid);
    void updateLoginAttempts(String email);
    void setLastLogin(Long userId);
    void addLoginDevice(Long userId, String device, String client, String ipAddress);
}
