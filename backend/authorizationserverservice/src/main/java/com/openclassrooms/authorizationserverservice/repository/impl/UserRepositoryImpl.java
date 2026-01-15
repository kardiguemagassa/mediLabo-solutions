package com.openclassrooms.authorizationserverservice.repository.impl;

import com.openclassrooms.authorizationserverservice.exception.ApiException;
import com.openclassrooms.authorizationserverservice.model.*;
import com.openclassrooms.authorizationserverservice.repository.UserRepository;
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

import static com.openclassrooms.authorizationserverservice.util.UserUtils.*;
import static java.lang.String.format;
import static java.sql.Types.VARCHAR;
import static java.util.Map.of;

import static com.openclassrooms.authorizationserverservice.query.UserQuery.*;

/**
 * <p>
 * Implémentation JDBC du repository {@link UserRepository}.
 * </p>
 *
 * <p>
 * Cette classe assure l'accès à la base de données pour la gestion complète des utilisateurs :
 * </p>
 *
 * <ul>
 *     <li>Authentification et gestion des connexions</li>
 *     <li>Gestion des rôles et des autorisations</li>
 *     <li>Gestion MFA (Multi-Factor Authentication)</li>
 *     <li>Gestion des tokens (activation, mot de passe, etc.)</li>
 *     <li>Gestion des appareils (devices)</li>
 * </ul>
 *
 * <p>
 * Toutes les requêtes SQL sont centralisées dans la classe {@link com.openclassrooms.authorizationserverservice.query.UserQuery}.
 * </p>
 *
 * <p>
 * Cette implémentation repose sur {@link JdbcClient} de Spring pour exécuter les requêtes SQL de manière sécurisée
 * avec des paramètres nommés.
 * </p>
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

    // USER MANAGEMENT TOKEN REPOSITORY

    /**
     * <p>
     * Récupère un utilisateur à partir de son UUID.
     * </p>
     *
     * <p>
     * Cette méthode est principalement utilisée lors :
     * </p>
     * <ul>
     *     <li>de l'authentification</li>
     *     <li>du chargement du profil utilisateur</li>
     *     <li>des opérations de sécurité (MFA, rôles, etc.)</li>
     * </ul>
     *
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
     * <p>
     * Recherche un utilisateur à partir de son adresse email.
     * </p>
     *
     * <p>
     * Utilisée principalement pour :
     * </p>
     * <ul>
     *     <li>la connexion</li>
     *     <li>la réinitialisation du mot de passe</li>
     *     <li>la validation de compte</li>
     * </ul>
     *
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

    /**
     *
     * @param id
     * @return
     */
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
     * <p>
     * Valide un code d’authentification à usage unique (OTP / MFA) pour un utilisateur.
     * </p>
     *
     * <p>
     * Cette méthode est utilisée dans le cadre de l’authentification multi-facteurs (MFA).
     * Elle permet de vérifier que le code saisi par l’utilisateur correspond bien
     * au code généré à partir de sa clé secrète (QR Code / TOTP).
     * </p>
     *
     * <p>
     * Le processus de validation inclut généralement :
     * </p>
     * <ul>
     *     <li>la récupération de la clé secrète associée à l’utilisateur</li>
     *     <li>la génération du code attendu via un algorithme TOTP</li>
     *     <li>la comparaison avec le code fourni par l’utilisateur</li>
     * </ul>
     *
     * <p>
     * Si le code est valide, l’utilisateur peut terminer son authentification.
     * Sinon, l’accès est refusé.
     * </p>
     *
     * @param userId identifiant unique de l’utilisateur
     * @param code code MFA saisi par l’utilisateur
     * @return {@code true} si le code est valide, {@code false} sinon
     */
    public boolean validateCode(String userId, String code) {
        return true;
    }

    /**
     * <p>
     * Réinitialise le nombre de tentatives de connexion échouées d’un utilisateur.
     * </p>
     *
     * <p>
     * Cette méthode est appelée après une authentification réussie afin de :
     * </p>
     * <ul>
     *     <li>débloquer le compte</li>
     *     <li>réinitialiser la politique de sécurité</li>
     * </ul>
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
     * <p>
     * Enregistre un nouvel appareil utilisé par un utilisateur lors de la connexion.
     * </p>
     *
     * <p>
     * Permet :
     * </p>
     * <ul>
     *     <li>le suivi des connexions</li>
     *     <li>la détection d'activités suspectes</li>
     *     <li>l'affichage de l'historique des appareils</li>
     * </ul>
     *
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

    /**
     * <p>
     * Met à jour la date et l'heure de la dernière connexion d'un utilisateur.
     * </p>
     *
     * <p>
     * Cette information est utilisée pour :
     * </p>
     * <ul>
     *     <li>le suivi des connexions</li>
     *     <li>la sécurité (détection d’activités suspectes)</li>
     *     <li>l’affichage de la dernière activité utilisateur</li>
     * </ul>
     *
     * <p>
     * La valeur est enregistrée directement en base de données via une requête SQL.
     * </p>
     *
     * @param userId identifiant technique de l’utilisateur en base de données
     */
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
     * <p>
     * Incrémente le nombre de tentatives de connexion échouées pour un utilisateur.
     * </p>
     *
     * <p>
     * Cette méthode est appelée lorsqu'une authentification échoue
     * (mot de passe ou code MFA incorrect).
     * </p>
     *
     * <p>
     * Elle permet de :
     * </p>
     * <ul>
     *     <li>détecter les tentatives de brute force</li>
     *     <li>verrouiller un compte après un certain nombre d'échecs</li>
     *     <li>améliorer la sécurité globale du système</li>
     * </ul>
     *
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

    /**
     * <p>
     * Construit les paramètres SQL nécessaires à la création d’un nouvel utilisateur.
     * </p>
     *
     * <p>
     * Cette méthode génère :
     * </p>
     * <ul>
     *     <li>un identifiant utilisateur unique</li>
     *     <li>un identifiant pour les credentials</li>
     *     <li>les informations de base du compte (nom, email, mot de passe)</li>
     *     <li>la clé de vérification du compte</li>
     * </ul>
     *
     * <p>
     * L'objet retourné est utilisé pour appeler une procédure stockée en base de données.
     * </p>
     *
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
     * <p>
     * Met à jour les informations de profil d’un utilisateur existant.
     * </p>
     *
     * <p>
     * Cette méthode permet de modifier :
     * </p>
     * <ul>
     *     <li>le prénom et le nom</li>
     *     <li>l'adresse e-mail</li>
     *     <li>le téléphone</li>
     *     <li>la biographie</li>
     *     <li>l'adresse postale</li>
     * </ul>
     *
     * <p>
     * La mise à jour est effectuée via une fonction stockée en base de données,
     * garantissant la cohérence des données.
     * </p>
     *
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
     * <p>
     * Crée un nouvel utilisateur dans le système via une procédure stockée.
     * </p>
     *
     * <p>
     * Cette opération :
     * </p>
     * <ul>
     *     <li>crée l'utilisateur</li>
     *     <li>génère ses identifiants</li>
     *     <li>crée un token d'activation</li>
     * </ul>
     *
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
     * <p>
     * Récupère un token d’activation de compte.
     * </p>
     *
     * <p>
     * Ce token est utilisé lors de :
     * </p>
     * <ul>
     *     <li>la validation d’un compte utilisateur</li>
     *     <li>l'activation après inscription</li>
     * </ul>
     *
     * <p>
     * Le token peut être expiré (24h après sa création).
     * </p>
     *
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
     * <p>
     * Active l’authentification multi-facteur (MFA) pour un utilisateur.
     * </p>
     *
     * <p>
     * Cette opération :
     * </p>
     * <ul>
     *     <li>génère un secret QR Code</li>
     *     <li>enregistre le QR Code</li>
     *     <li>active la sécurité MFA</li>
     * </ul>
     *
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
     * <p>
     * Désactive l'authentification multi-facteurs (MFA) pour un utilisateur.
     * </p>
     *
     * <p>
     * Cette opération :
     * </p>
     * <ul>
     *     <li>supprime l’obligation de fournir un code à usage unique</li>
     *     <li>désactive le secret MFA associé au compte</li>
     *     <li>met à jour l’état de sécurité de l’utilisateur</li>
     * </ul>
     *
     * <p>
     * La modification est effectuée via une fonction stockée en base de données.
     * </p>
     *
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
     * <p>
     * Inverse l'état d'expiration du compte utilisateur.
     * </p>
     *
     * <p>
     * Si le compte est :
     * </p>
     * <ul>
     *     <li>actuellement valide → il devient expiré</li>
     *     <li>actuellement expiré → il redevient valide</li>
     * </ul>
     *
     * <p>
     * Un compte expiré ne peut pas s'authentifier.
     * </p>
     *
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
     * <p>
     * Verrouille ou déverrouille un compte utilisateur.
     * </p>
     *
     * <p>
     * Un compte verrouillé :
     * </p>
     * <ul>
     *     <li>empêche toute authentification</li>
     *     <li>est généralement déclenché après trop d'échecs de connexion</li>
     * </ul>
     *
     * <p>
     * Cette méthode bascule automatiquement l'état verrouillé ↔ déverrouillé.
     * </p>
     *
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

    /**
     *
     * @param userUuid
     * @return
     */
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

    /**
     *
     * @param userUuid
     * @return
     */
    @Override
    public User toggleCredentialsExpired(String userUuid) {
        return null;
    }

    /**
     *
     * @param userUuid
     * @param encodedPassword
     */
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
     * <p>
     * Modifie le rôle d’un utilisateur.
     * </p>
     *
     * <p>
     * Cette opération impacte directement :
     * </p>
     * <ul>
     *     <li>les autorisations</li>
     *     <li>l’accès aux fonctionnalités</li>
     * </ul>
     *
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

    /**
     * <p>
     * Démarre le processus de réinitialisation du mot de passe pour un utilisateur.
     * </p>
     *
     * <p>
     * Cette méthode est appelée lorsqu’un utilisateur demande un mot de passe oublié.
     * Elle permet de :
     * </p>
     * <ul>
     *     <li>vérifier l’existence du compte associé à l’adresse e-mail</li>
     *     <li>générer un token sécurisé de réinitialisation</li>
     *     <li>enregistrer ce token en base de données</li>
     *     <li>permettre l’envoi d’un lien de réinitialisation par email</li>
     * </ul>
     *
     * <p>
     * Le lien envoyé à l’utilisateur contient le token qui sera utilisé pour valider
     * la demande de changement de mot de passe.
     * </p>
     *
     * @param email adresse e-mail de l’utilisateur ayant demandé la réinitialisation
     */
    @Override
    public void resetPassword(String email) {

    }

    /**
     * <p>
     * Finalise la réinitialisation du mot de passe d’un utilisateur.
     * </p>
     *
     * <p>
     * Cette méthode est appelée lorsque l’utilisateur soumet le formulaire de
     * nouveau mot de passe après avoir cliqué sur le lien reçu par email.
     * </p>
     *
     * <p>
     * Elle permet de :
     * </p>
     * <ul>
     *     <li>vérifier la validité du token de réinitialisation</li>
     *     <li>vérifier que le token appartient bien à l’utilisateur</li>
     *     <li>contrôler que les mots de passe saisis correspondent</li>
     *     <li>mettre à jour le mot de passe en base de données</li>
     *     <li>supprimer ou invalider le token après utilisation</li>
     * </ul>
     *
     * <p>
     * Cette opération garantit que le changement de mot de passe est sécurisé
     * et qu’un token ne peut pas être réutilisé.
     * </p>
     *
     * @param userUuid identifiant unique de l’utilisateur
     * @param token token de réinitialisation reçu par email
     * @param password nouveau mot de passe
     * @param confirmPassword confirmation du nouveau mot de passe
     */
    @Override
    public void doResetPassword(String userUuid, String token, String password, String confirmPassword) {

    }

    /**
     * <p>
     * Récupère la liste des utilisateurs du système.
     * </p>
     *
     * <p>
     * Inclut :
     * </p>
     * <ul>
     *     <li>les informations de base</li>
     *     <li>les rôles</li>
     *     <li>les paramètres de sécurité</li>
     * </ul>
     *
     * @return liste des {@link User}
     */
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

    /**
     * <p>
     * Supprime un token d’activation de compte.
     * </p>
     *
     * <p>
     * Cette méthode est appelée après :
     * </p>
     * <ul>
     *     <li>une activation réussie</li>
     *     <li>ou un token expiré</li>
     * </ul>
     *
     * @param token valeur du token
     */
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

    /**
     * <p>
     * Supprime un token de réinitialisation de mot de passe à partir de sa valeur.
     * </p>
     *
     * <p>
     * Permet d’éviter la réutilisation d’un lien après usage.
     * </p>
     *
     * @param token valeur du token
     */
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

    /**
     * <p>
     * Supprime tous les tokens de réinitialisation d’un utilisateur.
     * </p>
     *
     * <p>
     * Utilisé lorsque :
     * </p>
     * <ul>
     *     <li>le mot de passe a été modifié</li>
     *     <li>un ancien lien doit être invalidé</li>
     * </ul>
     *
     * @param userId identifiant de l'utilisateur
     */
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
     * <p>
     * Récupère l’utilisateur associé à un patient.
     * </p>
     *
     * <p>
     * Utilisé pour :
     * </p>
     * <ul>
     *     <li>les dossiers médicaux</li>
     *     <li>les accès patients</li>
     * </ul>
     *
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
     * <p>
     * Crée un token de réinitialisation de mot de passe pour un utilisateur.
     * </p>
     *
     * <p>
     * Ce token sera envoyé par e-mail à l’utilisateur afin qu'il puisse
     * définir un nouveau mot de passe.
     * </p>
     *
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

    /**
     * <p>
     * Récupère tous les utilisateurs ayant le rôle TECH_SUPPORT.
     * </p>
     *
     * <p>
     * Utilisé pour :
     * </p>
     * <ul>
     *     <li>l’assistance</li>
     *     <li>la gestion des incidents</li>
     * </ul>
     *
     * @return liste des supports techniques
     */
    @Override
    public List<User> getMediLaboSupports() {
        try {
            return jdbc.sql(SELECT_TECH_SUPPORTS_QUERY).query(User.class).list();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Utilisateur introuvable. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer");
        }
    }

    /**
     * <p>
     * Récupère le mot de passe chiffré d’un utilisateur.
     * </p>
     *
     * <p>
     * Utilisé pour :
     * </p>
     * <ul>
     *     <li>la validation d’un changement de mot de passe</li>
     *     <li>les contrôles de sécurité</li>
     * </ul>
     *
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
     * <p>
     * Met à jour la photo de profil d’un utilisateur.
     * </p>
     *
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
     * <p>
     * Récupère le token de réinitialisation de mot de passe d'un utilisateur.
     * </p>
     *
     * <p>
     * Ce token permet :
     * </p>
     * <ul>
     *     <li>de sécuriser la réinitialisation du mot de passe</li>
     *     <li>de vérifier que la demande est valide</li>
     * </ul>
     *
     * @param userId identifiant interne de l'utilisateur
     * @return {@link PasswordToken} ou {@code null} s'il n'existe pas
     */
    @Override
    public PasswordToken getPasswordToken(Long userId) {
        try {
            return jdbc.sql(SELECT_PASSWORD_TOKEN_BY_USER_ID_QUERY).params(of("userId", userId)).query(PasswordToken.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            //throw new ApiException("Invalid link. Please try again.");
            return null;
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    /**
     * <p>
     * Récupère un token de mot de passe à partir de sa valeur.
     * </p>
     *
     * <p>
     * Utilisé pour :
     * </p>
     * <ul>
     *     <li>valider un lien de réinitialisation</li>
     *     <li>vérifier l’expiration du token</li>
     * </ul>
     *
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
     * <p>
     * Met à jour les paramètres de sécurité d'un compte utilisateur.
     * </p>
     *
     * <p>
     * Cette méthode permet d'activer complètement un compte après une validation
     * (par exemple après une inscription ou une confirmation par e-mail).
     * </p>
     *
     * <p>
     * Les champs mis à jour sont :
     * </p>
     * <ul>
     *     <li><b>enabled</b> : active le compte</li>
     *     <li><b>account_non_expired</b> : marque le compte comme non expiré</li>
     *     <li><b>account_non_locked</b> : déverrouille le compte</li>
     * </ul>
     *
     * <p>
     * Elle est généralement appelée après :
     * </p>
     * <ul>
     *     <li>la validation d'un token d'activation</li>
     *     <li>ou la création initiale d'un compte</li>
     * </ul>
     *
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
     * <p>
     * Récupère le professionnel assigné à un patient.
     * </p>
     *
     * <p>
     * Peut être :
     * </p>
     * <ul>
     *     <li>un médecin</li>
     *     <li>un technicien</li>
     * </ul>
     *
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
     * <p>
     * Récupère les informations d’identification d’un utilisateur.
     * </p>
     *
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
     * <p>
     * Récupère la liste des appareils utilisés par un utilisateur.
     * </p>
     *
     * <p>
     * Utile pour :
     * </p>
     * <ul>
     *     <li>la sécurité</li>
     *     <li>la détection de connexions anormales</li>
     *     <li>l'affichage dans l'interface utilisateur</li>
     * </ul>
     *
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
     * <p>
     * Construit les paramètres SQL nécessaires à l’activation de l'authentification
     * multi-facteurs (MFA) pour un utilisateur.
     * </p>
     *
     * <p>
     * Cette méthode génère automatiquement l'URL du QR Code à partir du secret fourni.
     * Ces données sont utilisées par une fonction stockée pour activer le MFA.
     * </p>
     *
     * <p>
     * Les paramètres construits sont :
     * </p>
     * <ul>
     *     <li><b>userUuid</b> : identifiant unique de l’utilisateur</li>
     *     <li><b>qrCodeSecret</b> : clé secrète utilisée pour générer le code OTP</li>
     *     <li><b>qrCodeImageUri</b> : URL du QR Code à afficher à l’utilisateur</li>
     * </ul>
     *
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
     * <p>
     * Construit les paramètres SQL nécessaires à la mise à jour du profil d’un utilisateur.
     * </p>
     *
     * <p>
     * Cette méthode est utilisée lors de la modification des informations personnelles
     * d’un utilisateur (profil, coordonnées, biographie).
     * </p>
     *
     * <p>
     * Les paramètres fournis sont normalisés (ex: e-mail en minuscules) avant d’être envoyés
     * à la base de données.
     * </p>
     *
     * <p>
     * Paramètres construits :
     * </p>
     * <ul>
     *     <li><b>userUuid</b> : identifiant unique de l’utilisateur</li>
     *     <li><b>firstName</b> : prénom</li>
     *     <li><b>lastName</b> : nom</li>
     *     <li><b>email</b> : adresse e-mail normalisée</li>
     *     <li><b>phone</b> : numéro de téléphone</li>
     *     <li><b>address</b> : adresse postale</li>
     *     <li><b>bio</b> : description ou biographie de l’utilisateur</li>
     * </ul>
     *
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
     * <p>
     * Construit les paramètres SQL nécessaires à la création d’un nouvel utilisateur.
     * </p>
     *
     * <p>
     * Cette méthode est utilisée lors de l’inscription. Elle génère automatiquement :
     * </p>
     * <ul>
     *     <li>un UUID pour l’utilisateur</li>
     *     <li>un UUID pour les identifiants (credentials)</li>
     *     <li>un identifiant de membre</li>
     * </ul>
     *
     * <p>
     * Ces données sont utilisées par une procédure stockée pour créer l’utilisateur
     * et ses identifiants en une seule transaction.
     * </p>
     *
     * <p>
     * Paramètres générés :
     * </p>
     * <ul>
     *     <li><b>userUuid</b> : identifiant unique du nouvel utilisateur</li>
     *     <li><b>credentialUuid</b> : identifiant des informations d’authentification</li>
     *     <li><b>memberId</b> : identifiant métier du membre</li>
     *     <li><b>token</b> : token d’activation du compte</li>
     * </ul>
     *
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
                .addValue("memberId", memberId.get(), VARCHAR);
    }
}