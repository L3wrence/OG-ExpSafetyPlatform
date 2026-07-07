-- Teaching resource and virtual experiment resource center migration.
-- MySQL 8 repeatable script; keeps existing t_resource and t_learning_record compatible.

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

CALL add_column_if_missing('t_resource', 'course_id', 'course_id BIGINT NULL AFTER id');
CALL add_column_if_missing('t_resource', 'knowledge_point', 'knowledge_point VARCHAR(150) NULL DEFAULT NULL AFTER resource_type');
CALL add_column_if_missing('t_resource', 'risk_type', 'risk_type VARCHAR(100) NULL DEFAULT NULL AFTER knowledge_point');
CALL add_column_if_missing('t_resource', 'tags', 'tags VARCHAR(300) NULL DEFAULT NULL AFTER risk_type');
CALL add_column_if_missing('t_resource', 'category', 'category VARCHAR(30) NOT NULL DEFAULT ''EXTENSION'' AFTER tags');
CALL add_column_if_missing('t_resource', 'description', 'description TEXT NULL AFTER category');
CALL add_column_if_missing('t_resource', 'original_filename', 'original_filename VARCHAR(255) NULL DEFAULT NULL AFTER file_path');
CALL add_column_if_missing('t_resource', 'content_type', 'content_type VARCHAR(100) NULL DEFAULT NULL AFTER original_filename');
CALL add_column_if_missing('t_resource', 'completion_rule', 'completion_rule VARCHAR(30) NOT NULL DEFAULT ''CONFIRM'' AFTER required_flag');
CALL add_column_if_missing('t_resource', 'min_study_seconds', 'min_study_seconds INT NOT NULL DEFAULT 0 AFTER completion_rule');
CALL add_column_if_missing('t_resource', 'min_progress', 'min_progress INT NOT NULL DEFAULT 100 AFTER min_study_seconds');
CALL add_column_if_missing('t_resource', 'open_time', 'open_time DATETIME NULL AFTER min_progress');
CALL add_column_if_missing('t_resource', 'close_time', 'close_time DATETIME NULL AFTER open_time');
CALL add_column_if_missing('t_resource', 'open_scope', 'open_scope VARCHAR(30) NOT NULL DEFAULT ''COURSE'' AFTER close_time');
CALL add_column_if_missing('t_resource', 'invalid_flag', 'invalid_flag TINYINT NOT NULL DEFAULT 0 AFTER open_scope');
CALL add_column_if_missing('t_resource', 'invalid_check_time', 'invalid_check_time DATETIME NULL AFTER invalid_flag');
CALL add_column_if_missing('t_resource', 'download_count', 'download_count INT NOT NULL DEFAULT 0 AFTER view_count');
CALL add_column_if_missing('t_resource', 'favorite_count', 'favorite_count INT NOT NULL DEFAULT 0 AFTER download_count');
CALL add_column_if_missing('t_resource', 'like_count', 'like_count INT NOT NULL DEFAULT 0 AFTER favorite_count');
CALL add_column_if_missing('t_resource', 'comment_count', 'comment_count INT NOT NULL DEFAULT 0 AFTER like_count');
CALL add_column_if_missing('t_resource', 'rating_avg', 'rating_avg DECIMAL(3,2) NOT NULL DEFAULT 0 AFTER comment_count');
CALL add_column_if_missing('t_resource', 'rating_count', 'rating_count INT NOT NULL DEFAULT 0 AFTER rating_avg');

DROP PROCEDURE IF EXISTS add_column_if_missing;

CREATE TABLE IF NOT EXISTS t_resource_interaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    favorite_flag TINYINT NOT NULL DEFAULT 0,
    like_flag TINYINT NOT NULL DEFAULT 0,
    rating DECIMAL(2,1) NULL,
    comment VARCHAR(500) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_resource_user_deleted (resource_id, user_id, deleted),
    KEY idx_resource_interaction_resource (resource_id),
    KEY idx_resource_interaction_user (user_id, favorite_flag)
);

UPDATE t_resource r
JOIN t_experiment e ON e.id = r.experiment_id
SET r.course_id = e.course_id
WHERE r.course_id IS NULL;

UPDATE t_resource
SET category = CASE WHEN required_flag = 1 THEN 'REQUIRED' ELSE 'EXTENSION' END
WHERE category IS NULL OR category = '';

UPDATE t_resource
SET completion_rule = CASE
        WHEN resource_type IN ('VIDEO', 'TEACHING_VIDEO', 'MICRO_COURSE', 'INSTRUMENT_VIDEO', 'HSE_VIDEO', 'AUDIO') THEN 'PROGRESS'
        ELSE 'CONFIRM'
    END,
    min_progress = CASE
        WHEN resource_type IN ('VIDEO', 'TEACHING_VIDEO', 'MICRO_COURSE', 'INSTRUMENT_VIDEO', 'HSE_VIDEO', 'AUDIO') THEN 80
        ELSE 100
    END
WHERE completion_rule IS NULL OR completion_rule = '';

INSERT INTO t_resource (
    course_id, experiment_id, title, resource_type, knowledge_point, risk_type, tags, category,
    description, url, file_size, required_flag, completion_rule, min_study_seconds, min_progress,
    status, sort, view_count
)
SELECT e.course_id, e.id, '虚拟仿真实验入口', 'VIRTUAL_SIMULATION', '实验流程训练', e.risk_types,
       '虚拟实验,操作动画,课前训练', 'EXTENSION',
       '用于课前熟悉实验流程和关键风险点的虚拟仿真入口。',
       'https://www.smartedu.cn/', 0, 0, 'CONFIRM', 0, 100, 1, 99, 0
FROM t_experiment e
WHERE e.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM t_resource r
      WHERE r.experiment_id = e.id AND r.resource_type = 'VIRTUAL_SIMULATION' AND r.deleted = 0
  )
LIMIT 1;
