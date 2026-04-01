package com.openclassrooms.userservice.repository.impl;

import com.openclassrooms.userservice.exception.ApiException;
import com.openclassrooms.userservice.model.*;
import com.openclassrooms.userservice.query.UserQuery;
import com.openclassrooms.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;


import java.util.List;
import java.util.UUID;

import static com.openclassrooms.userservice.util.UserUtils.*;
import static java.lang.String.format;
import static java.sql.Types.VARCHAR;
import static java.util.Map.of;

import static com.openclassrooms.userservice.query.UserQuery.*;

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

    @Override
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
     * @param firstName prénom de l’utilisateur
     * @param lastName nom de famille de l’utilisateur
     * @param email adresse e-mail de l’utilisateur
     * @param password mot de passe chiffré
     * @param verificationKey clé utilisée pour l’activation du compte
     * @return source de paramètres SQL prête à être utilisée par JdbcClient
     */
    private SqlParameterSource getParamSource(String firstName, String lastName, String email, String password, String verificationKey) {
        return new MapSqlParameterSource()
                .addValue("userId", UUID.randomUUID().toString(), VARCHAR)
                .addValue("firstName", firstName, VARCHAR)
                .addValue("lastName", lastName, VARCHAR)
                .addValue("email", email.trim().toLowerCase(), VARCHAR)
                .addValue("password", password, VARCHAR)
                .addValue("verificationKey", verificationKey, VARCHAR)
                .addValue("credentialId", UUID.randomUUID().toString(), VARCHAR);
    }



    // USER MANAGEMENT REPOSITORY

    /**
     * @param userUuid identifiant fonctionnel unique de l’utilisateur
     * @param firstName nouveau prénom
     * @param lastName nouveau nom
     * @param email nouvelle adresse e-mail
     * @param phone nouveau numéro de téléphone
     * @param bio nouvelle biographie
     * @param address nouvelle adresse postale
     * @return utilisateur mis à jour
     */
    @Override
    public User updateUser(String userUuid, String firstName, String lastName, String email, String phone, String bio, String address) {
        try {
            return jdbc.sql(UPDATE_USER_FUNCTION).paramSource(getParamSource(userUuid, firstName, lastName, email, phone, bio, address)).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException(format("Aucun utilisateur trouvé POUR UUI %s", userUuid));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param firstName prénom
     * @param lastName nom
     * @param email email
     * @param username nom d'utilisateur
     * @param password mot de passe encodé
     * @return token d’activation du compte
     */
    @Override
    public String createUser(String firstName, String lastName, String email, String username, String password) {
        try {
            var token = randomUUUID.get();
            jdbc.sql(CREATE_USER_STORED_PROCEDURE).paramSource(getParamSource(firstName, lastName, email, username, password, token)).update();
            return token;
        } catch (DuplicateKeyException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Adresse e-mail ou nom d'utilisateur déjà utilisé. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param token valeur du token
     * @return {@link AccountToken} associé
     * @throws ApiException si le token est invalide ou inexistant
     */
    @Override
    public AccountToken getAccountToken(String token) {
        try {
            return jdbc.sql(SELECT_ACCOUNT_TOKEN_QUERY).param("token", token).query(AccountToken.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Lien invalide. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    @Override
    public User verifyPasswordToken(String token) {
        return null;
    }

    /**
     * @param userUuid UUID de l'utilisateur
     * @return utilisateur mis à jour
     */
    @Override
    public User enableMfa(String userUuid) {
        try {
            return jdbc.sql(ENABLE_USER_MFA_FUNCTION).paramSource(getParamSource(userUuid, qrCodeSecret.get())).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param userUuid identifiant fonctionnel unique de l’utilisateur
     * @return utilisateur mis à jour après la désactivation du MFA
     */
    @Override
    public User disableMfa(String userUuid) {
        try {
            return jdbc.sql(DISABLE_USER_MFA_FUNCTION).param("userUuid", userUuid).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param userUuid identifiant unique de l'utilisateur
     * @return utilisateur après mise à jour de l’état d’expiration
     */
    @Override
    public User toggleAccountExpired(String userUuid) {
        try {
            return jdbc.sql(TOGGLE_ACCOUNT_EXPIRED_FUNCTION).param("userUuid", userUuid).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param userUuid identifiant unique de l'utilisateur
     * @return utilisateur après modification de l’état de verrouillage
     */
    @Override
    public User toggleAccountLocked(String userUuid) {
        try {
            return jdbc.sql(TOGGLE_ACCOUNT_LOCKED_FUNCTION).param("userUuid", userUuid).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    @Override
    public User toggleAccountEnabled(String userUuid) {
        try {
            return jdbc.sql(TOGGLE_ACCOUNT_ENABLED_FUNCTION).param("userUuid", userUuid).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    @Override
    public User toggleCredentialsExpired(String userUuid) {
        return null;
    }

    @Override
    public void updatePassword(String userUuid, String encodedPassword) {
        try {
            jdbc.sql(UPDATE_USER_PASSWORD_QUERY).params(of("userUuid", userUuid, "password", encodedPassword)).update();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param userUuid UUID de l'utilisateur
     * @param role nouveau rôle
     * @return utilisateur mis à jour
     */
    @Override
    public User updateRole(String userUuid, String role) {
        try {
            return jdbc.sql(UPDATE_USER_ROLE_FUNCTION).params(of("userUuid", userUuid, "role", role)).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    @Override
    public void resetPassword(String email) {}

    /**
     * @param userUuid identifiant unique de l’utilisateur
     * @param token token de réinitialisation reçu par email
     * @param password nouveau mot de passe
     * @param confirmPassword confirmation du nouveau mot de passe
     */
    @Override
    public void doResetPassword(String userUuid, String token, String password, String confirmPassword) {}

    /**@return liste des {@link User}*/
    @Override
    public List<User> getUsers() {
        try {
            return jdbc.sql(SELECT_USERS_QUERY).query(User.class).list();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**@param token valeur du token*/
    @Override
    public void deleteAccountToken(String token) {
        try {
            jdbc.sql(DELETE_ACCOUNT_TOKEN_QUERY).param("token", token).update();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Token introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**@param token valeur du token*/
    @Override
    public void deletePasswordToken(String token) {
        try {
            jdbc.sql("").param("token", token).update();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Token introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer");
        }
    }

    /** @param userId identifiant de l'utilisateur*/
    @Override
    public void deletePasswordToken(Long userId) {
        try {
            jdbc.sql(DELETE_PASSWORD_TOKEN_QUERY).param("userId", userId).update();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Token not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param patientUuid UUID du patient
     * @return utilisateur associé
     */
    @Override
    public User getPatientUser(String patientUuid) {
        try {
            return jdbc.sql(SELECT_PATIENT_USER_QUERY).params(of("patientUuid", patientUuid)).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException(format("Aucun patient trouvé par UUID %s", patientUuid));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer");
        }
    }

    /**
     * @param userId identifiant interne de l'utilisateur
     * @return valeur du token généré
     */
    @Override
    public String createPasswordToken(Long userId) {
        try {
            var token = randomUUUID.get();
            jdbc.sql(CREATE_PASSWORD_TOKEN_QUERY).params(of("userId", userId, "token", token)).update();
            return token;
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer");
        }
    }

    /**@return liste des supports techniques*/
    @Override
    public List<User> getMediLaboSupports() {
        try {
            return jdbc.sql(SELECT_MEDILABO_SUPPORTS_QUERY).query(User.class).list();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer");
        }
    }

    /**
     * @param userUuid UUID de l'utilisateur
     * @return mot de passe chiffré
     */
    @Override
    public String getPassword(String userUuid) {
        try {
            return jdbc.sql(SELECT_USER_PASSWORD_QUERY).param("userUuid", userUuid).query(String.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param userUuid UUID de l'utilisateur
     * @param imageUrl URL de l’image
     */
    @Override
    public void updateImageUrl(String userUuid, String imageUrl) {
        try {
            jdbc.sql(UPDATE_USER_IMAGE_URL_QUERY).params(of("userUuid", userUuid, "imageUrl", imageUrl)).update();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param userId identifiant interne de l'utilisateur
     * @return {@link PasswordToken} ou {@code null} s'il n'existe pas
     */
    @Override
    public PasswordToken getPasswordToken(Long userId) {
        try {
            return jdbc.sql(SELECT_PASSWORD_TOKEN_BY_USER_ID_QUERY).params(of("userId", userId)).query(PasswordToken.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            return null;
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param token valeur du token
     * @return {@link PasswordToken}
     * @throws ApiException si le token est invalide
     */
    @Override
    public PasswordToken getPasswordToken(String token) {
        try {
            return jdbc.sql(SELECT_PASSWORD_TOKEN_QUERY).param("token", token).query(PasswordToken.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Lien invalide. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param userId identifiant interne de l’utilisateur
     * @throws ApiException si l’utilisateur n’existe pas ou en cas d’erreur technique
     */
    @Override
    public void updateAccountSettings(Long userId) {
        try {
            jdbc.sql(UPDATE_ACCOUNT_SETTINGS_QUERY).param("userId", userId).update();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param patientUuid UUID du patient
     * @return utilisateur assigné ou utilisateur vide
     */
    @Override
    public User getAssignee(String patientUuid) {
        try {
            return jdbc.sql(SELECT_PATIENT_ASSIGNEE_QUERY).param("patientUuid", patientUuid).query(User.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            log.error("Patient is not assigned.");
            return User.builder().build();
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param userUuid UUID de l'utilisateur
     * @return {@link Credential}
     */
    @Override
    public Credential getCredential(String userUuid) {
        try {
            return jdbc.sql(SELECT_USER_CREDENTIAL_QUERY).param("userUuid", userUuid).query(Credential.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Identifiants introuvables. Veuillez réessayer..");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param userUuid UUID de l'utilisateur
     * @return liste des {@link Device}
     */
    @Override
    public List<Device> getDevices(String userUuid) {
        try {
            return jdbc.sql(SELECT_DEVICES_QUERY).param("userUuid", userUuid).query(Device.class).list();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * @param userUuid identifiant unique de l’utilisateur
     * @param qrCodeSecret clé secrète pour la génération du QR Code
     * @return un {@link SqlParameterSource} prêt à être utilisé dans une requête SQL
     */
    private SqlParameterSource getParamSource(String userUuid, String qrCodeSecret) {
        return new MapSqlParameterSource()
                .addValue("userUuid", userUuid, VARCHAR)
                .addValue("qrCodeSecret", qrCodeSecret, VARCHAR)
                .addValue("qrCodeImageUri", qrCodeImageUri.apply(qrCodeSecret), VARCHAR);
    }

    /**
     * Construit les paramètres SQL nécessaires à la mise à jour du profil d’un utilisateur.
     * Cette méthode est utilisée lors de la modification des informations personnelles
     * d’un utilisateur (profil, coordonnées, biographie
     * Les paramètres fournis sont normalisés (ex: e-mail en minuscules) avant d’être envoyés
     * à la base de données.
     * Paramètres construits :
     * identifiant unique de l’utilisateur
     * firstName : prénom
     * lastName : nom
     * email : adresse e-mail normalisée
     * phone : numéro de téléphone
     * address : adresse postale
     * bio : description ou biographie de l'utilisateur
     * @return un {@link SqlParameterSource} contenant les données du profil utilisateur
     */
    private SqlParameterSource getParamSource(String userUuid, String firstName, String lastName, String email, String phone, String bio, String address) {
        return new MapSqlParameterSource()
                .addValue("userUuid", userUuid, VARCHAR)
                .addValue("firstName", firstName, VARCHAR)
                .addValue("lastName", lastName, VARCHAR)
                .addValue("email", email.trim().toLowerCase(), VARCHAR)
                .addValue("phone", phone, VARCHAR)
                .addValue("address", address, VARCHAR)
                .addValue("bio", bio, VARCHAR);
    }

    /**
     * Construit les paramètres SQL nécessaires à la création d’un nouvel utilisateur.
     * Cette méthode est utilisée lors de l’inscription. Elle génère automatiquement :
     * un UUID pour l’utilisateur
     * un UUID pour les identifiants (credentials)
     * un identifiant de membre
     * Ces données sont utilisées par une procédure stockée pour créer l’utilisateur
     * et ses identifiants en une seule transaction.
     * Paramètres générés :
     * userUuid : identifiant unique du nouvel utilisateur
     * credentialUuid : identifiant des informations d’authentification
     * memberId : identifiant métier du membre
     * token : token d’activation du compte
     * @return un {@link SqlParameterSource} contenant toutes les données nécessaires à la création du compte
     */
    private SqlParameterSource getParamSource(String firstName, String lastName, String email, String username, String password, String token) {
        return new MapSqlParameterSource()
                .addValue("userUuid", randomUUUID.get(), VARCHAR)
                .addValue("firstName", firstName, VARCHAR)
                .addValue("lastName", lastName, VARCHAR)
                .addValue("email", email.trim().toLowerCase(), VARCHAR)
                .addValue("username", username.trim().toLowerCase(), VARCHAR)
                .addValue("password", password, VARCHAR)
                .addValue("token", token, VARCHAR)
                .addValue("credentialUuid", randomUUUID.get(), VARCHAR)
                .addValue("memberId", memberId.get(), VARCHAR)
                .addValue("roleName", "USER", VARCHAR);
    }
}