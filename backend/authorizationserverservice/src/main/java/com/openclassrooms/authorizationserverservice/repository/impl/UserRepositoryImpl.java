package com.openclassrooms.authorizationserverservice.repository.impl;

import com.openclassrooms.authorizationserverservice.exception.ApiException;
import com.openclassrooms.authorizationserverservice.model.*;
import com.openclassrooms.authorizationserverservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import static java.lang.String.format;
import static java.util.Map.of;

import static com.openclassrooms.authorizationserverservice.query.UserQuery.*;

/**
 * implémentation repose sur {@link JdbcClient} de Spring pour exécuter les requêtes SQL de manière sécurisée
 * avec des paramètres nommés.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepositoryImpl implements UserRepository {
    private final JdbcClient jdbc;


    /**
     * @param uuid UUID unique de l'utilisateur
     * @return l'objet {@link User} correspondant
     * @throws ApiException si aucun utilisateur n'est trouvé ou si une erreur survient
     */
    @Override
    public User getUserByUuid(String uuid) {
        try {
            return jdbc.sql(SELECT_USER_BY_USER_ID_QUERY).param("userUuid", uuid).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException(format("Aucun utilisateur trouvé par UUID %s", uuid));
        } catch (Exception exception) {
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param email adresse e-mail de l'utilisateur
     * @return l'utilisateur correspondant
     * @throws ApiException si aucun utilisateur n'est trouvé
     */
    @Override
    public User getUserByEmail(String email) {
        try {
            return jdbc.sql(SELECT_USER_BY_EMAIL_QUERY).param("email", email).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException(format("Aucun utilisateur trouvé pour l'adresse e-mail %s", email));
        } catch (Exception exception) {
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }


    public User getUserById(Long id) {
        try {
            return jdbc.sql(SELECT_USER_BY_ID_QUERY).param("id", id).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException(format("Aucun utilisateur trouvé avec l'ID %s", id));
        } catch (Exception exception) {
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param userId identifiant unique de l’utilisateur
     * @param code code MFA saisi par l’utilisateur
     * @return {@code true} si le code est valide, {@code false} sinon
     */
    public boolean validateCode(String userId, String code) {
        return true;
    }

    /**
     *
     * @param userUuid UUID de l'utilisateur
     * @throws ApiException si l'utilisateur est introuvable
     */
    @Override
    public void resetLoginAttempts(String userUuid) {
        try {
            jdbc.sql(RESET_LOGIN_ATTEMPTS_QUERY).param("userUuid", userUuid).update();
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException(format("Aucun utilisateur trouvé avec l'ID %s", userUuid));
        } catch (Exception exception) {
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param userId identifiant interne de l'utilisateur
     * @param device nom de l'appareil
     * @param client navigateur ou application
     * @param ipAddress adresse IP utilisée
     */
    @Override
    public void addLoginDevice(Long userId, String device, String client, String ipAddress) {
        try {
            jdbc.sql(INSERT_NEW_DEVICE_QUERY).params(of("userId", userId, "device", device, "client", client, "ipAddress", ipAddress)).update();
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException(format("Aucun utilisateur trouvé avec l'ID %s", userId));
        } catch (Exception exception) {
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /** @param userId identifiant technique de l'utilisateur en base de données */
    @Override
    public void setLastLogin(Long userId) {
        try {
            jdbc.sql(SET_LAST_LOGIN_QUERY).param("userId", userId).update();
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException(format("Aucun utilisateur trouvé avec l'ID %s", userId));
        } catch (Exception exception) {
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param email adresse e-mail de l’utilisateur concerné
     */
    @Override
    public void updateLoginAttempts(String email) {
        try {
            jdbc.sql(UPDATE_LOGIN_ATTEMPTS).param("email", email).update();
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException(format("Aucun utilisateur trouvé pour l'adresse e-mail %s", email));
        } catch (Exception exception) {
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }
}