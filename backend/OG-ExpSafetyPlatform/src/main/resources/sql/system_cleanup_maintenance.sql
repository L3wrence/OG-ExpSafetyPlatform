-- System cleanup and compatibility maintenance.
-- MySQL 8 repeatable script; only removes invalid relational noise, not normal business data.

DELETE t
FROM t_token t
LEFT JOIN t_user u ON u.id = t.user_id
WHERE u.id IS NULL
   OR (t.expire_time IS NOT NULL AND t.expire_time < NOW());

DELETE ur
FROM t_user_role ur
LEFT JOIN t_user u ON u.id = ur.user_id
LEFT JOIN t_role r ON r.id = ur.role_id
WHERE u.id IS NULL OR r.id IS NULL;

DELETE rp
FROM t_role_permission rp
LEFT JOIN t_role r ON r.id = rp.role_id
LEFT JOIN t_permission p ON p.id = rp.permission_id
WHERE r.id IS NULL OR p.id IS NULL;

DELETE cs
FROM t_course_student cs
LEFT JOIN t_user u ON u.id = cs.student_id
LEFT JOIN t_lab_course c ON c.id = cs.course_id
WHERE u.id IS NULL OR c.id IS NULL;

DELETE lr
FROM t_learning_record lr
LEFT JOIN t_user u ON u.id = lr.student_id
LEFT JOIN t_resource r ON r.id = lr.resource_id
WHERE u.id IS NULL OR r.id IS NULL;

DELETE ri
FROM t_resource_interaction ri
LEFT JOIN t_user u ON u.id = ri.user_id
LEFT JOIN t_resource r ON r.id = ri.resource_id
WHERE u.id IS NULL OR r.id IS NULL;

DELETE a
FROM t_hse_practice_answer a
LEFT JOIN t_user u ON u.id = a.student_id
LEFT JOIN t_question q ON q.id = a.question_id
WHERE u.id IS NULL OR q.id IS NULL;

DELETE w
FROM t_hse_wrong_question w
LEFT JOIN t_user u ON u.id = w.student_id
LEFT JOIN t_question q ON q.id = w.question_id
WHERE u.id IS NULL OR q.id IS NULL;

DELETE f
FROM t_hse_question_favorite f
LEFT JOIN t_user u ON u.id = f.student_id
LEFT JOIN t_question q ON q.id = f.question_id
WHERE u.id IS NULL OR q.id IS NULL;

DELETE FROM t_recent_visit
WHERE path IS NULL OR path = '' OR path IN ('/', '/login');

INSERT INTO t_permission (name, code, type, parent_id, path, icon, sort)
SELECT p.name, p.code, 2, 0, NULL, NULL, p.sort
FROM (
    SELECT '门户首页' AS name, 'portal:view' AS code, 251 AS sort
    UNION ALL SELECT '个人资料维护', 'profile:update', 252
    UNION ALL SELECT '密码修改', 'profile:password', 253
    UNION ALL SELECT '消息提醒', 'portal:message', 254
    UNION ALL SELECT '全站搜索', 'portal:search', 255
    UNION ALL SELECT '在线考试', 'exam:take', 421
    UNION ALL SELECT '实验预约查看', 'reservation:view', 431
    UNION ALL SELECT '实验预约审核', 'reservation:review', 433
    UNION ALL SELECT '实验报告查看', 'report:view', 441
    UNION ALL SELECT '实验报告提交', 'report:submit', 442
    UNION ALL SELECT '实验报告审核', 'report:review', 444
    UNION ALL SELECT 'AI助手使用', 'ai:ask', 461
) p
WHERE NOT EXISTS (SELECT 1 FROM t_permission tp WHERE tp.code = p.code);

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p
WHERE r.role_code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM t_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p ON p.code IN (
    'portal:view', 'profile:update', 'profile:password', 'portal:message', 'portal:search',
    'course:view', 'course:create', 'course:update', 'course:delete',
    'experiment:view', 'experiment:create', 'experiment:update', 'experiment:delete',
    'resource:view', 'resource:create', 'resource:update', 'resource:delete',
    'safety:view', 'safety:create', 'safety:update', 'safety:delete',
    'question:view', 'question:create', 'question:update', 'question:delete',
    'exam-paper:view', 'exam:create', 'exam:update', 'exam:delete', 'exam:statistics',
    'reservation:view', 'reservation:manage', 'reservation:review',
    'report:view', 'report:grade', 'report:review',
    'recommend:view', 'ai:ask', 'dashboard:view'
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
    'portal:view', 'profile:update', 'profile:password', 'portal:message', 'portal:search',
    'course:view', 'experiment:view', 'resource:view', 'learning:update:self', 'safety:view',
    'exam:take', 'reservation:view', 'report:view', 'report:submit',
    'recommend:view', 'ai:ask', 'dashboard:view'
)
WHERE r.role_code = 'STUDENT'
  AND NOT EXISTS (
      SELECT 1 FROM t_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p ON p.code IN (
    'portal:view', 'profile:update', 'profile:password', 'portal:message', 'portal:search',
    'course:view', 'experiment:view', 'resource:view', 'safety:view',
    'reservation:view', 'reservation:review', 'dashboard:view'
)
WHERE r.role_code = 'LAB_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM t_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

UPDATE t_question q
JOIN t_safety_knowledge k ON k.knowledge_point = q.knowledge_point AND k.deleted = 0
SET q.knowledge_id = COALESCE(q.knowledge_id, k.id),
    q.experiment_id = COALESCE(q.experiment_id, k.experiment_id),
    q.risk_type = COALESCE(q.risk_type, k.risk_type)
WHERE q.knowledge_id IS NULL
   OR q.experiment_id IS NULL
   OR q.risk_type IS NULL;

UPDATE t_safety_knowledge
SET emergency_flag = 1
WHERE deleted = 0
  AND emergency_flag = 0
  AND (category = 'EMERGENCY' OR knowledge_point REGEXP '应急|疏散|急停|救援|受伤');

UPDATE t_safety_knowledge
SET content = CONCAT(content, '\n涉及应急处理时，以学校正式制度和现场教师要求为准。')
WHERE deleted = 0
  AND emergency_flag = 1
  AND content NOT LIKE '%学校正式制度%';
