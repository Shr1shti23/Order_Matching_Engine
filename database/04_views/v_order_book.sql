CREATE VIEW v_order_book AS
SELECT
    instrument_id,
    side,
    price,
    SUM(remaining_qty)  AS qty_at_price,
    MIN(created_at)     AS earliest_order
FROM orders
WHERE status IN ('PENDING', 'PARTIALLY_FILLED')
GROUP BY instrument_id, side, price;
