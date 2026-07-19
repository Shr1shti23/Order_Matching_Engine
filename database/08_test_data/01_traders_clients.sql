USE trading_platform;

-- =====================================================================
-- TRADERS (Created by the system admin, user_id = 1)
-- =====================================================================
INSERT INTO users (user_id, username, email, password_hash, password_algo, role_id, status, created_by)
VALUES
    (2, 'trader_arjun',   'arjun.mehta@bank.local',    '', 'argon2id', 2, 'ACTIVE', 1),
    (3, 'trader_priya',   'priya.sharma@bank.local',   '', 'argon2id', 2, 'ACTIVE', 1),
    (4, 'trader_rohan',   'rohan.verma@bank.local',    '', 'argon2id', 2, 'ACTIVE', 1),
    (5, 'trader_anika',   'anika.gupta@bank.local',    '', 'argon2id', 2, 'ACTIVE', 1),
    (6, 'trader_vikram',  'vikram.nair@bank.local',    '', 'argon2id', 2, 'ACTIVE', 1);

INSERT INTO traders (user_id, employee_code, department)
VALUES
    (2, 'EMP-001', 'Equities'),
    (3, 'EMP-002', 'Derivatives'),
    (4, 'EMP-003', 'Fixed Income'),
    (5, 'EMP-004', 'FX & Commodities'),
    (6, 'EMP-005', 'Equities');

-- =====================================================================
-- CLIENTS (Created by the system admin, user_id = 1)
-- =====================================================================
INSERT INTO users (user_id, username, email, password_hash, password_algo, role_id, status, created_by)
VALUES
    (7, 'client_sanjay',  'sanjay.kapoor@mail.com',    '', 'argon2id', 3, 'ACTIVE', 1),
    (8, 'client_neha',    'neha.joshi@mail.com',        '', 'argon2id', 3, 'ACTIVE', 1),
    (9, 'client_rahul',   'rahul.bansal@mail.com',      '', 'argon2id', 3, 'ACTIVE', 1),
    (10, 'client_divya',   'divya.iyer@mail.com',        '', 'argon2id', 3, 'ACTIVE', 1),
    (11, 'client_karan',   'karan.malhotra@mail.com',    '', 'argon2id', 3, 'ACTIVE', 1);

INSERT INTO clients (user_id, kyc_status, risk_profile)
VALUES
    (7, 'VERIFIED', 'HIGH'),
    (8, 'VERIFIED', 'MEDIUM'),
    (9, 'PENDING',  'LOW'),
    (10, 'VERIFIED', 'MEDIUM'),
    (11, 'REJECTED', 'HIGH');

-- Each client starts with a wallet loaded with money (e.g. INR 500,000 to 1,000,000)
INSERT INTO wallets (client_id, cash_balance, currency)
VALUES
    (7, 500000.00, 'INR'),
    (8, 750000.00, 'INR'),
    (9, 250000.00, 'INR'),
    (10, 1000000.00, 'INR'),
    (11, 150000.00, 'INR');

-- =====================================================================
-- TRADER ↔ CLIENT ASSIGNMENTS
-- =====================================================================
INSERT INTO trader_client_assignments (trader_id, client_id, assigned_by)
VALUES
    (2, 7, 1),   -- Arjun manages Sanjay
    (3, 8, 1),   -- Priya manages Neha
    (4, 9, 1),   -- Rohan manages Rahul
    (5, 10, 1),  -- Anika manages Divya
    (6, 11, 1);  -- Vikram manages Karan
