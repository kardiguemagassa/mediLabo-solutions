package com.openclassrooms.authorizationserverservice.query;

/**
 * Requêtes SQL pour les utilisateurs
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

public class UserQuery {

    public static final String SELECT_USER_BY_USER_ID_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities,  u.qr_code_secret, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.username, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address, c.password, c.updated_at + INTERVAL '90 day' > NOW() AS credentials_non_expired FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id JOIN credentials c ON c.user_id = u.user_id WHERE u.user_uuid = :userUuid
            """;
    public static final String SELECT_USER_BY_EMAIL_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities,  u.qr_code_secret, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.login_attempts, u.account_non_locked, u.created_at, u.email, u.username, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address, c.password, c.updated_at + INTERVAL '90 day' > NOW() AS credentials_non_expired FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id JOIN credentials c ON c.user_id = u.user_id WHERE u.email = :email
            """;
    public static final String SELECT_USER_BY_ID_QUERY =
            """
            SELECT r.name AS role, r.authority AS authorities,  u.qr_code_secret, u.qr_code_image_uri, u.member_id, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.username, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.bio, u.phone, u.address, c.password, c.updated_at + INTERVAL '90 day' > NOW() AS credentials_non_expired FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id JOIN credentials c ON c.user_id = u.user_id WHERE u.id = :id
            """;
    public static final String UPDATE_LOGIN_ATTEMPTS =
            """
            UPDATE users SET login_attempts = login_attempts + 1 WHERE email = :email
            """;
    public static final String RESET_LOGIN_ATTEMPTS_QUERY =
            """
            UPDATE users SET login_attempts = 0 WHERE user_uuid = :userUuid
            """;
    public static final String SET_LAST_LOGIN_QUERY =
            """
            UPDATE users SET last_login = NOW() WHERE user_id = :userId
            """;
    public static final String INSERT_NEW_DEVICE_QUERY =
            """
            INSERT INTO devices (user_id, device, client, ip_address) VALUES (:userId, :device, :client, :ipAddress)
            """;
}