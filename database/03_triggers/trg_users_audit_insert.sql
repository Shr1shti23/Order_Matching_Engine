DELIMITER $$

CREATE TRIGGER trg_users_audit_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (actor_user_id, action_type, entity_type, entity_id, details)
    VALUES (
        NEW.created_by,
        'CREATE',
        'users',
        NEW.user_id,
        JSON_OBJECT('username', NEW.username, 'role_id', NEW.role_id)
    );
END$$

DELIMITER ;
