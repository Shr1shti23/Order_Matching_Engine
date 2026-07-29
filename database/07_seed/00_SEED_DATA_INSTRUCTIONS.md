# Seed Data Execution Instructions

This document provides step-by-step instructions for running the database seed scripts to populate core platform metadata, system administrator credentials, tradable instruments, and initial test accounts for the **Bank Trading Platform / Order Matching Engine**.

---

## 1. Overview of Seed Files

The seed files are located in the `database/07_seed/` directory and are intended to be executed sequentially after table creation:

| File | Order | Description | Dependency |
|---|---|---|---|
| `00_SEED_DATA_INSTRUCTIONS.md` | `00` | **Instructions Document** (You are here) | N/A |
| `01_roles_permissions.sql` | `01` | Core RBAC Roles (`ADMIN`, `TRADER`, `CLIENT`) & Permissions mapping | Tables `roles`, `permissions`, `role_permissions` |
| `02_system_admin.sql` | `02` | Sysadmin bootstrap account (`sysadmin`) hashed with Argon2id | Table `users` & Role `1 (ADMIN)` |
| `03_instruments.sql` | `03` | Tradable market assets (Equities: AAPL, MSFT, TSLA, GOOGL, NVDA, AMZN, INFY, RELIANCE) | Table `instruments` & User `1 (sysadmin)` |

---

## 2. Quick-Start: Automated Master Execution

To execute all seed scripts along with database schema setup and test datasets in a single command, run the master script from the repository root:

### Option A: Command Line (PowerShell / Terminal)

```powershell
# Navigate to database folder
cd "database"

# Execute master rebuild (Drops & rebuilds schema + seed + test data)
mysql -u root -p < rebuild.sql
```

### Option B: MySQL Interactive Shell

```sql
mysql -u root -p
mysql> USE order_matching_engine;
mysql> SOURCE database/07_seed/01_roles_permissions.sql;
mysql> SOURCE database/07_seed/02_system_admin.sql;
mysql> SOURCE database/07_seed/03_instruments.sql;
```

### Option C: Database Management GUI (MySQL Workbench / DBeaver)

1. Open your database tool and connect to your local MySQL instance.
2. Ensure schema `order_matching_engine` is created.
3. Open and run each file in sequence:
   1. `database/07_seed/01_roles_permissions.sql`
   2. `database/07_seed/02_system_admin.sql`
   3. `database/07_seed/03_instruments.sql`

---

## 3. Seed Credentials & Security Parameters

All password hashes are generated using **Argon2id** (`m=19456, t=2, p=1`, 16-byte random salt, 32-byte hash) conforming to OWASP password storage recommendations.

### Default Seed Accounts

| Role | Username | Default Password | Status | Force Reset |
|---|---|---|---|---|
| **System Admin** | `sysadmin` | `sysadmin` | `ACTIVE` | `false` |
| **Trader Accounts** | `trader_01` to `trader_24` | `Trader#2026Pass!` | `ACTIVE` | `false` |
| **Suspended Trader** | `trader_25` | `Suspended#2026!` | `SUSPENDED` | `false` |
| **Client Accounts** | `client_01` to `client_24` | `Client#2026Pass!` | `ACTIVE` | `false` |
| **Suspended Client** | `client_25` | `Suspended#2026!` | `SUSPENDED` | `false` |

---

## 4. Verification Queries

Run the following SQL queries to verify that the seed data has been loaded cleanly into the database:

```sql
USE order_matching_engine;

-- 1. Verify Role and Permission Mappings
SELECT r.role_name, p.permission_name
FROM role_permissions rp
JOIN roles r ON rp.role_id = r.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
ORDER BY r.role_id, p.permission_id;

-- 2. Verify System Admin Account
SELECT user_id, username, email, role_id, status 
FROM users 
WHERE username = 'sysadmin';

-- 3. Verify Tradable Instruments
SELECT instrument_id, symbol, name, instrument_type, last_traded_price, status 
FROM instruments;
```
