-- ════════════════════════════════════════════════════════════════════════════
-- Author: Kardigué MAGASSA
-- MediLabo Authorization Server - Database Schema
-- Date : January 5th 2026
-- Version: 1.0
-- ════════════════════════════════════════════════════════════════════════════

-- ════════════════════════════════════════════════════════════════════════════
--- General Rules ---
-- Use underscore_names instead of CamelCase --
-- Table names should be plural --
-- Spell out id fields (item_id instead of id) --
-- Don't use ambiguous column names --
-- Name foreign key columns the same as the columns they refer to --
-- Use caps for all SQL keywords --
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

-- AUTHORIZATION SERVER
CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    client_id_issued_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret VARCHAR(200) DEFAULT NULL,
    client_secret_expires_at TIMESTAMP(6) WITH TIME ZONE DEFAULT NULL,
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types VARCHAR(1000) NOT NULL,
    redirect_uris VARCHAR(1000) DEFAULT NULL,
    post_logout_redirect_uris VARCHAR(1000) DEFAULT NULL,
    scopes VARCHAR(1000) NOT NULL,
    client_settings VARCHAR(2000) NOT NULL,
    token_settings VARCHAR(2000) NOT NULL,
    CONSTRAINT uq_oauth2_client_id UNIQUE (client_id)
);

-- USER SERVICE
CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    user_uuid VARCHAR(40) NOT NULL,
    username VARCHAR(50) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    member_id VARCHAR(40) NOT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    address VARCHAR(255) DEFAULT NULL,
    bio VARCHAR(255) DEFAULT NULL,
    qr_code_secret VARCHAR(100) DEFAULT NULL,
    qr_code_image_uri TEXT DEFAULT NULL,
    image_url VARCHAR(255) DEFAULT 'https://cdn-icons-png.flaticon.com/512/149/149071.png',
    last_login TIMESTAMP(6) WITH TIME ZONE DEFAULT NULL,
    login_attempts INTEGER DEFAULT 0,
    mfa BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_user_uuid UNIQUE (user_uuid),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_member_id UNIQUE (member_id)
);

CREATE TABLE IF NOT EXISTS roles (
    role_id BIGSERIAL PRIMARY KEY,
    role_uuid VARCHAR(40) NOT NULL,
    name VARCHAR(50) NOT NULL,
    authority TEXT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT uq_roles_role_uuid UNIQUE (role_uuid)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_role_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) 
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles (role_id) 
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS credentials (
    credential_id BIGSERIAL PRIMARY KEY,
    credential_uuid VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_credentials_credential_uuid UNIQUE (credential_uuid),
    CONSTRAINT uq_credentials_user_id UNIQUE (user_id),
    CONSTRAINT fk_credentials_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) 
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

-- TOKEN FOR ACCOUNT ACTIVATION
CREATE TABLE IF NOT EXISTS account_tokens (
    account_token_id BIGSERIAL PRIMARY KEY,
    token VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_account_tokens_token UNIQUE (token),
    CONSTRAINT uq_account_tokens_user_id UNIQUE (user_id),
    CONSTRAINT fk_account_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) 
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

-- TOKEN FOR PASSWORD RESET
CREATE TABLE IF NOT EXISTS password_tokens (
    password_token_id BIGSERIAL PRIMARY KEY,
    token VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_password_tokens_token UNIQUE (token),
    CONSTRAINT uq_password_tokens_user_id UNIQUE (user_id),
    CONSTRAINT fk_password_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) 
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

-- DEVICES CONNECTION TRACKING
CREATE TABLE IF NOT EXISTS devices (
    device_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device VARCHAR(100) NOT NULL,
    client VARCHAR(100) NOT NULL,
    ip_address VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_devices_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) 
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

-- INDEXES

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_user_uuid ON users(user_uuid);
CREATE INDEX IF NOT EXISTS idx_devices_user_id ON devices(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);

-- STORED PROCEDURES
-- Procedure for creating a full user
CREATE OR REPLACE PROCEDURE create_user (
    IN p_user_uuid VARCHAR(40),
    IN p_first_name VARCHAR(25),
    IN p_last_name VARCHAR(25),
    IN p_email VARCHAR(40),
    IN p_username VARCHAR(25),
    IN p_password VARCHAR(255),
    IN p_credential_uuid VARCHAR(40),
    IN p_token VARCHAR(40),
    IN p_member_id VARCHAR(40),
    IN p_role_name VARCHAR(25)
)
LANGUAGE PLPGSQL
AS $$
DECLARE
v_user_id BIGINT;
BEGIN
INSERT INTO users (user_uuid, first_name, last_name, email, username, member_id)
VALUES (p_user_uuid, p_first_name, p_last_name, p_email, p_username, p_member_id)
    RETURNING user_id INTO v_user_id;

INSERT INTO credentials (credential_uuid, user_id, password)
VALUES (p_credential_uuid, v_user_id, p_password);

INSERT INTO user_roles (user_id, role_id)
VALUES (v_user_id, (SELECT role_id FROM roles WHERE name = p_role_name));

INSERT INTO account_tokens (user_id, token)
VALUES (v_user_id, p_token);
END;
$$;

-- ────────────────────────────────────────────────────────────────────────────
-- FUNCTIONS
-- ────────────────────────────────────────────────────────────────────────────

-- Function to enable/disable MFA
CREATE OR REPLACE FUNCTION enable_user_mfa (
    IN p_user_uuid VARCHAR(40), 
    IN p_qr_code_secret VARCHAR(100), 
    IN p_qr_code_image_uri TEXT
)
RETURNS TABLE(
    qr_code_image_uri TEXT, 
    member_id VARCHAR, 
    role VARCHAR, 
    authorities TEXT, 
    account_non_expired BOOLEAN, 
    account_non_locked BOOLEAN, 
    created_at TIMESTAMP WITH TIME ZONE, 
    email VARCHAR, 
    enabled BOOLEAN, 
    first_name VARCHAR, 
    user_id BIGINT, 
    image_url VARCHAR, 
    last_login TIMESTAMP WITH TIME ZONE, 
    last_name VARCHAR, 
    mfa BOOLEAN, 
    updated_at TIMESTAMP WITH TIME ZONE, 
    user_uuid VARCHAR, 
    phone VARCHAR, 
    bio VARCHAR, 
    address VARCHAR
)
LANGUAGE PLPGSQL
AS $$
BEGIN
    UPDATE users 
    SET mfa = TRUE, qr_code_secret = p_qr_code_secret, qr_code_image_uri = p_qr_code_image_uri 
    WHERE users.user_uuid = p_user_uuid;
    
    RETURN QUERY 
    SELECT u.qr_code_image_uri, u.member_id, r.name AS role, r.authority AS authorities, 
           u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, 
           u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, 
           u.updated_at, u.user_uuid, u.phone, u.bio, u.address 
    FROM users u 
    JOIN user_roles ur ON ur.user_id = u.user_id 
    JOIN roles r ON r.role_id = ur.role_id 
    WHERE u.user_uuid = p_user_uuid;
END;
$$;

-- Function to disable MFA
CREATE OR REPLACE FUNCTION disable_user_mfa (IN p_user_uuid VARCHAR(40))
RETURNS TABLE(
    member_id VARCHAR, 
    role VARCHAR, 
    authorities TEXT, 
    account_non_expired BOOLEAN, 
    account_non_locked BOOLEAN, 
    created_at TIMESTAMP WITH TIME ZONE, 
    email VARCHAR, 
    enabled BOOLEAN, 
    first_name VARCHAR, 
    user_id BIGINT, 
    image_url VARCHAR, 
    last_login TIMESTAMP WITH TIME ZONE, 
    last_name VARCHAR, 
    mfa BOOLEAN, 
    updated_at TIMESTAMP WITH TIME ZONE, 
    user_uuid VARCHAR, 
    phone VARCHAR, 
    bio VARCHAR, 
    address VARCHAR
)
LANGUAGE PLPGSQL
AS $$
BEGIN
    UPDATE users 
    SET mfa = FALSE, qr_code_secret = NULL, qr_code_image_uri = NULL 
    WHERE users.user_uuid = p_user_uuid;
    
    RETURN QUERY 
    SELECT u.member_id, r.name AS role, r.authority AS authorities, 
           u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, 
           u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, 
           u.updated_at, u.user_uuid, u.phone, u.bio, u.address 
    FROM users u 
    JOIN user_roles ur ON ur.user_id = u.user_id 
    JOIN roles r ON r.role_id = ur.role_id 
    WHERE u.user_uuid = p_user_uuid;
END;
$$;

-- Fonction pour toggle account locked
CREATE OR REPLACE FUNCTION toggle_account_locked (IN p_user_uuid VARCHAR(40))
RETURNS TABLE(
    qr_code_image_uri TEXT, 
    member_id VARCHAR, 
    role VARCHAR, 
    authorities TEXT, 
    account_non_expired BOOLEAN, 
    account_non_locked BOOLEAN, 
    created_at TIMESTAMP WITH TIME ZONE, 
    email VARCHAR, 
    enabled BOOLEAN, 
    first_name VARCHAR, 
    user_id BIGINT, 
    image_url VARCHAR, 
    last_login TIMESTAMP WITH TIME ZONE, 
    last_name VARCHAR, 
    mfa BOOLEAN, 
    updated_at TIMESTAMP WITH TIME ZONE, 
    user_uuid VARCHAR, 
    phone VARCHAR, 
    bio VARCHAR, 
    address VARCHAR
)
LANGUAGE PLPGSQL
AS $$
BEGIN
    UPDATE users SET account_non_locked = NOT users.account_non_locked WHERE users.user_uuid = p_user_uuid;
    
    RETURN QUERY 
    SELECT u.qr_code_image_uri, u.member_id, r.name AS role, r.authority AS authorities, 
           u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, 
           u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, 
           u.updated_at, u.user_uuid, u.phone, u.bio, u.address 
    FROM users u 
    JOIN user_roles ur ON ur.user_id = u.user_id 
    JOIN roles r ON r.role_id = ur.role_id 
    WHERE u.user_uuid = p_user_uuid;
END;
$$;

-- Fonction pour toggle account enabled
CREATE OR REPLACE FUNCTION toggle_account_enabled (IN p_user_uuid VARCHAR(40))
RETURNS TABLE(
    qr_code_image_uri TEXT, 
    member_id VARCHAR, 
    role VARCHAR, 
    authorities TEXT, 
    account_non_expired BOOLEAN, 
    account_non_locked BOOLEAN, 
    created_at TIMESTAMP WITH TIME ZONE, 
    email VARCHAR, 
    enabled BOOLEAN, 
    first_name VARCHAR, 
    user_id BIGINT, 
    image_url VARCHAR, 
    last_login TIMESTAMP WITH TIME ZONE, 
    last_name VARCHAR, 
    mfa BOOLEAN, 
    updated_at TIMESTAMP WITH TIME ZONE, 
    user_uuid VARCHAR, 
    phone VARCHAR, 
    bio VARCHAR, 
    address VARCHAR
)
LANGUAGE PLPGSQL
AS $$
BEGIN
    UPDATE users SET enabled = NOT users.enabled WHERE users.user_uuid = p_user_uuid;
    
    RETURN QUERY 
    SELECT u.qr_code_image_uri, u.member_id, r.name AS role, r.authority AS authorities, 
           u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, 
           u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, 
           u.updated_at, u.user_uuid, u.phone, u.bio, u.address 
    FROM users u 
    JOIN user_roles ur ON ur.user_id = u.user_id 
    JOIN roles r ON r.role_id = ur.role_id 
    WHERE u.user_uuid = p_user_uuid;
END;
$$;

-- Fonction pour mettre à jour le rôle utilisateur
CREATE OR REPLACE FUNCTION update_user_role (IN p_user_uuid VARCHAR(40), IN p_role VARCHAR(50))
RETURNS TABLE(
    qr_code_image_uri TEXT, 
    member_id VARCHAR, 
    role VARCHAR, 
    authorities TEXT, 
    account_non_expired BOOLEAN, 
    account_non_locked BOOLEAN, 
    created_at TIMESTAMP WITH TIME ZONE, 
    email VARCHAR, 
    enabled BOOLEAN, 
    first_name VARCHAR, 
    user_id BIGINT, 
    image_url VARCHAR, 
    last_login TIMESTAMP WITH TIME ZONE, 
    last_name VARCHAR, 
    mfa BOOLEAN, 
    updated_at TIMESTAMP WITH TIME ZONE, 
    user_uuid VARCHAR, 
    phone VARCHAR, 
    bio VARCHAR, 
    address VARCHAR
)
LANGUAGE PLPGSQL
AS $$
BEGIN
    UPDATE user_roles 
    SET role_id = (SELECT r.role_id FROM roles r WHERE r.name = p_role) 
    WHERE user_roles.user_id = (SELECT users.user_id FROM users WHERE users.user_uuid = p_user_uuid);
    
    RETURN QUERY 
    SELECT u.qr_code_image_uri, u.member_id, r.name AS role, r.authority AS authorities, 
           u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, 
           u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, 
           u.updated_at, u.user_uuid, u.phone, u.bio, u.address 
    FROM users u 
    JOIN user_roles ur ON ur.user_id = u.user_id 
    JOIN roles r ON r.role_id = ur.role_id 
    WHERE u.user_uuid = p_user_uuid;
END;
$$;

-- Fonction pour mettre à jour le profil utilisateur
CREATE OR REPLACE FUNCTION update_user_profile(
    IN p_user_uuid VARCHAR(40), 
    IN p_first_name VARCHAR(50), 
    IN p_last_name VARCHAR(50), 
    IN p_email VARCHAR(100), 
    IN p_phone VARCHAR(20), 
    IN p_bio VARCHAR(255), 
    IN p_address VARCHAR(255)
)
RETURNS TABLE(
    qr_code_image_uri TEXT, 
    member_id VARCHAR, 
    role VARCHAR, 
    authorities TEXT, 
    account_non_expired BOOLEAN, 
    account_non_locked BOOLEAN, 
    created_at TIMESTAMP WITH TIME ZONE, 
    email VARCHAR, 
    enabled BOOLEAN, 
    first_name VARCHAR, 
    user_id BIGINT, 
    image_url VARCHAR, 
    last_login TIMESTAMP WITH TIME ZONE, 
    last_name VARCHAR, 
    mfa BOOLEAN, 
    updated_at TIMESTAMP WITH TIME ZONE, 
    user_uuid VARCHAR, 
    phone VARCHAR, 
    bio VARCHAR, 
    address VARCHAR
)
LANGUAGE PLPGSQL
AS $$
BEGIN
    UPDATE users u 
    SET first_name = p_first_name, 
        last_name = p_last_name, 
        email = p_email, 
        phone = p_phone, 
        bio = p_bio, 
        address = p_address, 
        updated_at = NOW() 
    WHERE u.user_uuid = p_user_uuid;
    
    RETURN QUERY 
    SELECT u.qr_code_image_uri, u.member_id, r.name AS role, r.authority AS authorities, 
           u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, 
           u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, 
           u.updated_at, u.user_uuid, u.phone, u.bio, u.address 
    FROM users u 
    JOIN user_roles ur ON ur.user_id = u.user_id 
    JOIN roles r ON r.role_id = ur.role_id 
    WHERE u.user_uuid = p_user_uuid;
END;
$$;

COMMIT;
