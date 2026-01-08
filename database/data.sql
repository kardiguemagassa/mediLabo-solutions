-- MEDILABO AUTHORIZATION SERVER - INITIAL DATA (IMPROVED)
-- ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
BEGIN;

-- ROLES
INSERT INTO roles (role_uuid, name, authority) VALUES ('7d1b82b1-92c7-4fae-b790-73eb1ac9d6b5', 'USER', 'user:read,user:update') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (role_uuid, name, authority) VALUES ('1a0e13de-4fdf-4db0-8a3d-08fce64cbe8c', 'PRACTITIONER', 'user:read,user:update,patient:read,patient:create,patient:update,note:read,note:create,note:update,assessment:read') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (role_uuid, name, authority) VALUES ('894853e1-9238-4c64-b5d8-c0a29bdf1b94', 'ORGANIZER', 'user:read,user:update,patient:read,patient:create,patient:update,patient:delete,note:read,note:create,note:update,note:delete,assessment:read,assessment:create') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (role_uuid, name, authority) VALUES ('598e0368-8d8d-43ca-95be-a1b949d368e6', 'ADMIN', 'user:create,user:read,user:update,user:delete,patient:read,patient:create,patient:update,patient:delete,note:read,note:create,note:update,note:delete,assessment:read,assessment:create') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (role_uuid, name, authority) 
VALUES (
    '838ca5ee-eb15-427a-b380-6cf7bfbd68b7', 
    'SUPER_ADMIN', 
    'eureka:read,app:create,app:read,app:update,app:delete,user:create,user:read,user:update,user:delete,patient:read,patient:create,patient:update,patient:delete,note:read,note:create,note:update,note:delete,assessment:read,assessment:create,comment:create,comment:read,comment:update,comment:delete'
) 
ON CONFLICT (name) DO NOTHING;




-- ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
-- CREATE ADMIN USER BY STORED PROCEDURES
-- BCRYPT HASH GENERATE WITH STRENGTH 12
--
-- CALL create_user (
--     '598e0368-8d8d-43ca-95be-a1b949d368e6',
--     'Kardigué',
--     'MAGASSA',
--     'magassakara@gmail.com',
--     'admin',
--     '$2a$12$LHHYFbYxsVRTdwCOvF/iruFi0Bod2RX7zCI2KBU.dpcU5rXNFDbDO',
--     '56585e47-013a-4d4e-957e-959fe720f333',
--     '58a18b9e-3900-4d16-9203-46a7029ed62f',
--     '68-54986-93',
--     'SUPER_ADMIN'
-- );

-- activer admin
UPDATE users
SET enabled = TRUE,
    account_non_expired = TRUE,
    account_non_locked = TRUE,
    mfa = TRUE
WHERE username = 'admin';

-- verifié admin
SELECT user_id, username, email, enabled, account_non_expired, account_non_locked, mfa
FROM users
WHERE username = 'admin';


-- role
SELECT u.username, r.name AS role_name, r.authority
FROM users u
         JOIN user_roles ur ON u.user_id = ur.user_id
         JOIN roles r ON ur.role_id = r.role_id
WHERE u.username = 'admin';


-- query comple
SELECT
    u.user_id,
    u.username,
    u.first_name,
    u.last_name,
    u.email,
    u.member_id,
    u.enabled,
    u.account_non_expired,
    u.account_non_locked,
    u.mfa,
    u.created_at,
    u.updated_at,
    r.name AS role_name,
    r.authority
FROM users u
         LEFT JOIN user_roles ur ON u.user_id = ur.user_id
         LEFT JOIN roles r ON ur.role_id = r.role_id
WHERE u.username = 'admin';

COMMIT;