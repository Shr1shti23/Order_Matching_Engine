USE trading_platform;

-- Disable FK checks so we can insert in order
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- LIVE (PENDING) ORDERS to populate the Order Book
-- =====================================================================

-- For AAPL (instrument_id = 1)
-- Let's put some resting SELL LIMIT orders so a BUY MARKET order can fill against them.
-- Divya (client_id 10, trader_id 5) selling 50 AAPL at 176.00
INSERT INTO orders (client_id, trader_id, instrument_id, side, order_type, time_in_force, price, original_qty, remaining_qty, status)
VALUES (10, 5, 1, 'SELL', 'LIMIT', 'DAY', 176.0000, 50, 50, 'PENDING');

-- Sanjay (client_id 7, trader_id 2) selling 100 AAPL at 176.50
INSERT INTO orders (client_id, trader_id, instrument_id, side, order_type, time_in_force, price, original_qty, remaining_qty, status)
VALUES (7, 2, 1, 'SELL', 'LIMIT', 'DAY', 176.5000, 100, 100, 'PENDING');

-- Let's put some resting BUY LIMIT orders so a SELL MARKET order can fill against them.
-- Neha (client_id 8, trader_id 3) buying 100 AAPL at 175.00
INSERT INTO orders (client_id, trader_id, instrument_id, side, order_type, time_in_force, price, original_qty, remaining_qty, status)
VALUES (8, 3, 1, 'BUY', 'LIMIT', 'DAY', 175.0000, 100, 100, 'PENDING');

-- For MSFT (instrument_id = 2)
-- Divya selling 50 MSFT at 425.00
INSERT INTO orders (client_id, trader_id, instrument_id, side, order_type, time_in_force, price, original_qty, remaining_qty, status)
VALUES (10, 5, 2, 'SELL', 'LIMIT', 'DAY', 425.0000, 50, 50, 'PENDING');

-- Neha buying 50 MSFT at 420.00
INSERT INTO orders (client_id, trader_id, instrument_id, side, order_type, time_in_force, price, original_qty, remaining_qty, status)
VALUES (8, 3, 2, 'BUY', 'LIMIT', 'DAY', 420.0000, 50, 50, 'PENDING');

SET FOREIGN_KEY_CHECKS = 1;
