-- P2 admin notice/log and reminder migration.
-- Applicable version: after portal_user_center_migration.sql and online_learning_optimization_migration.sql.
-- Execution date: 2026-07-07.
-- Repeatable: yes, all DDL/DML is guarded.
-- Rollback: remove inserted permissions/role bindings manually if needed; keep data tables and messages.

DROP PROCEDURE IF EXISTS add_index_if_missing;
DELIMITER //
CREATE PROCEDURE add_index_if_missing(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index
    ) THEN
        SET @ddl = p_ddl;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_index_if_missing(
    't_portal_message',
    'idx_message_user_biz',
    'CREATE INDEX idx_message_user_biz ON t_portal_message(user_id, biz_type, biz_id, deleted)'
);

CALL add_index_if_missing(
    't_portal_notice',
    'idx_notice_admin_query',
    'CREATE INDEX idx_notice_admin_query ON t_portal_notice(deleted, status, target_role, priority, publish_time)'
);

CALL add_index_if_missing(
    't_operation_log',
    'idx_operation_log_query',
    'CREATE INDEX idx_operation_log_query ON t_operation_log(create_time, module, action, result, user_id)'
);

DROP PROCEDURE IF EXISTS add_index_if_missing;

INSERT INTO t_permission (name, code, type, parent_id, path, icon, sort, create_time)
SELECT p.name, p.code, 2, 0, NULL, NULL, p.sort, NOW()
FROM (
    SELECT '公告管理' AS name, 'portal:notice:manage' AS code, 256 AS sort
    UNION ALL SELECT '操作日志查看', 'operation-log:view', 257
) p
WHERE NOT EXISTS (SELECT 1 FROM t_permission tp WHERE tp.code = p.code);

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p ON p.code IN ('portal:notice:manage', 'operation-log:view')
WHERE r.role_code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
        FROM t_role_permission rp
       WHERE rp.role_id = r.id
         AND rp.permission_id = p.id
  );
