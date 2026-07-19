DELIMITER $$

CREATE TRIGGER trg_trades_after_insert
AFTER INSERT ON trades
FOR EACH ROW
BEGIN
    UPDATE instruments
    SET    last_traded_price = NEW.price
    WHERE  instrument_id = NEW.instrument_id;

    INSERT INTO audit_log (actor_user_id, action_type, entity_type, entity_id, details)
    VALUES (
        NULL,
        'TRADE_EXECUTED',
        'trades',
        NEW.trade_id,
        JSON_OBJECT(
            'instrument_id', NEW.instrument_id,
            'price',         NEW.price,
            'quantity',      NEW.quantity
        )
    );
END$$

DELIMITER ;
