CREATE TABLE IF NOT EXISTS `t_step_learning_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `step_id` bigint NOT NULL,
  `experiment_id` bigint NOT NULL,
  `complete_time` datetime NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_step_learning_student_step_deleted` (`student_id`,`step_id`,`deleted`),
  KEY `idx_step_learning_student_experiment` (`student_id`,`experiment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
