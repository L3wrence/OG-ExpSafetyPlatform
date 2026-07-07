-- Course teaching organization migration.
-- MySQL 8 repeatable script; preserves existing course APIs and data.

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

CALL add_column_if_missing('t_lab_course', 'credit', 'credit DECIMAL(4,1) NOT NULL DEFAULT 0 AFTER sort');
CALL add_column_if_missing('t_lab_course', 'hours', 'hours INT NOT NULL DEFAULT 0 AFTER credit');
CALL add_column_if_missing('t_lab_course', 'assessment_method', 'assessment_method VARCHAR(500) NULL DEFAULT NULL AFTER hours');
CALL add_column_if_missing('t_lab_course', 'learning_requirement', 'learning_requirement TEXT NULL AFTER assessment_method');
CALL add_column_if_missing('t_lab_course', 'allow_empty_publish', 'allow_empty_publish TINYINT NOT NULL DEFAULT 0 AFTER learning_requirement');
CALL add_column_if_missing('t_lab_course', 'archive_time', 'archive_time DATETIME NULL AFTER allow_empty_publish');

CALL add_column_if_missing('t_course_student', 'teaching_class_id', 'teaching_class_id BIGINT NULL AFTER course_id');
CALL add_column_if_missing('t_course_student', 'group_name', 'group_name VARCHAR(100) NULL DEFAULT NULL AFTER semester');
CALL add_column_if_missing('t_course_student', 'remark', 'remark VARCHAR(255) NULL DEFAULT NULL AFTER group_name');

DROP PROCEDURE IF EXISTS add_column_if_missing;

CREATE TABLE IF NOT EXISTS t_teaching_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    class_name VARCHAR(100) NOT NULL,
    teacher_id BIGINT NOT NULL,
    assistant_id BIGINT NULL,
    admin_class VARCHAR(200) NULL,
    semester VARCHAR(20) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    KEY idx_teaching_class_course (course_id, deleted),
    KEY idx_teaching_class_teacher (teacher_id, assistant_id, status)
);

INSERT INTO t_permission (name, code, type, parent_id, path, icon, sort)
SELECT p.name, p.code, 2, 0, NULL, NULL, p.sort
FROM (
    SELECT '课程发布' AS name, 'course:publish' AS code, 205 AS sort
    UNION ALL SELECT '课程归档', 'course:archive', 206
    UNION ALL SELECT '课程复制', 'course:copy', 207
    UNION ALL SELECT '教学班管理', 'course:class:manage', 208
    UNION ALL SELECT '课程学生管理', 'course:student:manage', 209
) p
WHERE NOT EXISTS (SELECT 1 FROM t_permission tp WHERE tp.code = p.code);

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r
JOIN t_permission p ON p.code IN (
    'course:publish', 'course:archive', 'course:copy',
    'course:class:manage', 'course:student:manage'
)
WHERE r.role_code IN ('ADMIN', 'TEACHER')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO t_teaching_class (course_id, class_name, teacher_id, admin_class, semester, status)
SELECT c.id, CONCAT(c.course_name, ' 默认教学班'), c.teacher_id, '示例行政班', c.semester, 1
FROM t_lab_course c
WHERE c.deleted = 0
  AND c.teacher_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM t_teaching_class tc WHERE tc.course_id = c.id AND tc.deleted = 0)
LIMIT 1;

INSERT INTO t_course_student (course_id, teaching_class_id, student_id, semester, group_name, status, join_time)
SELECT tc.course_id, tc.id, u.id, tc.semester, '第一组', 1, NOW()
FROM t_teaching_class tc
JOIN t_user u
JOIN t_user_role ur ON ur.user_id = u.id
JOIN t_role r ON r.id = ur.role_id AND r.role_code = 'STUDENT'
WHERE tc.deleted = 0
  AND u.status = 1
  AND NOT EXISTS (
      SELECT 1 FROM t_course_student cs
      WHERE cs.course_id = tc.course_id AND cs.student_id = u.id AND cs.deleted = 0
  )
LIMIT 1;
