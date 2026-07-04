CREATE TABLE IF NOT EXISTS t_lab_course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_code VARCHAR(50) NOT NULL,
    course_name VARCHAR(100) NOT NULL,
    direction VARCHAR(50),
    teacher_id BIGINT,
    semester VARCHAR(20),
    description TEXT,
    cover_url VARCHAR(255),
    status TINYINT DEFAULT 1,
    sort INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_course_code (course_code),
    KEY idx_teacher_status_semester (teacher_id, status, semester)
);

CREATE TABLE IF NOT EXISTS t_course_student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    semester VARCHAR(20),
    status TINYINT DEFAULT 1,
    join_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_course_student_deleted (course_id, student_id, deleted),
    KEY idx_course_student_semester (semester)
);

CREATE TABLE IF NOT EXISTS t_experiment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    exp_code VARCHAR(50) NOT NULL,
    exp_name VARCHAR(120) NOT NULL,
    objective TEXT,
    principle TEXT,
    equipment TEXT,
    risk_level VARCHAR(20),
    duration_minutes INT DEFAULT 0,
    safety_pass_score INT DEFAULT 60,
    reservation_enabled TINYINT DEFAULT 1,
    status TINYINT DEFAULT 1,
    sort INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_course_exp_code_deleted (course_id, exp_code, deleted),
    KEY idx_experiment_course_status (course_id, status)
);

CREATE TABLE IF NOT EXISTS t_experiment_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    experiment_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    title VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    safety_tip TEXT,
    required_flag TINYINT DEFAULT 1,
    estimated_minutes INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_experiment_step_deleted (experiment_id, step_no, deleted),
    KEY idx_step_experiment (experiment_id)
);

CREATE TABLE IF NOT EXISTS t_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    experiment_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    resource_type VARCHAR(20),
    url VARCHAR(500),
    file_path VARCHAR(500),
    file_size BIGINT DEFAULT 0,
    required_flag TINYINT DEFAULT 0,
    view_count INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    sort INT DEFAULT 0,
    upload_user_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    KEY idx_resource_experiment_type_status (experiment_id, resource_type, status)
);

CREATE TABLE IF NOT EXISTS t_safety_knowledge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    experiment_id BIGINT,
    knowledge_point VARCHAR(120) NOT NULL,
    risk_type VARCHAR(50),
    content TEXT NOT NULL,
    related_step_id BIGINT,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    KEY idx_knowledge_point (knowledge_point),
    KEY idx_knowledge_risk_type (risk_type),
    KEY idx_knowledge_experiment (experiment_id)
);

CREATE TABLE IF NOT EXISTS t_learning_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    experiment_id BIGINT NOT NULL,
    progress DECIMAL(5,2) DEFAULT 0,
    duration_seconds INT DEFAULT 0,
    finish_flag TINYINT DEFAULT 0,
    first_time DATETIME,
    last_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_student_resource_deleted (student_id, resource_id, deleted),
    KEY idx_learning_experiment (experiment_id),
    KEY idx_learning_finish (finish_flag)
);

INSERT INTO t_permission (name, code, type, parent_id, path, sort, create_time)
SELECT '课程查看', 'course:view', 2, NULL, NULL, 100, NOW()
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_permission')
  AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'course:view');

INSERT INTO t_permission (name, code, type, parent_id, path, sort, create_time)
SELECT '实验查看', 'experiment:view', 2, NULL, NULL, 110, NOW()
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_permission')
  AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'experiment:view');

INSERT INTO t_permission (name, code, type, parent_id, path, sort, create_time)
SELECT '资源查看', 'resource:view', 2, NULL, NULL, 120, NOW()
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_permission')
  AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'resource:view');

INSERT INTO t_permission (name, code, type, parent_id, path, sort, create_time)
SELECT '学习进度更新', 'learning:update:self', 2, NULL, NULL, 130, NOW()
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_permission')
  AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'learning:update:self');

INSERT INTO t_permission (name, code, type, parent_id, path, sort, create_time)
SELECT '统计看板', 'dashboard:view', 2, NULL, NULL, 140, NOW()
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_permission')
  AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'dashboard:view');

INSERT INTO t_permission (name, code, type, parent_id, path, sort, create_time)
SELECT p.name, p.code, 2, NULL, NULL, p.sort, NOW()
FROM (
    SELECT '课程新增' AS name, 'course:create' AS code, 101 AS sort
    UNION ALL SELECT '课程修改', 'course:update', 102
    UNION ALL SELECT '课程删除', 'course:delete', 103
    UNION ALL SELECT '实验新增', 'experiment:create', 111
    UNION ALL SELECT '实验修改', 'experiment:update', 112
    UNION ALL SELECT '实验删除', 'experiment:delete', 113
    UNION ALL SELECT '资源新增', 'resource:create', 121
    UNION ALL SELECT '资源修改', 'resource:update', 122
    UNION ALL SELECT '资源删除', 'resource:delete', 123
    UNION ALL SELECT '安全知识查看', 'safety:view', 131
    UNION ALL SELECT '安全知识新增', 'safety:create', 132
    UNION ALL SELECT '安全知识修改', 'safety:update', 133
    UNION ALL SELECT '安全知识删除', 'safety:delete', 134
) p
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_permission')
  AND NOT EXISTS (SELECT 1 FROM t_permission tp WHERE tp.code = p.code);
