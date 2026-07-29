# Seed User Credentials Reference

> [!WARNING]
> **DEVELOPMENT AND TESTING ONLY**: The credentials listed below are strictly for local development, integration testing, and system bootstrapping. **NEVER** use these credentials in production environments.

---

## 1. Argon2id Password Hashing Specifications

All seeded passwords below are stored in the database as **Argon2id** hashes using OWASP-aligned security parameters:
- **Algorithm:** Argon2id (`$argon2id$`)
- **Memory Cost (`m`):** 19,456 KB (19 MiB)
- **Time Cost / Iterations (`t`):** 2
- **Parallelism (`p`):** 1 thread
- **Salt Length:** 16 bytes (Cryptographically random, per-user salt)
- **Hash Output Length:** 32 bytes

---

## 2. Default System Administrator (1 Account)

| User ID | Username | Default Password | Role | Account Status | Created By | Assigned Trader |
|---|---|---|---|---|---|---|
| 1 | `sysadmin` | `sysadmin` | ADMIN | `ACTIVE` | `NULL` | N/A |

---

## 3. Seeded Trader Accounts (25 Accounts)

| User ID | Username | Email | Default Password | Role | Account Status | Employee Code | Department |
|---|---|---|---|---|---|---|---|
| 2 | `trader_01` | `trader01@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-001 | Equities |
| 3 | `trader_02` | `trader02@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-002 | Derivatives |
| 4 | `trader_03` | `trader03@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-003 | Fixed Income |
| 5 | `trader_04` | `trader04@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-004 | FX & Commodities |
| 6 | `trader_05` | `trader05@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-005 | Quantitative Trading |
| 7 | `trader_06` | `trader06@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-006 | Equities |
| 8 | `trader_07` | `trader07@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-007 | Equities |
| 9 | `trader_08` | `trader08@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-008 | Derivatives |
| 10 | `trader_09` | `trader09@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-009 | Fixed Income |
| 11 | `trader_10` | `trader10@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-010 | Equities |
| 12 | `trader_11` | `trader11@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-011 | Derivatives |
| 13 | `trader_12` | `trader12@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-012 | FX & Commodities |
| 14 | `trader_13` | `trader13@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-013 | Quantitative Trading |
| 15 | `trader_14` | `trader14@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-014 | Equities |
| 16 | `trader_15` | `trader15@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-015 | Derivatives |
| 17 | `trader_16` | `trader16@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-016 | Fixed Income |
| 18 | `trader_17` | `trader17@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-017 | FX & Commodities |
| 19 | `trader_18` | `trader18@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-018 | Equities |
| 20 | `trader_19` | `trader19@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-019 | Derivatives |
| 21 | `trader_20` | `trader20@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-020 | Equities |
| 22 | `trader_21` | `trader21@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-021 | Fixed Income |
| 23 | `trader_22` | `trader22@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-022 | Derivatives |
| 24 | `trader_23` | `trader23@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-023 | Quantitative Trading |
| 25 | `trader_24` | `trader24@bank.local` | `Trader#2026Pass!` | TRADER | `ACTIVE` | EMP-024 | Equities |
| 26 | `trader_25` | `trader25@bank.local` | `Suspended#2026!` | TRADER | `SUSPENDED` | EMP-025 | Derivatives |

---

## 4. Seeded Client Accounts (25 Accounts)

| User ID | Username | Email | Default Password | Role | Account Status | Assigned Trader | KYC Status | Risk Profile | Cash Balance (INR) |
|---|---|---|---|---|---|---|---|---|---|
| 27 | `client_01` | `client01@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_01` (ID 2) | VERIFIED | HIGH | ₹4,958,150.00 |
| 28 | `client_02` | `client02@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_02` (ID 3) | VERIFIED | MEDIUM | ₹4,471,500.00 |
| 29 | `client_03` | `client03@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_03` (ID 4) | VERIFIED | HIGH | ₹3,968,100.00 |
| 30 | `client_04` | `client04@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_04` (ID 5) | VERIFIED | MEDIUM | ₹3,490,810.00 |
| 31 | `client_05` | `client05@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_05` (ID 6) | VERIFIED | HIGH | ₹2,936,175.00 |
| 32 | `client_06` | `client06@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_06` (ID 7) | VERIFIED | MEDIUM | ₹3,214,880.00 |
| 33 | `client_07` | `client07@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_07` (ID 8) | VERIFIED | LOW | ₹2,777,387.50 |
| 34 | `client_08` | `client08@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_08` (ID 9) | VERIFIED | MEDIUM | ₹2,622,612.50 |
| 35 | `client_09` | `client09@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_09` (ID 10) | VERIFIED | LOW | ₹2,385,441.00 |
| 36 | `client_10` | `client10@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_10` (ID 11) | VERIFIED | HIGH | ₹3,014,559.00 |
| 37 | `client_11` | `client11@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_11` (ID 12) | VERIFIED | MEDIUM | ₹1,982,480.00 |
| 38 | `client_12` | `client12@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_12` (ID 13) | VERIFIED | LOW | ₹1,817,520.00 |
| 39 | `client_13` | `client13@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_13` (ID 14) | VERIFIED | MEDIUM | ₹1,581,500.00 |
| 40 | `client_14` | `client14@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_14` (ID 15) | VERIFIED | HIGH | ₹1,518,500.00 |
| 41 | `client_15` | `client15@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_15` (ID 16) | VERIFIED | MEDIUM | ₹2,185,250.00 |
| 42 | `client_16` | `client16@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_16` (ID 17) | VERIFIED | LOW | ₹1,514,750.00 |
| 43 | `client_17` | `client17@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_17` (ID 18) | VERIFIED | MEDIUM | ₹1,385,920.00 |
| 44 | `client_18` | `client18@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_18` (ID 19) | VERIFIED | HIGH | ₹1,314,080.00 |
| 45 | `client_19` | `client19@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_19` (ID 20) | PENDING | LOW | ₹1,183,160.00 |
| 46 | `client_20` | `client20@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_20` (ID 21) | VERIFIED | MEDIUM | ₹1,837,960.00 |
| 47 | `client_21` | `client21@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_21` (ID 22) | VERIFIED | HIGH | ₹1,024,300.00 |
| 48 | `client_22` | `client22@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_22` (ID 23) | VERIFIED | MEDIUM | ₹973,875.00 |
| 49 | `client_23` | `client23@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_23` (ID 24) | PENDING | LOW | ₹810,890.00 |
| 50 | `client_24` | `client24@mail.com` | `Client#2026Pass!` | CLIENT | `ACTIVE` | `trader_24` (ID 25) | VERIFIED | MEDIUM | ₹780,200.00 |
| 51 | `client_25` | `client25@mail.com` | `Suspended#2026!` | CLIENT | `SUSPENDED` | `trader_25` (ID 26) | REJECTED | HIGH | ₹500,000.00 |

---

## 5. Summary of Account States

- **Total Seeded Users:** 51
- **System Administrator:** 1 (`sysadmin`, Active)
- **Active Traders:** 24 (`trader_01` to `trader_24`)
- **Suspended Trader:** 1 (`trader_25`)
- **Active Clients:** 24 (`client_01` to `client_24`)
- **Suspended Client:** 1 (`client_25`)
- **Total Active Accounts:** 49 (1 Admin + 24 Traders + 24 Clients)
- **Total Suspended Accounts:** 2 (`trader_25`, `client_25`)
