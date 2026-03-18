-- TestNone (F, 1966-12-31)
CALL create_user ('7d1b82b1-92c7-4fae-b790-73eb1ac9d6b5','Test','None','testnone@medilabo.com','testnone','$2a$12$9Rq8RyAQcWyCDd2307dOZuopxbJ2.pyxuIiDWQYENEiMapmBRyhb2','56585e47-013a-4d4e-957e-959fe720f001','58a18b9e-3900-4d16-9203-46a7029ed001','68-00001-01','USER');

-- TestBorderline (M, 1945-06-24)
CALL create_user ('7d1b82b1-92c7-4fae-b790-73eb1ac9d6b5','Test','Borderline','testborderline@medilabo.com','testborderline','$2a$12$9Rq8RyAQcWyCDd2307dOZuopxbJ2.pyxuIiDWQYENEiMapmBRyhb2','56585e47-013a-4d4e-957e-959fe720f002','58a18b9e-3900-4d16-9203-46a7029ed002','68-00002-02','USER');

-- TestInDanger (M, 2004-06-18)
CALL create_user ('7d1b82b1-92c7-4fae-b790-73eb1ac9d6b5','Test','InDanger','testindanger@medilabo.com','testindanger','$2a$12$9Rq8RyAQcWyCDd2307dOZuopxbJ2.pyxuIiDWQYENEiMapmBRyhb2','56585e47-013a-4d4e-957e-959fe720f003','58a18b9e-3900-4d16-9203-46a7029ed003','68-00003-03','USER');

-- TestEarlyOnset (F, 2002-06-28)
CALL create_user ('7d1b82b1-92c7-4fae-b790-73eb1ac9d6b5','Test','EarlyOnset','testearlyonset@medilabo.com','testearlyonset','$2a$12$9Rq8RyAQcWyCDd2307dOZuopxbJ2.pyxuIiDWQYENEiMapmBRyhb2','56585e47-013a-4d4e-957e-959fe720f004','58a18b9e-3900-4d16-9203-46a7029ed004','68-00004-04','USER');

-- Activer les comptes
UPDATE users SET enabled = TRUE, account_non_expired = TRUE, account_non_locked = TRUE
WHERE username IN ('testnone', 'testborderline', 'testindanger', 'testearlyonset');

-- Vérifier
SELECT user_uuid, username, first_name, last_name, enabled 
FROM users 
ORDER BY user_id;