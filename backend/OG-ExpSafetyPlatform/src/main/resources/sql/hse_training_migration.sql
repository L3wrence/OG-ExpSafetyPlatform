-- HSE safety knowledge and training migration.
-- MySQL 8 repeatable script; preserves existing safety knowledge, question and exam data.

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

CALL add_column_if_missing('t_safety_knowledge', 'category', 'category VARCHAR(50) NOT NULL DEFAULT ''HSE_BASIC'' AFTER experiment_id');
CALL add_column_if_missing('t_safety_knowledge', 'reference_resource_id', 'reference_resource_id BIGINT NULL AFTER related_step_id');
CALL add_column_if_missing('t_safety_knowledge', 'emergency_flag', 'emergency_flag TINYINT NOT NULL DEFAULT 0 AFTER reference_resource_id');

CALL add_column_if_missing('t_question', 'knowledge_id', 'knowledge_id BIGINT NULL AFTER knowledge_point');
CALL add_column_if_missing('t_question', 'experiment_id', 'experiment_id BIGINT NULL AFTER knowledge_id');
CALL add_column_if_missing('t_question', 'risk_type', 'risk_type VARCHAR(100) NULL DEFAULT NULL AFTER experiment_id');
CALL add_column_if_missing('t_question', 'related_resource_id', 'related_resource_id BIGINT NULL AFTER risk_type');

DROP PROCEDURE IF EXISTS add_column_if_missing;

CREATE TABLE IF NOT EXISTS t_hse_practice_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    knowledge_id BIGINT NULL,
    experiment_id BIGINT NULL,
    risk_type VARCHAR(100) NULL,
    practice_type VARCHAR(40) NOT NULL DEFAULT 'RANDOM',
    student_answer VARCHAR(1000) NULL,
    correct_flag TINYINT NOT NULL DEFAULT 0,
    score INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    KEY idx_hse_answer_student_time (student_id, create_time),
    KEY idx_hse_answer_question (question_id),
    KEY idx_hse_answer_knowledge (knowledge_id),
    KEY idx_hse_answer_risk (risk_type)
);

CREATE TABLE IF NOT EXISTS t_hse_wrong_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    knowledge_id BIGINT NULL,
    knowledge_point VARCHAR(200) NULL,
    risk_type VARCHAR(100) NULL,
    wrong_count INT NOT NULL DEFAULT 0,
    correct_streak INT NOT NULL DEFAULT 0,
    mastery_status VARCHAR(30) NOT NULL DEFAULT 'LEARNING',
    last_wrong_time DATETIME NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_hse_wrong_student_question_deleted (student_id, question_id, deleted),
    KEY idx_hse_wrong_student_status (student_id, mastery_status),
    KEY idx_hse_wrong_knowledge (knowledge_id, knowledge_point),
    KEY idx_hse_wrong_risk (risk_type)
);

CREATE TABLE IF NOT EXISTS t_hse_question_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_hse_favorite_student_question_deleted (student_id, question_id, deleted),
    KEY idx_hse_favorite_student (student_id)
);

UPDATE t_safety_knowledge
SET category = CASE
        WHEN knowledge_point REGEXP '制度|规范|纪律|准入' THEN 'SAFETY_RULE'
        WHEN knowledge_point REGEXP '化学|药品|试剂|废液|泄漏' OR risk_type REGEXP '化学|泄漏' THEN 'CHEMICAL'
        WHEN knowledge_point REGEXP '电气|触电|电源' OR risk_type REGEXP '电气|触电' THEN 'ELECTRICAL'
        WHEN knowledge_point REGEXP '火|爆|燃' OR risk_type REGEXP '火|爆|燃' THEN 'FIRE_EXPLOSION'
        WHEN knowledge_point REGEXP 'PPE|防护|护目|手套|实验服' THEN 'PPE'
        WHEN knowledge_point REGEXP '废弃|废物|废液' THEN 'WASTE'
        WHEN knowledge_point REGEXP '事故|案例' THEN 'ACCIDENT_CASE'
        WHEN knowledge_point REGEXP '应急|疏散|急停|救援|受伤' THEN 'EMERGENCY'
        WHEN knowledge_point REGEXP '设备|仪器|高温|高压|旋转' OR risk_type REGEXP '设备|高温|高压|旋转' THEN 'EQUIPMENT'
        ELSE 'HSE_BASIC'
    END,
    emergency_flag = CASE WHEN knowledge_point REGEXP '应急|疏散|急停|救援|受伤' THEN 1 ELSE emergency_flag END
WHERE deleted = 0;

UPDATE t_safety_knowledge
SET content = CONCAT(content, '\n涉及应急处理时，以学校正式制度和现场教师要求为准。')
WHERE deleted = 0
  AND emergency_flag = 1
  AND content NOT LIKE '%学校正式制度%';

UPDATE t_question q
JOIN t_safety_knowledge k ON k.knowledge_point = q.knowledge_point AND k.deleted = 0
SET q.knowledge_id = COALESCE(q.knowledge_id, k.id),
    q.experiment_id = COALESCE(q.experiment_id, k.experiment_id),
    q.risk_type = COALESCE(q.risk_type, k.risk_type)
WHERE q.knowledge_id IS NULL;

INSERT INTO t_safety_knowledge (
    experiment_id, category, knowledge_point, risk_type, content, related_step_id,
    reference_resource_id, emergency_flag, status
)
SELECT NULL, sample.category, sample.knowledge_point, sample.risk_type, sample.content, NULL, NULL, sample.emergency_flag, 1
FROM (
    SELECT 'SAFETY_RULE' AS category, '实验室基本安全制度' AS knowledge_point, '制度规范' AS risk_type,
           '进入实验室前应完成安全学习，遵守教师安排，不得单独开展高风险操作。' AS content, 0 AS emergency_flag
    UNION ALL SELECT 'CHEMICAL', '危险化学品分类与使用', '化学品风险',
           '按标签和安全技术说明书识别危险化学品，取用后及时封存，废液按类别回收。', 0
    UNION ALL SELECT 'EQUIPMENT', '高温高压与旋转设备风险', '设备风险',
           '启动前确认防护罩、压力表和联锁状态，异常振动、异响或压力突变时立即停机。', 0
    UNION ALL SELECT 'PPE', 'PPE个人防护用品使用规范', '个人防护',
           '根据实验风险穿戴实验服、护目镜、防护手套和防滑鞋，污染或破损后及时更换。', 0
    UNION ALL SELECT 'EMERGENCY', '火灾泄漏触电和人员受伤处置', '应急处置',
           '立即停止操作，切断能源并报告教师；涉及应急处理时，以学校正式制度和现场教师要求为准。', 1
) sample
WHERE NOT EXISTS (
    SELECT 1 FROM t_safety_knowledge k
    WHERE k.knowledge_point = sample.knowledge_point AND k.deleted = 0
);

INSERT INTO t_question (
    type, content, options, answer, score, analysis, knowledge_point, knowledge_id,
    experiment_id, risk_type, difficulty, course_id, create_by
)
SELECT 'SINGLE',
       '发现高压设备压力异常升高时，学生首先应采取的措施是？',
       '[{"value":"A","label":"继续观察并记录"},{"value":"B","label":"立即停止操作并报告教师"},{"value":"C","label":"自行拆卸阀门"},{"value":"D","label":"提高加热功率"}]',
       'B',
       5,
       '高压设备异常时应立即停止操作并报告教师，禁止学生自行拆卸或继续升压。',
       k.knowledge_point,
       k.id,
       k.experiment_id,
       k.risk_type,
       'EASY',
       NULL,
       COALESCE((SELECT MIN(u.id) FROM t_user u), 1)
FROM t_safety_knowledge k
WHERE k.knowledge_point = '高温高压与旋转设备风险'
  AND NOT EXISTS (
      SELECT 1 FROM t_question q
      WHERE q.content = '发现高压设备压力异常升高时，学生首先应采取的措施是？'
        AND (q.is_deleted = 0 OR q.is_deleted IS NULL)
  )
LIMIT 1;

INSERT INTO t_question (
    type, content, options, answer, score, analysis, knowledge_point, knowledge_id,
    experiment_id, risk_type, difficulty, course_id, create_by
)
SELECT 'JUDGE',
       '实验练习成绩可以直接替代正式安全考试并授予实验准入资格。',
       '[{"value":"A","label":"正确"},{"value":"B","label":"错误"}]',
       'B',
       5,
       '练习只用于巩固知识和识别薄弱点，实验准入以正式安全考试结果为准。',
       k.knowledge_point,
       k.id,
       k.experiment_id,
       k.risk_type,
       'EASY',
       NULL,
       COALESCE((SELECT MIN(u.id) FROM t_user u), 1)
FROM t_safety_knowledge k
WHERE k.knowledge_point = '实验室基本安全制度'
  AND NOT EXISTS (
      SELECT 1 FROM t_question q
      WHERE q.content = '实验练习成绩可以直接替代正式安全考试并授予实验准入资格。'
        AND (q.is_deleted = 0 OR q.is_deleted IS NULL)
  )
LIMIT 1;
