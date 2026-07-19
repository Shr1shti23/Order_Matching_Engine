USE trading_platform;

-- =====================================================================
-- SYSTEM ADMIN (seed — the only row allowed to have created_by = NULL)
-- =====================================================================
INSERT INTO users (username, email, password_hash, password_algo, role_id, status, created_by)
VALUES (
    'sysadmin',
    'sysadmin@bank.local',
    '$argon2id$v=19$m=65536,t=3,p=4$REPLACE_SALT$REPLACE_HASH',
    'argon2id',
    1,           -- ADMIN
    'ACTIVE',
    NULL         -- no creator; this is the bootstrap account
);
