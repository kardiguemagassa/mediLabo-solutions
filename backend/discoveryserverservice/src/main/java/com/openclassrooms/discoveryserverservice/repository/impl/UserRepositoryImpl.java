package com.openclassrooms.discoveryserverservice.repository.impl;

import com.openclassrooms.discoveryserverservice.exception.ApiException;
import com.openclassrooms.discoveryserverservice.model.User;
import com.openclassrooms.discoveryserverservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import static com.openclassrooms.discoveryserverservice.query.UserQuery.SELECT_USER_BY_USERNAME_QUERY;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final JdbcClient jdbc;

    @Override
    public User getUserByUsername(String username) {
        try {
            return jdbc.sql(SELECT_USER_BY_USERNAME_QUERY).param("username", username).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException(String.format("Utilisateur non trouvé %s", username));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur est survenue. Veuillez réessayer.");
        }
    }
}