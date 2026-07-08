DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(IN table_name_in VARCHAR(64), IN column_name_in VARCHAR(64), IN ddl_sql TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = table_name_in AND column_name = column_name_in
  ) THEN
    SET @stmt = CONCAT('ALTER TABLE ', table_name_in, ' ADD COLUMN ', ddl_sql);
    PREPARE s FROM @stmt;
    EXECUTE s;
    DEALLOCATE PREPARE s;
  END IF;
END //
DELIMITER ;

INSERT INTO t_role (role_name, role_code, description)
SELECT '普通用户', 'USER', '面向油气工程兴趣用户的公共学习身份'
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE role_code = 'USER');

INSERT INTO t_permission (name, code, type, parent_id, path, icon, sort, create_time)
SELECT p.name, p.code, 2, 0, NULL, NULL, p.sort, NOW()
FROM (
  SELECT '教师认证申请' AS name, 'teacher-certification:apply' AS code, 610 AS sort
  UNION ALL SELECT '教师认证审核', 'teacher-certification:review', 611
  UNION ALL SELECT '课堂加入', 'course:join', 612
  UNION ALL SELECT '课堂邀请码管理', 'course:invite:manage', 613
  UNION ALL SELECT '资源投稿', 'resource-submission:create', 614
  UNION ALL SELECT '资源投稿审核', 'resource-submission:review', 615
) p
WHERE NOT EXISTS (SELECT 1 FROM t_permission tp WHERE tp.code = p.code);

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p ON p.code IN (
  'portal:view', 'profile:update', 'profile:password', 'portal:message', 'portal:search',
  'course:view', 'course:join',
  'resource:view', 'learning:update:self', 'ai:ask',
  'exam:take', 'reservation:view', 'report:view',
  'teacher-certification:apply', 'resource-submission:create'
)
WHERE r.role_code = 'USER'
  AND NOT EXISTS (
    SELECT 1 FROM t_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p ON p.code IN (
  'teacher-certification:apply', 'resource-submission:create',
  'course:invite:manage', 'resource-submission:review'
)
WHERE r.role_code = 'TEACHER'
  AND NOT EXISTS (
    SELECT 1 FROM t_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p ON p.code IN (
  'teacher-certification:review', 'resource-submission:review', 'course:invite:manage'
)
WHERE r.role_code = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM t_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

CREATE TABLE IF NOT EXISTS t_teacher_certification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  school VARCHAR(120) NOT NULL,
  employee_no VARCHAR(80) NOT NULL,
  education_email VARCHAR(120) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  reviewer_id BIGINT NULL,
  review_comment VARCHAR(500) NULL,
  review_time DATETIME NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_teacher_cert_user_status (user_id, status, deleted),
  KEY idx_teacher_cert_status_time (status, create_time, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师认证申请';

CREATE TABLE IF NOT EXISTS t_class_invite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  teaching_class_id BIGINT NULL,
  invite_code VARCHAR(32) NOT NULL,
  expire_time DATETIME NULL,
  max_uses INT NULL,
  used_count INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_by BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_class_invite_code (invite_code, deleted),
  KEY idx_class_invite_course_status (course_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课堂邀请码';

CREATE TABLE IF NOT EXISTS t_resource_submission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submitter_id BIGINT NOT NULL,
  title VARCHAR(180) NOT NULL,
  resource_type VARCHAR(40) NOT NULL,
  knowledge_point VARCHAR(200) NULL,
  risk_type VARCHAR(120) NULL,
  tags VARCHAR(255) NULL,
  description VARCHAR(1000) NULL,
  url VARCHAR(500) NULL,
  file_path VARCHAR(500) NULL,
  original_filename VARCHAR(255) NULL,
  content_type VARCHAR(120) NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  reviewer_id BIGINT NULL,
  review_comment VARCHAR(500) NULL,
  review_time DATETIME NULL,
  public_resource_id BIGINT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_submission_status_time (status, create_time, deleted),
  KEY idx_submission_submitter (submitter_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公共资源投稿';

DROP PROCEDURE IF EXISTS add_column_if_missing;
