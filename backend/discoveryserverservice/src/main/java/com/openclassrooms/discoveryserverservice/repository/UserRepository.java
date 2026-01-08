package com.openclassrooms.discoveryserverservice.repository;

import com.openclassrooms.discoveryserverservice.model.User;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

public interface UserRepository {
    User getUserByUsername(String username);
}
