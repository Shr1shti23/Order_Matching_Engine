-- =====================================================================
-- SEED DATA 02: SYSTEM ADMIN BOOTSTRAP ACCOUNT
-- For execution instructions, see database/07_seed/00_SEED_DATA_INSTRUCTIONS.md
-- =====================================================================
USE order_matching_engine;

-- =====================================================================
-- SYSTEM ADMIN (seed — the only row allowed to have created_by = NULL)
-- Password: sysadmin
-- Hash generated with Argon2id: m=19456, t=2, p=1 (OWASP-aligned)
-- =====================================================================
INSERT INTO users (username, email, password_hash, password_algo, role_id, status, created_by)
VALUES (
    'sysadmin',
    'sysadmin@bank.local',
    '$argon2id$v=19$m=19456,t=2,p=1$pGexS09PIxVDWHxhIRZkXQ$pI6hMBWuU1yCDGiqIcBgXGQ/1+3UjrwSrVgvsAvoR2M',
    'argon2id',
    1,           -- ADMIN
    'ACTIVE',
    NULL         -- no creator; this is the bootstrap account
);
