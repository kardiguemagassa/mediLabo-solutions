SELECT*FROM ACCOUNT_TOKENS;
SELECT*FROM PASSWORD_TOKENS;
SELECT*FROM OAUTH2_REGISTERED_CLIENT;
SELECT*FROM USERS
SELECT*FROM ROLES

SELECT user_id, username, email, enabled, account_non_expired, account_non_locked, mfa
FROM users
WHERE username = 'admin';

UPDATE USERS SET MFA = FALSE WHERE USERNAME = 'admin'

UPDATE oauth2_registered_client SET redirect_uris = 'http://localhost:4200'
WHERE client_id = 'client';

SELECT * FROM oauth2_registered_client WHERE client_id = 'client';

SELECT 
    client_id, 
    redirect_uris, 
    scopes 
FROM oauth2_registered_client 
WHERE client_id = 'client';


-- enable_user_mfa test
SELECT * FROM enable_user_mfa('598e0368-8d8d-43ca-95be-a1b949d368e6','qrCodeSecret','qrCodeImageUri');

-- disable_user_mfa test
SELECT * FROM disable_user_mfa('598e0368-8d8d-43ca-95be-a1b949d368e6');