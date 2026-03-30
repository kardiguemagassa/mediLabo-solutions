package com.openclassrooms.authorizationserverservice.query;

/**
 * <p>
 * Classe utilitaire contenant toutes les requêtes SQL et fonctions stockées
 * liées à la gestion des utilisateurs dans l'application.
 * </p>
 *
 * <p>
 * Cette classe centralise :
 * </p>
 * <ul>
 *     <li>Les requêtes de sélection d'un utilisateur par UUID, email ou ID</li>
 *     <li>Les mises à jour des informations utilisateur (profil, mot de passe, rôle, paramètres de compte)</li>
 *     <li>Les requêtes pour la gestion des tokens (account, password)</li>
 *     <li>La gestion des dispositifs et des tentatives de connexion</li>
 *     <li>Les opérations liées à l'authentification multi-facteurs (MFA)</li>
 *     <li>Les requêtes spécifiques pour récupérer les utilisateurs assignés ou les tech supports</li>
 * </ul>
 *
 * <p>
 * Auteur : FirstName LastName<br>
 * Version : 1.0<br>
 * Email : magassa***REMOVED_USER***@gmail.com<br>
 * Since : 2026-05-01
 * </p>
 */

public class UserQuery {

    // USER MANAGEMENT TOKEN QUERY

    /** Requête pour récupérer un utilisateur complet par son UUID
     * */
    public static final String SELECT_USER_BY_USER_ID_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities,  u.qr_code_secret, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.username, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address, c.password, c.updated_at + INTERVAL '90 day' > NOW() AS credentials_non_expired FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id JOIN credentials c ON c.user_id = u.user_id WHERE u.user_uuid = :userUuid
            """;

    /** Requête pour récupérer un utilisateur complet par son email
     * */
    public static final String SELECT_USER_BY_EMAIL_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities,  u.qr_code_secret, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.login_attempts, u.account_non_locked, u.created_at, u.email, u.username, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address, c.password, c.updated_at + INTERVAL '90 day' > NOW() AS credentials_non_expired FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id JOIN credentials c ON c.user_id = u.user_id WHERE u.email = :email
            """;

    /** Requête pour récupérer un utilisateur complet par son ID interne
     * */
    public static final String SELECT_USER_BY_ID_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities,  u.qr_code_secret, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.username, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address, c.password, c.updated_at + INTERVAL '90 day' > NOW() AS credentials_non_expired FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id JOIN credentials c ON c.user_id = u.user_id WHERE u.user_id = :id
            """;

    /** Met à jour le nombre de tentatives de connexion échouées pour un email
     * */
    public static final String UPDATE_LOGIN_ATTEMPTS =
            """
            UPDATE users SET login_attempts = login_attempts + 1 WHERE email = :email
            """;

    /** Réinitialise les tentatives de connexion pour un utilisateur donné
     * */
    public static final String RESET_LOGIN_ATTEMPTS_QUERY =
            """
            UPDATE users SET login_attempts = 0 WHERE user_uuid = :userUuid
            """;

    /** Met à jour la date du dernier login pour un utilisateur
     * */
    public static final String SET_LAST_LOGIN_QUERY =
            """
            UPDATE users SET last_login = NOW() WHERE user_id = :userId
            """;

    /** Insère un nouveau dispositif pour l’utilisateur
     * */
    public static final String INSERT_NEW_DEVICE_QUERY =
            """
            INSERT INTO devices (user_id, device, client, ip_address) VALUES (:userId, :device, :client, :ipAddress)
            """;

    // USER MANAGEMENT QUERY

    /** Récupère un utilisateur par UUID (sans les credentials)
     * */
    public static final String SELECT_USER_BY_USER_UUID_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.username, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id WHERE u.user_uuid = :userUuid
            """;

    /** Met à jour le profil d’un utilisateur via une fonction stockée
     * */
    public static final String UPDATE_USER_FUNCTION =
            """
            SELECT * FROM update_user_profile(:userUuid, :firstName, :lastName, :email, :phone, :bio, :address)
            """;

    /** Crée un nouvel utilisateur via une procédure stockée
     * */
    public static final String CREATE_USER_STORED_PROCEDURE =
            """
            CALL create_user(:userUuid, :firstName, :lastName, :email, :username, :password, :credentialUuid, :token, :memberId, :roleName)
            """;

    /** Sélectionne un token de compte spécifique
     * */
    public static final String SELECT_ACCOUNT_TOKEN_QUERY =
            """
            SELECT account_token_id, token, user_id, (created_at + '24 HOURS') < NOW() AS expired, created_at, updated_at FROM account_tokens WHERE token = :token
            """;

    /** Sélectionne un token de mot de passe spécifique
     * */
    public static final String SELECT_PASSWORD_TOKEN_QUERY =
            """
            SELECT password_token_id, token, user_id, (created_at + '24 HOURS') < NOW() AS expired, created_at, updated_at FROM password_tokens WHERE token = :token
            """;

    /** Supprime un token de compte
     * */
    public static final String DELETE_ACCOUNT_TOKEN_QUERY =
            """
            DELETE FROM account_tokens WHERE token = :token
            """;

    /** Sélectionne un token de mot de passe par ID utilisateur
     * */
    public static final String SELECT_PASSWORD_TOKEN_BY_USER_ID_QUERY =
            """
            SELECT password_token_id, token, user_id, (created_at + '24 HOURS') < NOW() AS expired, created_at, updated_at FROM password_tokens WHERE user_id = :userId
            """;

    /** Supprime un token de mot de passe pour un utilisateur
     * */
    public static final String DELETE_PASSWORD_TOKEN_QUERY =
            """
            DELETE FROM password_tokens WHERE user_id = :userId
            """;

    /** Active les paramètres de compte d’un utilisateur
     * */
    public static final String UPDATE_ACCOUNT_SETTINGS_QUERY =
            """
            UPDATE users SET enabled = TRUE, account_non_expired = TRUE, account_non_locked = TRUE WHERE user_id = :userId
            """;

    /** Active MFA pour un utilisateur
     * */
    public static final String ENABLE_USER_MFA_FUNCTION =
            """
            SELECT * FROM enable_user_mfa(:userUuid, :qrCodeSecret, :qrCodeImageUri)
            """;

    /** Désactive MFA pour un utilisateur
     * */
    public static final String DISABLE_USER_MFA_FUNCTION =
            """
            SELECT * FROM disable_user_mfa(:userUuid)
            """;

    /** Met à jour l’URL de l'image de profil de l’utilisateur
     * */
    public static final String UPDATE_USER_IMAGE_URL_QUERY =
            """
            UPDATE users SET image_url = :imageUrl WHERE user_uuid = :userUuid
            """;

    /** Bascule le statut du compte expiré pour un utilisateur
     * */
    public static final String TOGGLE_ACCOUNT_EXPIRED_FUNCTION =
            """
            SELECT * FROM toggle_account_expired(:userUuid)
            """;

    /** Bascule le statut du compte verrouillé pour un utilisateur
     * */
    public static final String TOGGLE_ACCOUNT_LOCKED_FUNCTION =
            """
            SELECT * FROM toggle_account_locked(:userUuid)
            """;

    /** Bascule le statut du compte activé pour un utilisateur
     * */
    public static final String TOGGLE_ACCOUNT_ENABLED_FUNCTION =
            """
            SELECT * FROM toggle_account_enabled(:userUuid)
            """;

    /** Met à jour le mot de passe d’un utilisateur
     * */
    public static final String UPDATE_USER_PASSWORD_QUERY =
            """
            UPDATE credentials SET password = :password WHERE user_id = (SELECT user_id FROM users WHERE user_uuid = :userUuid)
            """;

    /** Sélectionne le mot de passe d’un utilisateur
     * */
    public static final String SELECT_USER_PASSWORD_QUERY =
            """
            SELECT c.password FROM credentials c JOIN users u ON c.user_id = u.user_id WHERE u.user_uuid = :userUuid
            """;

    /** Met à jour le rôle d’un utilisateur via une fonction stockée
     * */
    public static final String UPDATE_USER_ROLE_FUNCTION =
            """
            SELECT * FROM update_user_role(:userUuid, :role)
            """;

    /** Récupère la liste des utilisateurs (limité à 100)
     * */
    public static final String SELECT_USERS_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.username, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id LIMIT 100
            """;

    /** Récupère l’assigné d’un patient
     * */
    public static final String SELECT_PATIENT_ASSIGNEE_QUERY =
            """
            SELECT u.user_id, u.user_uuid, u.first_name, u.last_name, u.email, u.image_url FROM users u JOIN patients p ON u.user_id = t.assignee_id WHERE p.patient_uuid = :patientUuid
            """;

    /** Récupère les informations de credentials d’un utilisateur
     * */
    public static final String SELECT_USER_CREDENTIAL_QUERY =
            """
            SELECT c.credential_id, c.credential_uuid, c.password, c.created_at, c.updated_at FROM credentials c JOIN users u ON c.user_id = u.user_id WHERE u.user_uuid = :userUuid
            """;

    /** Récupère les dispositifs récents d’un utilisateur
     * */
    public static final String SELECT_DEVICES_QUERY =
            """
            SELECT * FROM devices WHERE user_id = (SELECT user_id FROM users WHERE user_uuid = :userUuid) ORDER BY created_at DESC LIMIT 15
            """;

    /** Crée un token de réinitialisation de mot de passe pour un utilisateur
     * */
    public static final String CREATE_PASSWORD_TOKEN_QUERY =
            """
            INSERT INTO password_tokens (user_id, token) VALUES (:userId, :token)
            """;

    /** Récupère la liste des tech supports
     * */
    public static final String SELECT_TECH_SUPPORTS_QUERY =
            """
            SELECT u.user_uuid, u.first_name, u.last_name, u.image_url, u.email FROM users u JOIN user_roles ur ON u.user_id = ur.user_id JOIN roles r ON ur.role_id = r.role_id WHERE r.name = 'TECH_SUPPORT'
            """;

    /** Récupère l'utilisateur associé à un patient
     * */
    public static final String SELECT_PATIENT_USER_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id WHERE u.user_id = (SELECT user_id FROM patients WHERE patient_uuid = :patientUuid)
            """;
}