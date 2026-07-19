USE trading_platform;

-- Disable FK checks so we can insert in order and set up historical states if needed
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- INITIAL HOLDINGS SETUPS (pre-trade or post-trade snapshot)
-- =====================================================================
-- Client 7 (Sanjay) holds 100 AAPL shares
INSERT INTO holdings (client_id, instrument_id, quantity, avg_buy_price)
VALUES (7, 1, 100, 175.5000);

-- Client 8 (Neha) holds 100 AAPL and 50 MSFT shares
INSERT INTO holdings (client_id, instrument_id, quantity, avg_buy_price)
VALUES 
    (8, 1, 100, 170.0000),
    (8, 2, 50, 420.2000);

-- Client 10 (Divya) holds 150 MSFT shares
INSERT INTO holdings (client_id, instrument_id, quantity, avg_buy_price)
VALUES (10, 2, 150, 410.0000);

-- =====================================================================
-- HISTORICAL ORDERS
-- =====================================================================
-- Trade 1: Sanjay buys 100 AAPL from Neha
INSERT INTO orders (order_id, client_id, trader_id, instrument_id, side, order_type, price, original_qty, remaining_qty, status)
VALUES
    (1, 7, 2, 1, 'BUY', 'LIMIT', 175.5000, 100, 0, 'FILLED'),
    (2, 8, 3, 1, 'SELL', 'LIMIT', 175.5000, 100, 0, 'FILLED');

-- Trade 2: Neha buys 50 MSFT from Divya
INSERT INTO orders (order_id, client_id, trader_id, instrument_id, side, order_type, price, original_qty, remaining_qty, status)
VALUES
    (3, 8, 3, 2, 'BUY', 'LIMIT', 420.2000, 50, 0, 'FILLED'),
    (4, 10, 5, 2, 'SELL', 'LIMIT', 420.2000, 50, 0, 'FILLED');

-- =====================================================================
-- HISTORICAL TRADES
-- =====================================================================
INSERT INTO trades (trade_id, instrument_id, buy_order_id, sell_order_id, price, quantity)
VALUES
    (1, 1, 1, 2, 175.5000, 100),
    (2, 2, 3, 4, 420.2000, 50);

-- =====================================================================
-- WALLET TRANSACTIONS
-- =====================================================================
-- Wallets start with adjusted balances from the trades
-- Sanjay (Client 7) paid: 100 * 175.50 = 17,550.00 INR
-- Neha (Client 8) received 17,550.00 INR (AAPL sell) and paid 50 * 420.20 = 21,010.00 INR (MSFT buy) -> Net: -3,460.00 INR
-- Divya (Client 10) received 21,010.00 INR

INSERT INTO wallet_transactions (wallet_id, transaction_type, amount, balance_after, trade_id, reference)
VALUES
    (1, 'TRADE_DEBIT', -17550.00, 482450.00, 1, 'Buy 100 AAPL'),
    (2, 'TRADE_CREDIT', 17550.00, 767550.00, 1, 'Sell 100 AAPL'),
    (2, 'TRADE_DEBIT', -21010.00, 746540.00, 2, 'Buy 50 MSFT'),
    (4, 'TRADE_CREDIT', 21010.00, 1021010.00, 2, 'Sell 50 MSFT');

-- Adjust cash balances in wallets table to match final balances
UPDATE wallets SET cash_balance = 482450.00 WHERE client_id = 7;
UPDATE wallets SET cash_balance = 746540.00 WHERE client_id = 8;
UPDATE wallets SET cash_balance = 1021010.00 WHERE client_id = 10;

-- =====================================================================
-- HOLDING TRANSACTIONS
-- =====================================================================
INSERT INTO holding_transactions (client_id, instrument_id, trade_id, transaction_type, quantity, price)
VALUES
    (7, 1, 1, 'BUY', 100, 175.5000),
    (8, 1, 1, 'SELL', -100, 175.5000),
    (8, 2, 2, 'BUY', 50, 420.2000),
    (10, 2, 2, 'SELL', -50, 420.2000);

-- =====================================================================
-- PRICE HISTORY
-- =====================================================================
INSERT INTO price_history (instrument_id, trade_price, traded_volume)
VALUES
    (1, 175.5000, 100),
    (2, 420.2000, 50);

SET FOREIGN_KEY_CHECKS = 1;
