-- =====================================================================
-- BANK TRADING PLATFORM — DATABASE REBUILD SCRIPT
-- =====================================================================
-- Drops the database and performs a fresh installation.
-- Usage: mysql -u username -p < rebuild.sql
-- =====================================================================

DROP DATABASE IF EXISTS trading_platform;

SOURCE install.sql;
