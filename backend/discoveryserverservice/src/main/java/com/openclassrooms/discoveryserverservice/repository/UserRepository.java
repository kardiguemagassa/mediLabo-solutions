package com.openclassrooms.discoveryserverservice.repository;

import com.openclassrooms.discoveryserverservice.model.User;

/**
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */

public interface UserRepository {
    User getUserByUsername(String username);
}
