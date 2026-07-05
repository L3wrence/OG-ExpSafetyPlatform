CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(64) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(30),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_username (username)
);

CREATE TABLE IF NOT EXISTS t_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    UNIQUE KEY uk_role_code (role_code)
);

CREATE TABLE IF NOT EXISTS t_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    type TINYINT NOT NULL,
    parent_id BIGINT DEFAULT 0,
    path VARCHAR(255),
    icon VARCHAR(100),
    sort INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permission_code (code)
);

CREATE TABLE IF NOT EXISTS t_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS t_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS t_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL,
    expire_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_token_value (token),
    KEY idx_token_user (user_id)
);

INSERT INTO t_role (role_name, role_code, description)
SELECT '系统管理员', 'ADMIN', '拥有系统全部管理权限'
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE role_code = 'ADMIN');

INSERT INTO t_role (role_name, role_code, description)
SELECT '教师', 'TEACHER', '管理自己负责的实验课程'
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE role_code = 'TEACHER');

INSERT INTO t_role (role_name, role_code, description)
SELECT '学生', 'STUDENT', '学习实验课程并查看个人数据'
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE role_code = 'STUDENT');

INSERT INTO t_role (role_name, role_code, description)
SELECT '实验室管理员', 'LAB_ADMIN', '查看实验室运行统计并管理预约'
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE role_code = 'LAB_ADMIN');

INSERT INTO t_permission (name, code, type, parent_id, path, icon, sort)
SELECT p.name, p.code, p.type, p.parent_id, p.path, p.icon, p.sort
FROM (
    SELECT '系统管理' AS name, 'system:menu' AS code, 1 AS type, 0 AS parent_id, '/system' AS path, 'setting' AS icon, 10 AS sort
    UNION ALL SELECT '用户管理', 'user:menu', 1, 0, '/users', 'user', 20
    UNION ALL SELECT '角色管理', 'role:menu', 1, 0, '/roles', 'shield', 30
    UNION ALL SELECT '权限管理', 'permission:menu', 1, 0, '/permissions', 'key', 40
    UNION ALL SELECT '实验教学', 'teaching:menu', 1, 0, '/teaching', 'book', 50
    UNION ALL SELECT '数据统计', 'dashboard:menu', 1, 0, '/dashboard', 'chart', 60
    UNION ALL SELECT '用户查看', 'user:view', 2, 0, NULL, NULL, 101
    UNION ALL SELECT '用户新增', 'user:create', 2, 0, NULL, NULL, 102
    UNION ALL SELECT '用户修改', 'user:update', 2, 0, NULL, NULL, 103
    UNION ALL SELECT '用户删除', 'user:delete', 2, 0, NULL, NULL, 104
    UNION ALL SELECT '角色查看', 'role:view', 2, 0, NULL, NULL, 111
    UNION ALL SELECT '角色权限分配', 'role:permission:update', 2, 0, NULL, NULL, 112
    UNION ALL SELECT '权限查看', 'permission:view', 2, 0, NULL, NULL, 121
    UNION ALL SELECT '课程查看', 'course:view', 2, 0, NULL, NULL, 201
    UNION ALL SELECT '课程新增', 'course:create', 2, 0, NULL, NULL, 202
    UNION ALL SELECT '课程修改', 'course:update', 2, 0, NULL, NULL, 203
    UNION ALL SELECT '课程删除', 'course:delete', 2, 0, NULL, NULL, 204
    UNION ALL SELECT '实验查看', 'experiment:view', 2, 0, NULL, NULL, 211
    UNION ALL SELECT '实验新增', 'experiment:create', 2, 0, NULL, NULL, 212
    UNION ALL SELECT '实验修改', 'experiment:update', 2, 0, NULL, NULL, 213
    UNION ALL SELECT '实验删除', 'experiment:delete', 2, 0, NULL, NULL, 214
    UNION ALL SELECT '资源查看', 'resource:view', 2, 0, NULL, NULL, 221
    UNION ALL SELECT '资源新增', 'resource:create', 2, 0, NULL, NULL, 222
    UNION ALL SELECT '资源修改', 'resource:update', 2, 0, NULL, NULL, 223
    UNION ALL SELECT '资源删除', 'resource:delete', 2, 0, NULL, NULL, 224
    UNION ALL SELECT '学习进度更新', 'learning:update:self', 2, 0, NULL, NULL, 231
    UNION ALL SELECT '安全知识查看', 'safety:view', 2, 0, NULL, NULL, 241
    UNION ALL SELECT '安全知识新增', 'safety:create', 2, 0, NULL, NULL, 242
    UNION ALL SELECT '安全知识修改', 'safety:update', 2, 0, NULL, NULL, 243
    UNION ALL SELECT '安全知识删除', 'safety:delete', 2, 0, NULL, NULL, 244
    UNION ALL SELECT '统计看板', 'dashboard:view', 2, 0, NULL, NULL, 301
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
    'course:view', 'course:create', 'course:update', 'course:delete',
    'experiment:view', 'experiment:create', 'experiment:update', 'experiment:delete',
    'resource:view', 'resource:create', 'resource:update', 'resource:delete',
    'safety:view', 'safety:create', 'safety:update', 'safety:delete',
    'dashboard:view'
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
    'course:view', 'experiment:view', 'resource:view',
    'learning:update:self', 'safety:view', 'dashboard:view'
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
    'course:view', 'experiment:view', 'resource:view', 'safety:view', 'dashboard:view'
)
WHERE r.role_code = 'LAB_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM t_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
