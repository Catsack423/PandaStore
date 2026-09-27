-- Local development account: checkout_admin / AdminDemo123!
-- BCrypt cost 12, generated and verified with the backend PasswordService.
-- A duplicate username or email is left unchanged.
INSERT INTO users (username, email, password_hash, role, status, created_at, updated_at)
VALUES (
    'checkout_admin',
    'checkout_admin@example.com',
    '$2a$12$O4.gGlatusCiWddyf./p0et/MlR..osvNU8/vZLvX7T9RDY8YOJUi',
    'ADMIN',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING
RETURNING user_id, username, role, status;

SELECT user_id, username, role, status
FROM users
WHERE username = 'checkout_admin';
