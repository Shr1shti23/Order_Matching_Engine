USE order_matching_engine;

-- =====================================================================
-- ORDER BOOK STATE — Cancelled and Pending (Live) Orders
-- =====================================================================
-- Orders 33–34: CANCELLED  (resting orders that were never matched and
--               then withdrawn by the trader)
-- Orders 35–40: PENDING    (current live orders sitting in the order
--               book, available for matching-engine consumption)
--
-- Instrument IDs: 1=AAPL 2=MSFT 3=TSLA 4=GOOGL 5=NVDA 6=AMZN 7=INFY 8=RELIANCE
-- client_id/trader_id reminder:
--   client_N → uid = N+26;  trader_N → uid = N+1
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- CANCELLED ORDERS (orders 33–34)
-- =====================================================================
INSERT INTO orders (order_id, client_id, trader_id, instrument_id, side, order_type,
                    time_in_force, price, original_qty, remaining_qty, status,
                    created_at, updated_at)
VALUES
-- c09 (uid=35) tried to SELL 100 TSLA at 195.00 — too far from market, withdrawn
(33, 35, 10, 3, 'SELL', 'LIMIT', 'DAY', 195.0000, 100, 100, 'CANCELLED',
     '2026-07-25 09:05:00', '2026-07-25 14:58:00'),

-- c11 (uid=37) tried to BUY 50 GOOGL at 148.00 — bid too low, cancelled by trader
(34, 37, 12, 4, 'BUY',  'LIMIT', 'DAY', 148.0000,  50,  50, 'CANCELLED',
     '2026-07-25 10:30:00', '2026-07-25 14:59:00');

-- =====================================================================
-- PENDING / LIVE ORDERS (orders 35–40) — current order book state
-- =====================================================================
INSERT INTO orders (order_id, client_id, trader_id, instrument_id, side, order_type,
                    time_in_force, price, original_qty, remaining_qty, status,
                    created_at, updated_at)
VALUES
-- BID side (resting BUY limit orders)
-- c01 (uid=27) GTC BUY 50 MSFT @ 418.00 — willing to buy below last price 421.00
(35, 27,  2, 2, 'BUY',  'LIMIT', 'GTC', 418.0000,  50,  50, 'PENDING',
     '2026-07-28 09:35:00', '2026-07-28 09:35:00'),

-- c09 (uid=35) DAY BUY 100 AAPL @ 174.00 — conservative bid below last price 176.00
(36, 35, 10, 1, 'BUY',  'LIMIT', 'DAY', 174.0000, 100, 100, 'PENDING',
     '2026-07-28 09:50:00', '2026-07-28 09:50:00'),

-- c11 (uid=37) GTC BUY 20 NVDA @ 482.00 — building more NVDA position
(37, 37, 12, 5, 'BUY',  'LIMIT', 'GTC', 482.0000,  20,  20, 'PENDING',
     '2026-07-28 10:10:00', '2026-07-28 10:10:00'),

-- ASK side (resting SELL limit orders)
-- c01 (uid=27) DAY SELL 30 AAPL @ 178.00 — offering AAPL above last price 176.00
(38, 27,  2, 1, 'SELL', 'LIMIT', 'DAY', 178.0000,  30,  30, 'PENDING',
     '2026-07-28 09:38:00', '2026-07-28 09:38:00'),

-- c17 (uid=43) GTC SELL 40 AAPL @ 179.00 — second ask level for AAPL
(39, 43, 18, 1, 'SELL', 'LIMIT', 'GTC', 179.0000,  40,  40, 'PENDING',
     '2026-07-28 10:05:00', '2026-07-28 10:05:00'),

-- c03 (uid=29) DAY SELL 25 MSFT @ 422.00 — trimming MSFT above last price 421.00
(40, 29,  4, 2, 'SELL', 'LIMIT', 'DAY', 422.0000,  25,  25, 'PENDING',
     '2026-07-28 10:15:00', '2026-07-28 10:15:00');

-- =====================================================================
-- ORDER EVENTS — lifecycle for cancelled and pending orders
-- =====================================================================
INSERT INTO order_events (order_id, event_type, previous_status, new_status,
                          quantity_changed, price, actor_user_id, created_at)
VALUES
-- Cancelled orders: CREATED then CANCELLED
(33, 'CREATED',   NULL,      'PENDING',   NULL, 195.0000, 10, '2026-07-25 09:05:00'),
(33, 'CANCELLED', 'PENDING', 'CANCELLED', 100,  195.0000, 10, '2026-07-25 14:58:00'),
(34, 'CREATED',   NULL,      'PENDING',   NULL, 148.0000, 12, '2026-07-25 10:30:00'),
(34, 'CANCELLED', 'PENDING', 'CANCELLED',  50,  148.0000, 12, '2026-07-25 14:59:00'),

-- Live orders: CREATED event only (no match yet)
(35, 'CREATED', NULL, 'PENDING', NULL, 418.0000,  2, '2026-07-28 09:35:00'),
(36, 'CREATED', NULL, 'PENDING', NULL, 174.0000, 10, '2026-07-28 09:50:00'),
(37, 'CREATED', NULL, 'PENDING', NULL, 482.0000, 12, '2026-07-28 10:10:00'),
(38, 'CREATED', NULL, 'PENDING', NULL, 178.0000,  2, '2026-07-28 09:38:00'),
(39, 'CREATED', NULL, 'PENDING', NULL, 179.0000, 18, '2026-07-28 10:05:00'),
(40, 'CREATED', NULL, 'PENDING', NULL, 422.0000,  4, '2026-07-28 10:15:00');

-- =====================================================================
-- AUDIT LOG — supplemental manual entries
-- (trg_users_audit_insert, trg_orders_audit_insert, and
--  trg_trades_after_insert auto-populate audit_log during seed inserts.
--  We add a few extra entries below to demonstrate LOGIN and SUSPEND
--  audit history.)
-- =====================================================================
INSERT INTO audit_log (actor_user_id, action_type, entity_type, entity_id, details, created_at)
VALUES
-- Successful admin logins
(1, 'LOGIN',   'users',  1, JSON_OBJECT('ip', '192.168.1.10', 'result', 'SUCCESS'), '2026-07-20 08:00:00'),
(1, 'LOGIN',   'users',  1, JSON_OBJECT('ip', '192.168.1.10', 'result', 'SUCCESS'), '2026-07-25 08:30:00'),
(1, 'LOGIN',   'users',  1, JSON_OBJECT('ip', '192.168.1.10', 'result', 'SUCCESS'), '2026-07-29 08:15:00'),
-- Admin suspends trader_25 and client_25
(1, 'SUSPEND', 'users', 26, JSON_OBJECT('reason', 'Regulatory review — account frozen pending AML investigation'), '2026-07-24 11:00:00'),
(1, 'SUSPEND', 'users', 51, JSON_OBJECT('reason', 'Linked to suspended trader; account frozen per compliance policy'), '2026-07-24 11:05:00'),
-- Sample trader logins
(2,  'LOGIN', 'users',  2, JSON_OBJECT('ip', '10.0.1.11', 'result', 'SUCCESS'), '2026-07-28 09:30:00'),
(3,  'LOGIN', 'users',  3, JSON_OBJECT('ip', '10.0.1.12', 'result', 'SUCCESS'), '2026-07-28 09:31:00'),
(4,  'LOGIN', 'users',  4, JSON_OBJECT('ip', '10.0.1.13', 'result', 'SUCCESS'), '2026-07-28 09:32:00'),
-- Cancelled order audit (trader actions)
(10, 'ORDER_CANCELLED', 'orders', 33, JSON_OBJECT('reason', 'Market moved away from limit price'), '2026-07-25 14:58:00'),
(12, 'ORDER_CANCELLED', 'orders', 34, JSON_OBJECT('reason', 'Client changed strategy'), '2026-07-25 14:59:00');

SET FOREIGN_KEY_CHECKS = 1;
