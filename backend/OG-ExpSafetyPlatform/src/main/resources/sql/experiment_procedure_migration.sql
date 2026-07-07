-- Experiment procedure migration.
-- MySQL 8 repeatable script; keeps existing t_experiment and t_experiment_step data compatible.

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

CALL add_column_if_missing('t_experiment', 'direction', 'direction VARCHAR(100) NULL DEFAULT NULL AFTER exp_code');
CALL add_column_if_missing('t_experiment', 'description', 'description TEXT NULL AFTER direction');
CALL add_column_if_missing('t_experiment', 'materials', 'materials TEXT NULL AFTER equipment');
CALL add_column_if_missing('t_experiment', 'location', 'location VARCHAR(200) NULL DEFAULT NULL AFTER materials');
CALL add_column_if_missing('t_experiment', 'applicable_classes', 'applicable_classes VARCHAR(300) NULL DEFAULT NULL AFTER location');
CALL add_column_if_missing('t_experiment', 'hazard_sources', 'hazard_sources TEXT NULL AFTER risk_level');
CALL add_column_if_missing('t_experiment', 'risk_types', 'risk_types VARCHAR(300) NULL DEFAULT NULL AFTER hazard_sources');
CALL add_column_if_missing('t_experiment', 'ppe_requirements', 'ppe_requirements TEXT NULL AFTER risk_types');
CALL add_column_if_missing('t_experiment', 'prerequisite_knowledge', 'prerequisite_knowledge TEXT NULL AFTER ppe_requirements');
CALL add_column_if_missing('t_experiment', 'safety_requirement', 'safety_requirement TEXT NULL AFTER prerequisite_knowledge');
CALL add_column_if_missing('t_experiment', 'exam_required', 'exam_required TINYINT NOT NULL DEFAULT 1 AFTER safety_requirement');
CALL add_column_if_missing('t_experiment', 'data_record_requirement', 'data_record_requirement TEXT NULL AFTER safety_pass_score');
CALL add_column_if_missing('t_experiment', 'abnormal_handling', 'abnormal_handling TEXT NULL AFTER data_record_requirement');
CALL add_column_if_missing('t_experiment', 'emergency_procedure', 'emergency_procedure TEXT NULL AFTER abnormal_handling');
CALL add_column_if_missing('t_experiment', 'report_template_url', 'report_template_url VARCHAR(500) NULL DEFAULT NULL AFTER emergency_procedure');
CALL add_column_if_missing('t_experiment', 'grading_criteria', 'grading_criteria TEXT NULL AFTER report_template_url');

CALL add_column_if_missing('t_experiment_step', 'media_type', 'media_type VARCHAR(20) NOT NULL DEFAULT ''TEXT'' AFTER safety_tip');
CALL add_column_if_missing('t_experiment_step', 'media_url', 'media_url VARCHAR(500) NULL DEFAULT NULL AFTER media_type');
CALL add_column_if_missing('t_experiment_step', 'flowchart_data', 'flowchart_data TEXT NULL AFTER media_url');

DROP PROCEDURE IF EXISTS add_column_if_missing;

UPDATE t_experiment e
JOIN t_lab_course c ON c.id = e.course_id
SET e.direction = c.direction
WHERE e.direction IS NULL AND c.direction IS NOT NULL;

UPDATE t_experiment
SET exam_required = 1
WHERE exam_required IS NULL;

INSERT INTO t_experiment_step (experiment_id, step_no, title, content, safety_tip, media_type, required_flag, estimated_minutes)
SELECT e.id, 1, '实验准备', '核对实验仪器、样品和记录表，确认实验环境满足要求。', '穿戴规定PPE，检查阀门、压力表和电源状态。', 'TEXT', 1, 10
FROM t_experiment e
WHERE e.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM t_experiment_step s WHERE s.experiment_id = e.id AND s.deleted = 0)
LIMIT 1;

UPDATE t_experiment
SET safety_requirement = COALESCE(safety_requirement, '实验前完成安全学习，遵守教师现场指令，禁止单独操作高压或带电设备。'),
    ppe_requirements = COALESCE(ppe_requirements, '实验服、防护眼镜、防滑手套'),
    data_record_requirement = COALESCE(data_record_requirement, '按实验记录表实时记录关键参数、异常现象和处理结果。'),
    abnormal_handling = COALESCE(abnormal_handling, '发现泄漏、异常声响、压力突变或电气异常时立即停止操作并报告教师。'),
    emergency_procedure = COALESCE(emergency_procedure, '停止实验、切断能源、撤离无关人员、按应急预案处置并记录。'),
    grading_criteria = COALESCE(grading_criteria, '预习与安全规范30%，实验操作30%，数据记录20%，报告分析20%。')
WHERE deleted = 0
LIMIT 1;
