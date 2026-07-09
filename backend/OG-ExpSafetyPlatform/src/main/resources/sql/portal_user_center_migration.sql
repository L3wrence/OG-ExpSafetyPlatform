-- Unified portal and user center migration.
-- MySQL 8 repeatable script; keeps existing tables and data compatible.

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(IN table_name_in VARCHAR(64), IN column_name_in VARCHAR(64), IN column_def_in TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = table_name_in
           AND column_name = column_name_in
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', table_name_in, ' ADD COLUMN ', column_def_in);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_column_if_missing('t_user', 'avatar_url', 'avatar_url VARCHAR(500) NULL DEFAULT NULL AFTER phone');
CALL add_column_if_missing('t_user', 'major', 'major VARCHAR(100) NULL DEFAULT NULL AFTER avatar_url');
CALL add_column_if_missing('t_user', 'class_name', 'class_name VARCHAR(100) NULL DEFAULT NULL AFTER major');
CALL add_column_if_missing('t_user', 'email', 'email VARCHAR(100) NULL DEFAULT NULL AFTER class_name');
CALL add_column_if_missing('t_lab_time_slot', 'slot_date', 'slot_date DATE GENERATED ALWAYS AS (`date`) STORED');

DROP PROCEDURE IF EXISTS add_column_if_missing;

CREATE TABLE IF NOT EXISTS t_portal_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    target_role VARCHAR(50) DEFAULT 'ALL',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    status TINYINT DEFAULT 1,
    publish_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    expire_time DATETIME NULL,
    create_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    KEY idx_notice_role_status_time (target_role, status, publish_time),
    KEY idx_notice_deleted (deleted)
);

CREATE TABLE IF NOT EXISTS t_portal_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    biz_type VARCHAR(50),
    biz_id BIGINT,
    path VARCHAR(255),
    read_flag TINYINT DEFAULT 0,
    read_time DATETIME NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    KEY idx_message_user_read_time (user_id, read_flag, create_time),
    KEY idx_message_biz (biz_type, biz_id)
);

CREATE TABLE IF NOT EXISTS t_recent_visit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    path VARCHAR(255) NOT NULL,
    module VARCHAR(50),
    visit_count INT DEFAULT 1,
    last_visit_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_recent_user_path (user_id, path),
    KEY idx_recent_user_time (user_id, last_visit_time)
);

CREATE TABLE IF NOT EXISTS t_user_shortcut (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(80) NOT NULL,
    path VARCHAR(255) NOT NULL,
    icon VARCHAR(50),
    sort INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_shortcut_user_sort (user_id, sort)
);

CREATE TABLE IF NOT EXISTS t_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    username VARCHAR(50),
    module VARCHAR(80) NOT NULL,
    action VARCHAR(80) NOT NULL,
    content VARCHAR(500),
    result VARCHAR(20) DEFAULT 'SUCCESS',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_operation_user_time (user_id, create_time),
    KEY idx_operation_module_time (module, create_time)
);

INSERT INTO t_permission (name, code, type, parent_id, path, icon, sort)
SELECT p.name, p.code, p.type, p.parent_id, p.path, p.icon, p.sort
FROM (
    SELECT '门户首页' AS name, 'portal:view' AS code, 2 AS type, 0 AS parent_id, NULL AS path, NULL AS icon, 501 AS sort
    UNION ALL SELECT '个人资料维护', 'profile:update', 2, 0, NULL, NULL, 502
    UNION ALL SELECT '密码修改', 'profile:password', 2, 0, NULL, NULL, 503
    UNION ALL SELECT '消息提醒', 'portal:message', 2, 0, NULL, NULL, 504
    UNION ALL SELECT '全站搜索', 'portal:search', 2, 0, NULL, NULL, 505
) p
WHERE NOT EXISTS (SELECT 1 FROM t_permission tp WHERE tp.code = p.code);

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p ON p.code IN ('portal:view', 'profile:update', 'profile:password', 'portal:message', 'portal:search')
WHERE r.role_code IN ('ADMIN', 'TEACHER', 'STUDENT', 'USER')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_permission rp
       WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO t_portal_notice (title, content, target_role, priority, status, publish_time)
SELECT '安全考核平台统一门户已上线',
       '请完善个人资料并关注首页待办、消息提醒和学习日历。',
       'ALL', 'HIGH', 1, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM t_portal_notice WHERE title = '安全考核平台统一门户已上线' AND deleted = 0
);

INSERT INTO t_portal_notice (title, content, target_role, priority, status, publish_time)
SELECT '实验前请完成必学资源和安全考试',
       '未完成必学资源或未通过安全考试的学生将无法预约对应实验。',
       'STUDENT', 'MEDIUM', 1, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM t_portal_notice WHERE title = '实验前请完成必学资源和安全考试' AND deleted = 0
);

INSERT INTO t_portal_message (user_id, title, content, biz_type, path, read_flag)
SELECT u.id, '欢迎使用统一门户', '这里会展示你的待办、公告、消息和学习日程。', 'PORTAL', '/home', 0
FROM t_user u
WHERE NOT EXISTS (
    SELECT 1 FROM t_portal_message m
     WHERE m.user_id = u.id AND m.title = '欢迎使用统一门户' AND m.deleted = 0
);
