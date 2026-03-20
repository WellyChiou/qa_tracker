-- Baseline-only account bootstrap:
-- username: admin
-- default password (for initialization verification): admin123
-- IMPORTANT: replace this password immediately after first deployment.
USE invest;

UPDATE users
SET password = '$2a$10$lUNZEYnqKQQAr8D3kZqCb.iSU0L7RWMIYLFP7LMqRANshdLcGpO8a',
    is_enabled = 1,
    is_account_non_locked = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'admin';
