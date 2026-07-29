# Seed Credentials Reference (DEV / TEST ONLY)

> [!WARNING]
> **DEVELOPMENT AND TESTING ONLY**: The credentials listed in this file are strictly for local development, integration testing, and initial system bootstrapping. **NEVER** use, deploy, or reference these credentials in staging or production environments.

For the full detailed account matrix including user IDs, email addresses, wallets, and risk profiles, see [Seed_User_Credentials.md](file:///c:/Users/zshri/DSA%20project%201/Bank-Trading-System-/Seed_User_Credentials.md).

---

## 1. System Administrator (1 Account)

| Username | Default Password | Role | Account Status | Force Password Reset |
|---|---|---|---|---|
| `sysadmin` | `sysadmin` | Administrator (Role 1) | `ACTIVE` | `false` |

---

## 2. Seeded Trader Accounts (25 Accounts)

| Username | Default Password | Role | Account Status | Department / Code |
|---|---|---|---|---|
| `trader_01` .. `trader_24` | `Trader#2026Pass!` | Trader (Role 2) | `ACTIVE` | EMP-001 to EMP-024 |
| `trader_25` | `Suspended#2026!` | Trader (Role 2) | `SUSPENDED` | EMP-025 (Derivatives) |

---

## 3. Seeded Client Accounts (25 Accounts)

| Username | Default Password | Role | Account Status | Assigned Trader |
|---|---|---|---|---|
| `client_01` .. `client_24` | `Client#2026Pass!` | Client (Role 3) | `ACTIVE` | `trader_01` to `trader_24` |
| `client_25` | `Suspended#2026!` | Client (Role 3) | `SUSPENDED` | `trader_25` |

---

## 4. Argon2id Hash Configuration

All passwords above are stored in the database exclusively as **Argon2id** hashes with OWASP-aligned security parameters:
- **Variant:** Argon2id (`$argon2id$`)
- **Memory Cost:** 19,456 KB (19 MiB)
- **Time Cost (Iterations):** 2
- **Parallelism:** 1 thread
- **Salt Length:** 16 bytes (Cryptographically random, per-user salt)
- **Hash Output Length:** 32 bytes
