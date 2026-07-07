-- AmazingTeaching visual identity fields.
-- MySQL 8 repeatable script; all columns are nullable to keep existing data compatible.

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

CALL add_column_if_missing('t_lab_course', 'tagline', 'tagline VARCHAR(160) NULL DEFAULT NULL AFTER cover_url');
CALL add_column_if_missing('t_lab_course', 'highlight_tags', 'highlight_tags VARCHAR(300) NULL DEFAULT NULL AFTER tagline');
CALL add_column_if_missing('t_lab_course', 'visual_theme', 'visual_theme VARCHAR(60) NULL DEFAULT NULL AFTER highlight_tags');

CALL add_column_if_missing('t_experiment', 'cover_url', 'cover_url VARCHAR(255) NULL DEFAULT NULL AFTER direction');
CALL add_column_if_missing('t_experiment', 'scenario_intro', 'scenario_intro TEXT NULL AFTER cover_url');
CALL add_column_if_missing('t_experiment', 'visual_theme', 'visual_theme VARCHAR(60) NULL DEFAULT NULL AFTER scenario_intro');

UPDATE t_lab_course
SET tagline = COALESCE(tagline, '把油气工程实验拆成看得懂、能操作、可复盘的学习路径。'),
    highlight_tags = COALESCE(highlight_tags, '实验可视化,安全准入,资源学习'),
    visual_theme = COALESCE(visual_theme, 'oilfield-lab')
WHERE deleted = 0;

UPDATE t_experiment
SET scenario_intro = COALESCE(scenario_intro, '从真实油气工程现场问题出发，完成原理理解、风险识别、步骤演练和实验复盘。'),
    visual_theme = COALESCE(visual_theme, 'procedure-map')
WHERE deleted = 0;

DROP PROCEDURE IF EXISTS add_column_if_missing;
