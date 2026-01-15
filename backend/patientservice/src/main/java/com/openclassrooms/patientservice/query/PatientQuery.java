package com.openclassrooms.patientservice.query;

/**
 * Modèle User pour l'authentification
 * POJO simple mappé depuis SQL (pas d'entité JPA)
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */

public class UserQuery {
    public static final String SELECT_USER_BY_USER_UUID_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.username, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id WHERE u.user_uuid = :userUuid
            """;
    public static final String SELECT_USER_BY_USER_ID_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.username, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id WHERE u.user_id = :userId
            """;
    public static final String SELECT_USER_BY_EMAIL_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.username, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id WHERE u.email = :email
            """;
    public static final String UPDATE_USER_FUNCTION =
            """
            SELECT * FROM update_user_profile(:userUuid, :firstName, :lastName, :email, :phone, :bio, :address)
            """;
    public static final String CREATE_USER_STORED_PROCEDURE =
            """
            CALL create_user(:userUuid, :firstName, :lastName, :email, :username, :password, :credentialUuid, :token, :memberId)
            """;
    public static final String SELECT_ACCOUNT_TOKEN_QUERY =
            """
            SELECT account_token_id, token, user_id, (created_at + '24 HOURS') < NOW() AS expired, created_at, updated_at FROM account_tokens WHERE token = :token
            """;
    public static final String SELECT_PASSWORD_TOKEN_QUERY =
            """
            SELECT password_token_id, token, user_id, (created_at + '24 HOURS') < NOW() AS expired, created_at, updated_at FROM password_tokens WHERE token = :token
            """;
    public static final String DELETE_ACCOUNT_TOKEN_QUERY =
            """
            DELETE FROM account_tokens WHERE token = :token
            """;
    public static final String SELECT_PASSWORD_TOKEN_BY_USER_ID_QUERY =
            """
            SELECT password_token_id, token, user_id, (created_at + '24 HOURS') < NOW() AS expired, created_at, updated_at FROM password_tokens WHERE user_id = :userId
            """;
    public static final String DELETE_PASSWORD_TOKEN_QUERY =
            """
            DELETE FROM password_tokens WHERE user_id = :userId
            """;
    public static final String UPDATE_ACCOUNT_SETTINGS_QUERY =
            """
            UPDATE users SET enabled = TRUE, account_non_expired = TRUE, account_non_locked = TRUE WHERE user_id = :userId
            """;
    public static final String ENABLE_USER_MFA_FUNCTION =
            """
            SELECT * FROM enable_user_mfa(:userUuid, :qrCodeSecret, :qrCodeImageUri)
            """;
    public static final String DISABLE_USER_MFA_FUNCTION =
            """
            SELECT * FROM disable_user_mfa(:userUuid)
            """;
    public static final String UPDATE_USER_IMAGE_URL_QUERY =
            """
            UPDATE users SET image_url = :imageUrl WHERE user_uuid = :userUuid
            """;
    public static final String TOGGLE_ACCOUNT_EXPIRED_FUNCTION =
            """
            SELECT * FROM toggle_account_expired(:userUuid)
            """;
    public static final String TOGGLE_ACCOUNT_LOCKED_FUNCTION =
            """
            SELECT * FROM toggle_account_locked(:userUuid)
            """;
    public static final String TOGGLE_ACCOUNT_ENABLED_FUNCTION =
            """
            SELECT * FROM toggle_account_enabled(:userUuid)
            """;
    public static final String UPDATE_USER_PASSWORD_QUERY =
            """
            UPDATE credentials SET password = :password WHERE user_id = (SELECT user_id FROM users WHERE user_uuid = :userUuid)
            """;
    public static final String SELECT_USER_PASSWORD_QUERY =
            """
            SELECT c.password FROM credentials c JOIN users u ON c.user_id = u.user_id WHERE u.user_uuid = :userUuid
            """;
    public static final String UPDATE_USER_ROLE_FUNCTION =
            """
            SELECT * FROM update_user_role(:userUuid, :role)
            """;
    public static final String SELECT_USERS_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.username, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id LIMIT 100
            """;
    public static final String SELECT_TICKET_ASSIGNEE_QUERY =
            """
            SELECT u.user_id, u.user_uuid, u.first_name, u.last_name, u.email, u.image_url FROM users u JOIN tickets t ON u.user_id = t.assignee_id WHERE t.ticket_uuid = :ticketUuid
            """;
    public static final String SELECT_USER_CREDENTIAL_QUERY =
            """
            SELECT c.credential_id, c.credential_uuid, c.password, c.created_at, c.updated_at FROM credentials c JOIN users u ON c.user_id = u.user_id WHERE u.user_uuid = :userUuid
            """;
    public static final String SELECT_DEVICES_QUERY =
            """
            SELECT * FROM devices WHERE user_id = (SELECT user_id FROM users WHERE user_uuid = :userUuid) ORDER BY created_at DESC LIMIT 15
            """;
    public static final String CREATE_PASSWORD_TOKEN_QUERY =
            """
            INSERT INTO password_tokens (user_id, token) VALUES (:userId, :token)
            """;
    public static final String SELECT_TECH_SUPPORTS_QUERY =
            """
            SELECT u.user_uuid, u.first_name, u.last_name, u.image_url, u.email FROM users u JOIN user_roles ur ON u.user_id = ur.user_id JOIN roles r ON ur.role_id = r.role_id WHERE r.name = 'TECH_SUPPORT'
            """;
    public static final String SELECT_TICKET_USER_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id WHERE u.user_id = (SELECT user_id FROM tickets WHERE ticket_uuid = :ticketUuid)
            """;
}
