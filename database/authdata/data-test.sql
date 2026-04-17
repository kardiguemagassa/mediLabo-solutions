-- TestNone (F, 1966-12-31)
CALL create_user ('b292408d-e54e-4383-bf32-e1ef296ca38a','Test','None','testnone@medilabo.com','testnone','$2a$12$9Rq8RyAQcWyCDd2307dOZuopxbJ2.pyxuIiDWQYENEiMapmBRyhb2','56585e47-013a-4d4e-957e-959fe720f001','58a18b9e-3900-4d16-9203-46a7029ed001','68-00001-01','USER');

-- TestBorderline (M, 1945-06-24)
CALL create_user ('21659dac-a8db-4ad1-aa2b-7fe12535dc64','Test','Borderline','testborderline@medilabo.com','testborderline','$2a$12$9Rq8RyAQcWyCDd2307dOZuopxbJ2.pyxuIiDWQYENEiMapmBRyhb2','56585e47-013a-4d4e-957e-959fe720f002','58a18b9e-3900-4d16-9203-46a7029ed002','68-00002-02','USER');

-- TestInDanger (M, 2004-06-18)
CALL create_user ('d14fd0e9-0967-402b-b867-47e0423f4ed9','Test','InDanger','testindanger@medilabo.com','testindanger','$2a$12$9Rq8RyAQcWyCDd2307dOZuopxbJ2.pyxuIiDWQYENEiMapmBRyhb2','56585e47-013a-4d4e-957e-959fe720f003','58a18b9e-3900-4d16-9203-46a7029ed003','68-00003-03','USER');

-- TestEarlyOnset (F, 2002-06-28)
CALL create_user ('4e06b5a5-65dd-4dd5-995a-cfade775e91e','Test','EarlyOnset','testearlyonset@medilabo.com','testearlyonset','$2a$12$9Rq8RyAQcWyCDd2307dOZuopxbJ2.pyxuIiDWQYENEiMapmBRyhb2','56585e47-013a-4d4e-957e-959fe720f004','58a18b9e-3900-4d16-9203-46a7029ed004','68-00004-04','USER');

-- Activer les comptes
UPDATE users SET enabled = TRUE, account_non_expired = TRUE, account_non_locked = TRUE
WHERE username IN ('testnone', 'testborderline', 'testindanger', 'testearlyonset');

-- Vérifier
SELECT user_uuid, username, first_name, last_name, enabled 
FROM users 
ORDER BY user_id;