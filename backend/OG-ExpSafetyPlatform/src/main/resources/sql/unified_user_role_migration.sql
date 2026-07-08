-- Phase 4 role cleanup: keep only ADMIN and USER as global roles.
-- Teacher capability is derived from t_teacher_certification.status = 'APPROVED'.
-- Classroom student capability is derived from t_course_student membership.

INSERT INTO t_role (role_name, role_code, description)
SELECT '普通用户', 'USER', '公共资源学习、交流、投稿和加入课堂'
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE role_code = 'USER');

INSERT INTO t_role (role_name, role_code, description)
SELECT '系统管理员', 'ADMIN', '平台治理、权限、审核与日志'
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE role_code = 'ADMIN');

INSERT INTO t_permission (name, code, type, parent_id, path, icon, sort, create_time)
SELECT p.name, p.code, 2, 0, NULL, NULL, p.sort, NOW()
FROM (
    SELECT '门户访问' name, 'portal:view' code, 10 sort UNION ALL
    SELECT '门户搜索', 'portal:search', 20 UNION ALL
    SELECT '消息日程', 'portal:message', 30 UNION ALL
    SELECT '个人资料', 'profile:update', 40 UNION ALL
    SELECT '修改密码', 'profile:password', 50 UNION ALL
    SELECT '课程查看', 'course:view', 60 UNION ALL
    SELECT '课堂加入', 'course:join', 70 UNION ALL
    SELECT '实验查看', 'experiment:view', 80 UNION ALL
    SELECT '资源查看', 'resource:view', 90 UNION ALL
    SELECT '学习记录', 'learning:update:self', 100 UNION ALL
    SELECT '考试参加', 'exam:take', 110 UNION ALL
    SELECT '预约查看', 'reservation:view', 120 UNION ALL
    SELECT '报告查看', 'report:view', 130 UNION ALL
    SELECT '报告提交', 'report:submit', 140 UNION ALL
    SELECT '安全知识查看', 'safety:view', 150 UNION ALL
    SELECT 'AI问答', 'ai:ask', 160 UNION ALL
    SELECT '教师认证申请', 'teacher-certification:apply', 170 UNION ALL
    SELECT '资源投稿', 'resource-submission:create', 180
) p
WHERE NOT EXISTS (SELECT 1 FROM t_permission existing WHERE existing.code = p.code);

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p ON p.code IN (
    'portal:view', 'portal:search', 'portal:message',
    'profile:update', 'profile:password',
    'course:view', 'course:join', 'experiment:view',
    'resource:view', 'learning:update:self',
    'exam:take', 'reservation:view', 'report:view', 'report:submit', 'safety:view',
    'ai:ask', 'teacher-certification:apply', 'resource-submission:create'
)
WHERE r.role_code = 'USER'
  AND NOT EXISTS (
      SELECT 1 FROM t_role_permission rp
       WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, user_role.id
FROM t_user u
JOIN t_role user_role ON user_role.role_code = 'USER'
WHERE NOT EXISTS (
    SELECT 1
    FROM t_user_role ur
    JOIN t_role admin_role ON admin_role.id = ur.role_id AND admin_role.role_code = 'ADMIN'
    WHERE ur.user_id = u.id
)
AND NOT EXISTS (
    SELECT 1 FROM t_user_role ur
     WHERE ur.user_id = u.id AND ur.role_id = user_role.id
);

DELETE ur
FROM t_user_role ur
JOIN t_role r ON r.id = ur.role_id
WHERE r.role_code IN ('TEACHER', 'STUDENT', 'LAB_ADMIN');

DELETE rp
FROM t_role_permission rp
JOIN t_role r ON r.id = rp.role_id
WHERE r.role_code IN ('TEACHER', 'STUDENT', 'LAB_ADMIN');

DELETE FROM t_role
WHERE role_code IN ('TEACHER', 'STUDENT', 'LAB_ADMIN');

DELETE ur
FROM t_user_role ur
JOIN t_user u ON u.id = ur.user_id
WHERE u.username = 'lab_admin';

DELETE FROM t_user
WHERE username = 'lab_admin';

UPDATE t_portal_notice
SET target_role = 'USER'
WHERE target_role IN ('TEACHER', 'STUDENT', 'LAB_ADMIN');
