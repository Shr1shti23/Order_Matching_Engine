# Database — Bank Trading Platform

## Files

| File | Purpose |
|---|---|
| `schema.sql` | Creates the `trading_platform` database, all tables, indexes, triggers, and views |
| `seed.sql` | Populates roles, permissions, 1 admin, 5 traders, 5 clients, wallets, and trader-client assignments |

## Run Order

Always run the schema first, then the seed file.

### Option A — MySQL CLI

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/seed.sql
```

### Option B — MySQL CLI (interactive)

```sql
SOURCE db/schema.sql;
SOURCE db/seed.sql;
```

### Option C — MySQL Workbench

1. Open `db/schema.sql` → Run (⚡)
2. Open `db/seed.sql`   → Run (⚡)

---

## Seed Accounts

> **Passwords are empty strings in the seed file.**  
> Before going to production, replace every `password_hash` value with a real
> Argon2id encoded string produced by the Java application.

### Admin

| username | email | role |
|---|---|---|
| `sysadmin` | sysadmin@bank.local | ADMIN |

### Traders

| username | email | department |
|---|---|---|
| `trader_arjun`  | arjun.mehta@bank.local   | Equities |
| `trader_priya`  | priya.sharma@bank.local  | Derivatives |
| `trader_rohan`  | rohan.verma@bank.local   | Fixed Income |
| `trader_anika`  | anika.gupta@bank.local   | FX & Commodities |
| `trader_vikram` | vikram.nair@bank.local   | Equities |

### Clients

| username | email | KYC | Risk |
|---|---|---|---|
| `client_sanjay` | sanjay.kapoor@mail.com  | VERIFIED | HIGH |
| `client_neha`   | neha.joshi@mail.com     | VERIFIED | MEDIUM |
| `client_rahul`  | rahul.bansal@mail.com   | PENDING  | LOW |
| `client_divya`  | divya.iyer@mail.com     | VERIFIED | MEDIUM |
| `client_karan`  | karan.malhotra@mail.com | REJECTED | HIGH |

### Trader → Client Assignments

| Trader | Client |
|---|---|
| trader_arjun  | client_sanjay |
| trader_priya  | client_neha   |
| trader_rohan  | client_rahul  |
| trader_anika  | client_divya  |
| trader_vikram | client_karan  |

---

## Notes

- The trigger `trg_users_enforce_creator` blocks any user creation where
  the creator is not an ADMIN.  The seed admin is the only row ever allowed to
  have `created_by = NULL`, and the trigger permits this only when the `users`
  table is completely empty.
- All client wallets start at **₹ 0.00**.  Fund them through the application's
  deposit flow after onboarding.
- The `@admin_id` / `@t1`…`@c5` session variables in `seed.sql` rely on
  `AUTO_INCREMENT` being contiguous.  If you reset the table between runs,
  drop and recreate the database first so IDs start from 1 again.
