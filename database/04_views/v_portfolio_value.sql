CREATE VIEW v_portfolio_value AS
SELECT
    h.client_id,
    h.instrument_id,
    i.symbol,
    h.quantity,
    h.avg_buy_price,
    i.last_traded_price,
    (h.quantity * i.last_traded_price)                        AS current_value,
    (h.quantity * (i.last_traded_price - h.avg_buy_price))    AS unrealized_pnl
FROM holdings   h
JOIN instruments i ON h.instrument_id = i.instrument_id;
