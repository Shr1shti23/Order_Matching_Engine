DELIMITER $$

CREATE TRIGGER trg_users_audit_update
AFTER UPDATE ON users
FOR EACH ROW
BEGIN
    IF NEW.status <> OLD.status THEN
        INSERT INTO audit_log (actor_user_id, action_type, entity_type, entity_id, details)
        VALUES (
            NEW.user_id,
            IF(NEW.status = 'SUSPENDED', 'SUSPEND', 'UPDATE'),
            'users',
            NEW.user_id,
            JSON_OBJECT('old_status', OLD.status, 'new_status', NEW.status)
        );
    END IF;
END$$

DELIMITER ;
