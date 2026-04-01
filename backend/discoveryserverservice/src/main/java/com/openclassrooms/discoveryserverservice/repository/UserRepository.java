package com.openclassrooms.discoveryserverservice.repository;

import com.openclassrooms.discoveryserverservice.model.User;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

public interface UserRepository {
    User getUserByUsername(String username);
}
