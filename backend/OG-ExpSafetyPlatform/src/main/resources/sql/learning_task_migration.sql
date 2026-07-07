-- Learning task and process management migration.
-- MySQL 8 repeatable script; does not modify existing business tables.

CREATE TABLE IF NOT EXISTS t_learning_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    experiment_id BIGINT NOT NULL,
    task_name VARCHAR(120) NOT NULL,
    task_type VARCHAR(40) NOT NULL,
    target_resource_id BIGINT NULL,
    target_knowledge_id BIGINT NULL,
    target_paper_id BIGINT NULL,
    prerequisite_task_id BIGINT NULL,
    required_flag TINYINT NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 0,
    open_time DATETIME NULL,
    deadline DATETIME NULL,
    completion_rule VARCHAR(30) NOT NULL DEFAULT 'AUTO',
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    KEY idx_learning_task_experiment (experiment_id, status, sort),
    KEY idx_learning_task_course (course_id),
    KEY idx_learning_task_prerequisite (prerequisite_task_id)
);

CREATE TABLE IF NOT EXISTS t_learning_task_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    start_time DATETIME NULL,
    complete_time DATETIME NULL,
    source_type VARCHAR(40) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_learning_task_student_deleted (task_id, student_id, deleted),
    KEY idx_learning_task_record_student (student_id, status)
);

INSERT INTO t_learning_task (
    course_id, experiment_id, task_name, task_type, target_resource_id, required_flag, sort, completion_rule, status
)
SELECT e.course_id, e.id, '阅读实验指导书', 'READ_RESOURCE', r.id, 1, 10, 'AUTO', 1
FROM t_experiment e
JOIN t_resource r ON r.experiment_id = e.id AND r.deleted = 0 AND r.status = 1
WHERE e.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM t_learning_task t
      WHERE t.experiment_id = e.id AND t.task_type = 'READ_RESOURCE' AND t.deleted = 0
  )
ORDER BY r.required_flag DESC, r.sort ASC, r.id ASC
LIMIT 1;

INSERT INTO t_learning_task (
    course_id, experiment_id, task_name, task_type, prerequisite_task_id, required_flag, sort, completion_rule, status
)
SELECT e.course_id, e.id, '确认实验准备清单', 'CHECKLIST', t.id, 1, 90, 'CONFIRM', 1
FROM t_experiment e
JOIN t_learning_task t ON t.experiment_id = e.id AND t.deleted = 0
WHERE e.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM t_learning_task c
      WHERE c.experiment_id = e.id AND c.task_type = 'CHECKLIST' AND c.deleted = 0
  )
LIMIT 1;
