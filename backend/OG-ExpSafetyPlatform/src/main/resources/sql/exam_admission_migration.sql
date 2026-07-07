-- Online safety exam and experiment admission migration.
-- MySQL 8 repeatable script; keeps existing exam and reservation data compatible.

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
DELIMITER ;

CALL add_column_if_missing('t_exam_paper', 'attempt_limit', 'attempt_limit INT NOT NULL DEFAULT 1 AFTER duration');
CALL add_column_if_missing('t_exam_paper', 'show_answer_after_submit', 'show_answer_after_submit TINYINT NOT NULL DEFAULT 1 AFTER attempt_limit');
CALL add_column_if_missing('t_exam_paper', 'admission_validity_days', 'admission_validity_days INT NOT NULL DEFAULT 180 AFTER show_answer_after_submit');
CALL add_column_if_missing('t_exam_paper', 'multiple_score_policy', 'multiple_score_policy VARCHAR(30) NOT NULL DEFAULT ''ALL_OR_NOTHING'' AFTER admission_validity_days');
CALL add_column_if_missing('t_exam_paper', 'random_enabled', 'random_enabled TINYINT NOT NULL DEFAULT 0 AFTER multiple_score_policy');
CALL add_column_if_missing('t_exam_paper', 'random_count', 'random_count INT NOT NULL DEFAULT 0 AFTER random_enabled');

CALL add_column_if_missing('t_exam_record', 'question_snapshot_json', 'question_snapshot_json LONGTEXT NULL AFTER status');
CALL add_column_if_missing('t_exam_record', 'auto_submit_flag', 'auto_submit_flag TINYINT NOT NULL DEFAULT 0 AFTER question_snapshot_json');
CALL add_column_if_missing('t_exam_record', 'admission_id', 'admission_id BIGINT NULL AFTER auto_submit_flag');
CALL add_column_if_missing('t_exam_record', 'end_time', 'end_time DATETIME NULL AFTER submit_time');

CREATE TABLE IF NOT EXISTS t_experiment_admission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    experiment_id BIGINT NOT NULL,
    paper_id BIGINT NULL,
    record_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALID',
    issued_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until DATETIME NULL,
    revoke_time DATETIME NULL,
    revoked_by BIGINT NULL,
    revoke_reason VARCHAR(500) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_admission_student_exp (student_id, experiment_id, status, deleted),
    KEY idx_admission_record (record_id),
    KEY idx_admission_valid_until (valid_until)
);

INSERT INTO t_experiment_admission (
    student_id, experiment_id, paper_id, record_id, status, issued_time, valid_until, deleted
)
SELECT er.student_id,
       er.experiment_id,
       er.paper_id,
       er.id,
       'VALID',
       COALESCE(er.submit_time, er.update_time, er.create_time, NOW()),
       DATE_ADD(COALESCE(er.submit_time, er.update_time, er.create_time, NOW()),
                INTERVAL COALESCE(ep.admission_validity_days, 180) DAY),
       0
FROM t_exam_record er
JOIN t_exam_paper ep ON ep.id = er.paper_id
WHERE er.passed = 1
  AND er.experiment_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM t_experiment_admission ea
      WHERE ea.record_id = er.id
        AND ea.deleted = 0
  );

UPDATE t_exam_record er
JOIN t_experiment_admission ea ON ea.record_id = er.id AND ea.deleted = 0
SET er.admission_id = ea.id
WHERE er.admission_id IS NULL;

UPDATE t_exam_paper
SET attempt_limit = COALESCE(NULLIF(attempt_limit, 0), 1),
    show_answer_after_submit = COALESCE(show_answer_after_submit, 1),
    admission_validity_days = COALESCE(NULLIF(admission_validity_days, 0), 180),
    multiple_score_policy = COALESCE(NULLIF(multiple_score_policy, ''), 'ALL_OR_NOTHING'),
    random_enabled = COALESCE(random_enabled, 0),
    random_count = COALESCE(random_count, 0);

DROP PROCEDURE add_column_if_missing;
