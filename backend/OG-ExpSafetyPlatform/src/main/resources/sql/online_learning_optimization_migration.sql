-- Online learning optimization migration.
-- Applicable version: first Codex optimization round, 2026-07-06.
-- Prerequisites: user_permission_schema.sql, experiment_teaching_schema.sql, exam_reservation_report_ai_schema.sql, exam_admission_migration.sql.
-- Repeatable: yes, guarded by information_schema checks where DDL may already exist.
-- Rollback: manually drop added columns/indexes only after confirming application code no longer uses them.

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;

DELIMITER //
CREATE PROCEDURE add_column_if_missing(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

CREATE PROCEDURE add_index_if_missing(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_definition TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_table, ' ADD ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_column_if_missing('t_exam_record', 'last_save_time', 'last_save_time DATETIME NULL AFTER end_time');
CALL add_column_if_missing('t_exam_record', 'final_grade_time', 'final_grade_time DATETIME NULL AFTER last_save_time');
CALL add_column_if_missing('t_learning_record', 'last_position_seconds', 'last_position_seconds INT NOT NULL DEFAULT 0 AFTER duration_seconds');
CALL add_column_if_missing('t_learning_record', 'note', 'note VARCHAR(1000) NULL AFTER last_position_seconds');
CALL add_index_if_missing('t_exam_answer', 'idx_exam_answer_record_question', 'INDEX idx_exam_answer_record_question(record_id, question_id)');
CALL add_index_if_missing('t_report', 'idx_report_student_experiment', 'INDEX idx_report_student_experiment(student_id, experiment_id)');

CREATE TABLE IF NOT EXISTS t_discussion_topic (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    experiment_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    is_anonymous TINYINT NOT NULL DEFAULT 0,
    is_featured TINYINT NOT NULL DEFAULT 0,
    reply_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_discussion_course(course_id),
    INDEX idx_discussion_experiment(experiment_id),
    INDEX idx_discussion_status_time(status, update_time)
);

CREATE TABLE IF NOT EXISTS t_discussion_reply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    topic_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_teacher_reply TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_discussion_reply_topic(topic_id)
);

CREATE TABLE IF NOT EXISTS t_report_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    experiment_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    schema_json LONGTEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_report_template_experiment(experiment_id)
);

CREATE TABLE IF NOT EXISTS t_report_rubric_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    experiment_id BIGINT NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    max_score INT NOT NULL,
    order_no INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_rubric_experiment(experiment_id)
);

CREATE TABLE IF NOT EXISTS t_report_score_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_score_id BIGINT NOT NULL,
    rubric_item_id BIGINT NOT NULL,
    score DECIMAL(6,2) NOT NULL,
    comment VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_score_rubric(report_score_id, rubric_item_id),
    INDEX idx_score_item_score(report_score_id)
);

UPDATE t_exam_record er
SET er.status = CASE
        WHEN er.status = 'REVIEWED' THEN 'GRADED'
        WHEN er.status = 'SUBMITTED' AND EXISTS (
            SELECT 1
            FROM t_exam_answer ea
            WHERE ea.record_id = er.id
              AND ea.is_correct IS NULL
        ) THEN 'PENDING_REVIEW'
        WHEN er.status = 'SUBMITTED' THEN 'GRADED'
        ELSE er.status
    END,
    er.final_grade_time = CASE
        WHEN er.status IN ('SUBMITTED', 'REVIEWED') THEN COALESCE(er.submit_time, er.update_time, er.create_time)
        ELSE er.final_grade_time
    END
WHERE er.status IN ('SUBMITTED', 'REVIEWED');

DROP PROCEDURE add_column_if_missing;
DROP PROCEDURE add_index_if_missing;
