# Database Seeding Instructions for Order Matching Engine

This document provides step-by-step instructions for executing SQL scripts to set up, seed, and reset the **`order_matching_engine`** database for development, testing, and demonstration.

---

## 1. Overview of Seed Datasets

The project database folder is structured into ordered modules:

```text
database/
├── SEED_DATA_INSTRUCTIONS.md           <-- You are here!
├── install.sql                         # Sequential master installation script
├── rebuild.sql                         # Drops database & executes install.sql
├── reset_database.sql                  # Resets transactional tables (preserves users & instruments)
├── uninstall.sql                       # Drops order_matching_engine database
│
├── 00_database/
│   └── 00_create_db.sql                # Creates 'order_matching_engine' DB with utf8mb4 encoding
├── 01_tables/                          # 20 relational table definitions
├── 03_triggers/                        # System audit & rule enforcement triggers
├── 04_views/                           # Order book, dashboard, and portfolio views
├── 07_seed/                            # Core metadata & bootstrap accounts
│   ├── 01_roles_permissions.sql        # ADMIN, TRADER, CLIENT roles & permissions
│   ├── 02_system_admin.sql             # Sysadmin account (sysadmin / sysadmin)
│   └── 03_instruments.sql              # 8 tradable stock instruments (AAPL, MSFT, etc.)
└── 08_test_data/                       # Realistic test dataset
    ├── 01_traders_clients.sql          # 25 Traders, 25 Clients, 25 Wallets, 25 Assignments
    ├── 02_wallets_holdings_trades.sql  # Executed trades, holdings, wallet & holding logs, price history
    └── 03_active_orders.sql            # Cancelled orders, live Order Book (BIDs/ASKs), audit logs
```

---

## 2. Quick-Start: Complete Rebuild & Seed

To perform a clean installation with the complete seed dataset:

### Option A: Via Command Line (PowerShell / Terminal)

```powershell
cd "c:\Users\zshri\DSA project 1\Bank-Trading-System-\database"
mysql -u root -p < rebuild.sql
```

*(Enter your MySQL password when prompted.)*

### Option B: Via MySQL Interactive Shell

```sql
mysql -u root -p
mysql> cd "c:/Users/zshri/DSA project 1/Bank-Trading-System-/database";
mysql> SOURCE rebuild.sql;
```

### Option C: Via MySQL Workbench / DBeaver GUI

1. Open your database GUI tool and connect to MySQL.
2. Open file: `database/rebuild.sql`.
3. Click **Execute / Run Script** (⚡ icon).

---

## 3. Account Summary & Login Credentials

Every account password is encrypted using **Argon2id** (`m=19456, t=2, p=1`). No plaintext passwords are stored.

| Account Type | Count | Status | Usernames | Default Password |
|---|---|---|---|---|
| **System Admin** | 1 | `ACTIVE` | `sysadmin` | `sysadmin` |
| **Active Traders** | 24 | `ACTIVE` | `trader_01` to `trader_24` | `Trader#2026Pass!` |
| **Suspended Trader** | 1 | `SUSPENDED` | `trader_25` | `Suspended#2026!` |
| **Active Clients** | 24 | `ACTIVE` | `client_01` to `client_24` | `Client#2026Pass!` |
| **Suspended Client** | 1 | `SUSPENDED` | `client_25` | `Suspended#2026!` |

> [!NOTE]
> For complete account details (emails, employee codes, wallet balances, risk profiles, trader assignments), refer to [Seed_User_Credentials.md](file:///c:/Users/zshri/DSA%20project%201/Bank-Trading-System-/Seed_User_Credentials.md).

---

## 4. Integrity & Verification Instructions

After running `rebuild.sql`, you can verify database population via MySQL CLI:

```sql
USE order_matching_engine;

-- 1. Check user accounts count (should be 51 total: 1 admin + 25 traders + 25 clients)
SELECT role_id, status, COUNT(*) FROM users GROUP BY role_id, status;

-- 2. Check active order book orders (should show resting BUY and SELL orders)
SELECT * FROM v_order_book;

-- 3. Check administrative dashboard metrics view
SELECT * FROM v_admin_dashboard;
```
