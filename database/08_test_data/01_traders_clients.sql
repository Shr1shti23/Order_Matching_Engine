USE order_matching_engine;

-- =====================================================================
-- COMPREHENSIVE SEED DATA — 25 TRADERS + 25 CLIENTS + ASSIGNMENTS
-- =====================================================================
-- Password Hashes (Argon2id m=19456, t=2, p=1 — OWASP-aligned):
--   Active Traders  password : Trader#2026Pass!
--   Active Clients  password : Client#2026Pass!
--   Suspended users password : Suspended#2026!
--
-- User-ID allocation:
--   sysadmin  : user_id =  1
--   trader_01…trader_25 : user_id =  2…26
--   client_01…client_25 : user_id = 27…51
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- TRADERS — users table (user_id 2..26)
-- All created by sysadmin (user_id = 1)
-- =====================================================================
INSERT INTO users (user_id, username, email, password_hash, password_algo, role_id, status, created_by)
VALUES
-- Active Traders (1–24), password: Trader#2026Pass!
(2,  'trader_01', 'trader01@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(3,  'trader_02', 'trader02@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(4,  'trader_03', 'trader03@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(5,  'trader_04', 'trader04@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(6,  'trader_05', 'trader05@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(7,  'trader_06', 'trader06@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(8,  'trader_07', 'trader07@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(9,  'trader_08', 'trader08@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(10, 'trader_09', 'trader09@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(11, 'trader_10', 'trader10@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(12, 'trader_11', 'trader11@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(13, 'trader_12', 'trader12@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(14, 'trader_13', 'trader13@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(15, 'trader_14', 'trader14@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(16, 'trader_15', 'trader15@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(17, 'trader_16', 'trader16@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(18, 'trader_17', 'trader17@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(19, 'trader_18', 'trader18@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(20, 'trader_19', 'trader19@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(21, 'trader_20', 'trader20@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(22, 'trader_21', 'trader21@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(23, 'trader_22', 'trader22@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(24, 'trader_23', 'trader23@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
(25, 'trader_24', 'trader24@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$c1+7nDDdCz1XHOS0/WDRvA$DNWbdPMWgkMvvrnMe9SHQ/fLMcg5UXJqgk0yNVsD+fY', 'argon2id', 2, 'ACTIVE', 1),
-- Suspended Trader (25), password: Suspended#2026!
(26, 'trader_25', 'trader25@bank.local', '$argon2id$v=19$m=19456,t=2,p=1$a/AwtpLOc/W4vHTnzq+O/A$L7NaA4IDJQOBk3IU+ezWaHJjm0PIDyAB2bde2UDWL4E', 'argon2id', 2, 'SUSPENDED', 1);

-- =====================================================================
-- TRADERS — trader profile table
-- =====================================================================
INSERT INTO traders (user_id, employee_code, department, aadhaar_last4)
VALUES
(2,  'EMP-001', 'Equities',              '1234'),
(3,  'EMP-002', 'Derivatives',           '2345'),
(4,  'EMP-003', 'Fixed Income',          '3456'),
(5,  'EMP-004', 'FX & Commodities',      '4567'),
(6,  'EMP-005', 'Quantitative Trading',  '5678'),
(7,  'EMP-006', 'Equities',              '6789'),
(8,  'EMP-007', 'Equities',              '7890'),
(9,  'EMP-008', 'Derivatives',           '8901'),
(10, 'EMP-009', 'Fixed Income',          '9012'),
(11, 'EMP-010', 'Equities',              '0123'),
(12, 'EMP-011', 'Derivatives',           '1235'),
(13, 'EMP-012', 'FX & Commodities',      '2346'),
(14, 'EMP-013', 'Quantitative Trading',  '3457'),
(15, 'EMP-014', 'Equities',              '4568'),
(16, 'EMP-015', 'Derivatives',           '5679'),
(17, 'EMP-016', 'Fixed Income',          '6780'),
(18, 'EMP-017', 'FX & Commodities',      '7891'),
(19, 'EMP-018', 'Equities',              '8902'),
(20, 'EMP-019', 'Derivatives',           '9013'),
(21, 'EMP-020', 'Equities',              '0124'),
(22, 'EMP-021', 'Fixed Income',          '1236'),
(23, 'EMP-022', 'Derivatives',           '2347'),
(24, 'EMP-023', 'Quantitative Trading',  '3458'),
(25, 'EMP-024', 'Equities',              '4569'),
(26, 'EMP-025', 'Derivatives',           '5670');  -- suspended trader

-- =====================================================================
-- CLIENTS — users table (user_id 27..51)
-- All created by sysadmin (user_id = 1)
-- =====================================================================
INSERT INTO users (user_id, username, email, password_hash, password_algo, role_id, status, created_by)
VALUES
-- Active Clients (01–24), password: Client#2026Pass!
(27, 'client_01', 'client01@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(28, 'client_02', 'client02@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(29, 'client_03', 'client03@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(30, 'client_04', 'client04@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(31, 'client_05', 'client05@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(32, 'client_06', 'client06@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(33, 'client_07', 'client07@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(34, 'client_08', 'client08@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(35, 'client_09', 'client09@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(36, 'client_10', 'client10@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(37, 'client_11', 'client11@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(38, 'client_12', 'client12@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(39, 'client_13', 'client13@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(40, 'client_14', 'client14@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(41, 'client_15', 'client15@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(42, 'client_16', 'client16@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(43, 'client_17', 'client17@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(44, 'client_18', 'client18@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(45, 'client_19', 'client19@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(46, 'client_20', 'client20@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(47, 'client_21', 'client21@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(48, 'client_22', 'client22@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(49, 'client_23', 'client23@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
(50, 'client_24', 'client24@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$rE1YkCGOq+HR1SY7dfwtWQ$tlp41Tq2g27n9QgsVIsZJCqA+crF82KxPrbRZaTm5Ec', 'argon2id', 3, 'ACTIVE', 1),
-- Suspended Client (25), password: Suspended#2026!
(51, 'client_25', 'client25@mail.com', '$argon2id$v=19$m=19456,t=2,p=1$a/AwtpLOc/W4vHTnzq+O/A$L7NaA4IDJQOBk3IU+ezWaHJjm0PIDyAB2bde2UDWL4E', 'argon2id', 3, 'SUSPENDED', 1);

-- =====================================================================
-- CLIENTS — client profile table
-- KYC / risk profile mix: realistic distribution
-- =====================================================================
INSERT INTO clients (user_id, kyc_status, risk_profile, aadhaar_last4)
VALUES
(27, 'VERIFIED', 'HIGH',   '2211'),
(28, 'VERIFIED', 'MEDIUM', '3322'),
(29, 'VERIFIED', 'HIGH',   '4433'),
(30, 'VERIFIED', 'MEDIUM', '5544'),
(31, 'VERIFIED', 'HIGH',   '6655'),
(32, 'VERIFIED', 'MEDIUM', '7766'),
(33, 'VERIFIED', 'LOW',    '8877'),
(34, 'VERIFIED', 'MEDIUM', '9988'),
(35, 'VERIFIED', 'LOW',    '0099'),
(36, 'VERIFIED', 'HIGH',   '1100'),
(37, 'VERIFIED', 'MEDIUM', '2212'),
(38, 'VERIFIED', 'LOW',    '3323'),
(39, 'VERIFIED', 'MEDIUM', '4434'),
(40, 'VERIFIED', 'HIGH',   '5545'),
(41, 'VERIFIED', 'MEDIUM', '6656'),
(42, 'VERIFIED', 'LOW',    '7767'),
(43, 'VERIFIED', 'MEDIUM', '8878'),
(44, 'VERIFIED', 'HIGH',   '9989'),
(45, 'PENDING',  'LOW',    '0090'),
(46, 'VERIFIED', 'MEDIUM', '1201'),
(47, 'VERIFIED', 'HIGH',   '2312'),
(48, 'VERIFIED', 'MEDIUM', '3423'),
(49, 'PENDING',  'LOW',    '4534'),
(50, 'VERIFIED', 'MEDIUM', '5645'),
(51, 'REJECTED', 'HIGH',   '6756');  -- suspended client

-- =====================================================================
-- WALLETS — one wallet per client
-- Balances reflect post-trade state (net of all historical trades)
-- wallet_id auto-increment: client_01→1, client_02→2, …, client_25→25
-- =====================================================================
-- Starting deposits minus trade debits / plus trade credits (INR):
--
--   client_01 (uid=27): deposit 5,000,000  buys: T1(−17550)+T11(−24300)         = 4,958,150.00
--   client_02 (uid=28): deposit 4,500,000  sells: T1(+17550) buys: T12(−73875)  sells: T15(+27825) = 4,471,500.00
--   client_03 (uid=29): deposit 4,000,000  buys: T2(−21010)+T13(−10890)         = 3,968,100.00
--   client_04 (uid=30): deposit 3,500,000  sells: T2(+21010) buys: T14(−30200)  = 3,490,810.00
--   client_05 (uid=31): deposit 3,000,000  buys: T3(−36000)+T15(−27825)         = 2,936,175.00
--   client_06 (uid=32): deposit 3,200,000  sells: T3(+36000) buys: T16−partial(−21120) = 3,214,880.00
--   client_07 (uid=33): deposit 2,800,000  buys: T4(−22612.50)                  = 2,777,387.50
--   client_08 (uid=34): deposit 2,600,000  sells: T4(+22612.50)                 = 2,622,612.50
--   client_09 (uid=35): deposit 2,400,000  buys: T5(−14559)                     = 2,385,441.00
--   client_10 (uid=36): deposit 3,000,000  sells: T5(+14559)                    = 3,014,559.00
--   client_11 (uid=37): deposit 2,000,000  buys: T6(−17520)                     = 1,982,480.00
--   client_12 (uid=38): deposit 1,800,000  sells: T6(+17520)                    = 1,817,520.00
--   client_13 (uid=39): deposit 1,600,000  buys: T7(−18500)                     = 1,581,500.00
--   client_14 (uid=40): deposit 1,500,000  sells: T7(+18500)                    = 1,518,500.00
--   client_15 (uid=41): deposit 2,200,000  buys: T8(−14750)                     = 2,185,250.00
--   client_16 (uid=42): deposit 1,500,000  sells: T8(+14750)                    = 1,514,750.00
--   client_17 (uid=43): deposit 1,400,000  buys: T9(−14080)                     = 1,385,920.00
--   client_18 (uid=44): deposit 1,300,000  sells: T9(+14080)                    = 1,314,080.00
--   client_19 (uid=45): deposit 1,200,000  buys: T10(−16840)                    = 1,183,160.00
--   client_20 (uid=46): deposit 1,800,000  sells: T10(+16840)+T16(+21120)       = 1,837,960.00
--   client_21 (uid=47): deposit 1,000,000  sells: T11(+24300)                   = 1,024,300.00
--   client_22 (uid=48): deposit   900,000  sells: T12(+73875)                   =   973,875.00
--   client_23 (uid=49): deposit   800,000  sells: T13(+10890)                   =   810,890.00
--   client_24 (uid=50): deposit   750,000  sells: T14(+30200)                   =   780,200.00
--   client_25 (uid=51): deposit   500,000  SUSPENDED — no trades                =   500,000.00
--
INSERT INTO wallets (client_id, cash_balance, currency)
VALUES
(27, 4958150.00, 'INR'),   -- client_01
(28, 4471500.00, 'INR'),   -- client_02
(29, 3968100.00, 'INR'),   -- client_03
(30, 3490810.00, 'INR'),   -- client_04
(31, 2936175.00, 'INR'),   -- client_05
(32, 3214880.00, 'INR'),   -- client_06
(33, 2777387.50, 'INR'),   -- client_07
(34, 2622612.50, 'INR'),   -- client_08
(35, 2385441.00, 'INR'),   -- client_09
(36, 3014559.00, 'INR'),   -- client_10
(37, 1982480.00, 'INR'),   -- client_11
(38, 1817520.00, 'INR'),   -- client_12
(39, 1581500.00, 'INR'),   -- client_13
(40, 1518500.00, 'INR'),   -- client_14
(41, 2185250.00, 'INR'),   -- client_15
(42, 1514750.00, 'INR'),   -- client_16
(43, 1385920.00, 'INR'),   -- client_17
(44, 1314080.00, 'INR'),   -- client_18
(45, 1183160.00, 'INR'),   -- client_19
(46, 1837960.00, 'INR'),   -- client_20
(47, 1024300.00, 'INR'),   -- client_21
(48,  973875.00, 'INR'),   -- client_22
(49,  810890.00, 'INR'),   -- client_23
(50,  780200.00, 'INR'),   -- client_24
(51,  500000.00, 'INR');   -- client_25 (SUSPENDED)

-- =====================================================================
-- TRADER ↔ CLIENT ASSIGNMENTS
-- trader_N manages client_N (1-to-1, all assigned by sysadmin)
-- =====================================================================
INSERT INTO trader_client_assignments (trader_id, client_id, assigned_by)
VALUES
(2,  27, 1),   -- trader_01 → client_01
(3,  28, 1),   -- trader_02 → client_02
(4,  29, 1),   -- trader_03 → client_03
(5,  30, 1),   -- trader_04 → client_04
(6,  31, 1),   -- trader_05 → client_05
(7,  32, 1),   -- trader_06 → client_06
(8,  33, 1),   -- trader_07 → client_07
(9,  34, 1),   -- trader_08 → client_08
(10, 35, 1),   -- trader_09 → client_09
(11, 36, 1),   -- trader_10 → client_10
(12, 37, 1),   -- trader_11 → client_11
(13, 38, 1),   -- trader_12 → client_12
(14, 39, 1),   -- trader_13 → client_13
(15, 40, 1),   -- trader_14 → client_14
(16, 41, 1),   -- trader_15 → client_15
(17, 42, 1),   -- trader_16 → client_16
(18, 43, 1),   -- trader_17 → client_17
(19, 44, 1),   -- trader_18 → client_18
(20, 45, 1),   -- trader_19 → client_19
(21, 46, 1),   -- trader_20 → client_20
(22, 47, 1),   -- trader_21 → client_21
(23, 48, 1),   -- trader_22 → client_22
(24, 49, 1),   -- trader_23 → client_23
(25, 50, 1),   -- trader_24 → client_24
(26, 51, 1);   -- trader_25 → client_25 (both suspended)

SET FOREIGN_KEY_CHECKS = 1;
