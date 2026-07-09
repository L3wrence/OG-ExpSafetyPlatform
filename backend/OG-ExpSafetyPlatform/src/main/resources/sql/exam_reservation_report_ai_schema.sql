CREATE TABLE IF NOT EXISTS t_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    options JSON,
    answer VARCHAR(500) NOT NULL,
    score INT DEFAULT 0,
    analysis VARCHAR(500),
    knowledge_point VARCHAR(200),
    difficulty VARCHAR(20) DEFAULT 'MEDIUM',
    course_id BIGINT,
    create_by BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    KEY idx_question_course (course_id),
    KEY idx_question_knowledge (knowledge_point)
);

CREATE TABLE IF NOT EXISTS t_exam_paper (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    course_id BIGINT,
    experiment_id BIGINT,
    total_score INT DEFAULT 100,
    pass_score INT DEFAULT 60,
    duration INT DEFAULT 30,
    teacher_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT',
    start_time DATETIME,
    end_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    deleted TINYINT GENERATED ALWAYS AS (is_deleted) STORED,
    KEY idx_exam_paper_course_status (course_id, status),
    KEY idx_exam_paper_experiment (experiment_id)
);

CREATE TABLE IF NOT EXISTS t_exam_paper_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    score INT NOT NULL,
    order_num INT DEFAULT 0,
    UNIQUE KEY uk_paper_question (paper_id, question_id),
    KEY idx_paper_question_paper (paper_id)
);

CREATE TABLE IF NOT EXISTS t_exam_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    paper_id BIGINT NOT NULL,
    experiment_id BIGINT,
    total_score INT,
    objective_score INT,
    subjective_score INT,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS',
    passed TINYINT,
    start_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    submit_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    KEY idx_exam_record_student (student_id),
    KEY idx_exam_record_paper (paper_id),
    KEY idx_exam_record_experiment (experiment_id)
);

CREATE TABLE IF NOT EXISTS t_exam_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    knowledge_id BIGINT,
    student_answer VARCHAR(500),
    is_correct TINYINT,
    correct_flag TINYINT,
    score INT DEFAULT 0,
    KEY idx_exam_answer_record (record_id),
    KEY idx_exam_answer_question (question_id),
    KEY idx_exam_answer_knowledge (knowledge_id)
);

CREATE TABLE IF NOT EXISTS t_lab_time_slot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    experiment_id BIGINT,
    `date` DATE NOT NULL,
    slot_date DATE GENERATED ALWAYS AS (`date`) STORED,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INT NOT NULL,
    booked_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    create_by BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_time_slot_lab_date (lab_id, `date`, status),
    KEY idx_time_slot_experiment (experiment_id)
);

CREATE TABLE IF NOT EXISTS t_reservation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    time_slot_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    experiment_id BIGINT,
    purpose VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    teacher_id BIGINT,
    review_comment VARCHAR(500),
    review_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    KEY idx_reservation_student_status (student_id, status),
    KEY idx_reservation_slot_status (time_slot_id, status),
    KEY idx_reservation_experiment (experiment_id)
);

CREATE TABLE IF NOT EXISTS t_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    experiment_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    file_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'DRAFT',
    submit_time DATETIME,
    latest_submit_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    deleted TINYINT GENERATED ALWAYS AS (is_deleted) STORED,
    KEY idx_report_student_status (student_id, status),
    KEY idx_report_experiment (experiment_id)
);

CREATE TABLE IF NOT EXISTS t_report_score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    score INT NOT NULL,
    comment VARCHAR(500),
    is_latest TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    grade_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_report_score_report_latest (report_id, is_latest),
    KEY idx_report_score_grade_time (grade_time)
);

CREATE TABLE IF NOT EXISTS t_recommend_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    experiment_id BIGINT,
    resource_id BIGINT NOT NULL,
    total_score DECIMAL(5,2) NOT NULL,
    score_breakdown JSON NOT NULL,
    reason VARCHAR(500) NOT NULL,
    clicked TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_recommend_student_experiment (student_id, experiment_id),
    KEY idx_recommend_resource (resource_id)
);

CREATE TABLE IF NOT EXISTS t_ai_chat_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    scene VARCHAR(50) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    tool_name VARCHAR(100),
    experiment_id BIGINT,
    manual_revision TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ai_user_scene_time (user_id, scene, create_time)
);

INSERT INTO t_permission (name, code, type, parent_id, path, sort, create_time)
SELECT p.name, p.code, 2, 0, NULL, p.sort, NOW()
FROM (
    SELECT '题库查看' AS name, 'question:view' AS code, 401 AS sort
    UNION ALL SELECT '题库维护', 'question:manage', 402
    UNION ALL SELECT '题库新增', 'question:create', 403
    UNION ALL SELECT '题库修改', 'question:update', 404
    UNION ALL SELECT '题库删除', 'question:delete', 405
    UNION ALL SELECT '试卷查看', 'exam-paper:view', 411
    UNION ALL SELECT '试卷维护', 'exam-paper:manage', 412
    UNION ALL SELECT '考试新增', 'exam:create', 413
    UNION ALL SELECT '考试修改', 'exam:update', 414
    UNION ALL SELECT '考试删除', 'exam:delete', 415
    UNION ALL SELECT '在线考试', 'exam:take', 421
    UNION ALL SELECT '考试统计', 'exam:statistics', 422
    UNION ALL SELECT '实验预约查看', 'reservation:view', 431
    UNION ALL SELECT '实验预约管理', 'reservation:manage', 432
    UNION ALL SELECT '实验预约审核', 'reservation:review', 433
    UNION ALL SELECT '实验报告查看', 'report:view', 441
    UNION ALL SELECT '实验报告提交', 'report:submit', 442
    UNION ALL SELECT '实验报告评分', 'report:grade', 443
    UNION ALL SELECT '实验报告审核', 'report:review', 444
    UNION ALL SELECT '推荐查看', 'recommend:view', 451
    UNION ALL SELECT '推荐反馈', 'recommend:feedback', 452
    UNION ALL SELECT 'AI助手使用', 'ai:ask', 461
) p
WHERE NOT EXISTS (SELECT 1 FROM t_permission tp WHERE tp.code = p.code);

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p
WHERE r.role_code = 'ADMIN'
  AND p.code IN (
      'question:view', 'question:manage', 'question:create', 'question:update', 'question:delete',
      'exam-paper:view', 'exam-paper:manage', 'exam:create', 'exam:update', 'exam:delete',
      'exam:take', 'exam:statistics',
      'reservation:view', 'reservation:manage', 'reservation:review',
      'report:view', 'report:submit', 'report:grade', 'report:review',
      'recommend:view', 'recommend:feedback',
      'ai:ask'
  )
  AND NOT EXISTS (
      SELECT 1 FROM t_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p ON p.code IN (
    'question:view', 'question:manage', 'question:create', 'question:update', 'question:delete',
    'exam-paper:view', 'exam-paper:manage', 'exam:create', 'exam:update', 'exam:delete',
    'exam:statistics',
    'reservation:view', 'reservation:manage', 'reservation:review',
    'report:view', 'report:grade', 'report:review',
    'recommend:view',
    'ai:ask'
)
WHERE r.role_code = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1 FROM t_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p ON p.code IN (
    'exam:take',
    'reservation:view',
    'report:view', 'report:submit',
    'recommend:view', 'recommend:feedback',
    'ai:ask'
)
WHERE r.role_code = 'STUDENT'
  AND NOT EXISTS (
      SELECT 1 FROM t_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
