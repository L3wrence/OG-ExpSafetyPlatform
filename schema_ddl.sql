-- ============================================================
-- OG-ExpSafetyPlatform 数据库完整 DDL
-- 数据库: ogexpsafetyplatform
-- 字符集: utf8mb4
-- 共 43 张表
-- 导出日期: 2026-07-11
-- 用途: 导入 Navicat / DataGrip / MySQL Workbench 生成 E-R 图
-- ============================================================

CREATE DATABASE IF NOT EXISTS ogexpsafetyplatform
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE ogexpsafetyplatform;

CREATE TABLE `t_ai_chat_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `scene` varchar(50) NOT NULL,
  `question` text NOT NULL,
  `answer` text NOT NULL,
  `tool_name` varchar(100) DEFAULT NULL,
  `experiment_id` bigint DEFAULT NULL,
  `manual_revision` text,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_user_scene_time` (`user_id`,`scene`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_class_invite` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `teaching_class_id` bigint DEFAULT NULL,
  `invite_code` varchar(32) NOT NULL,
  `expire_time` datetime DEFAULT NULL,
  `max_uses` int DEFAULT NULL,
  `used_count` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `created_by` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_invite_code` (`invite_code`,`deleted`),
  KEY `idx_class_invite_course_status` (`course_id`,`status`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课堂邀请码';

CREATE TABLE `t_course_student` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `teaching_class_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  `semester` varchar(20) DEFAULT NULL,
  `group_name` varchar(100) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `status` tinyint DEFAULT '1',
  `join_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_student_deleted` (`course_id`,`student_id`,`deleted`),
  KEY `idx_course_student_semester` (`semester`),
  KEY `idx_course_student_user_course` (`student_id`,`status`,`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_discussion_reply` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `topic_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` text NOT NULL,
  `is_teacher_reply` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_discussion_reply_topic` (`topic_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_discussion_topic` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `experiment_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'OPEN',
  `is_anonymous` tinyint NOT NULL DEFAULT '0',
  `is_featured` tinyint NOT NULL DEFAULT '0',
  `reply_count` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_discussion_course` (`course_id`),
  KEY `idx_discussion_experiment` (`experiment_id`),
  KEY `idx_discussion_status_time` (`status`,`update_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_exam_answer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `knowledge_id` bigint DEFAULT NULL,
  `student_answer` varchar(500) DEFAULT NULL,
  `is_correct` tinyint DEFAULT NULL,
  `correct_flag` tinyint DEFAULT NULL,
  `score` int DEFAULT '0',
  `grading_comment` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_exam_answer_record` (`record_id`),
  KEY `idx_exam_answer_question` (`question_id`),
  KEY `idx_exam_answer_knowledge` (`knowledge_id`),
  KEY `idx_exam_answer_record_question` (`record_id`,`question_id`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_exam_paper` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  `experiment_id` bigint DEFAULT NULL,
  `total_score` int DEFAULT '100',
  `objective_score` int DEFAULT '100',
  `subjective_score` int DEFAULT '0',
  `pass_score` int DEFAULT '60',
  `duration` int DEFAULT '30',
  `attempt_limit` int NOT NULL DEFAULT '1',
  `show_answer_after_submit` tinyint NOT NULL DEFAULT '1',
  `admission_validity_days` int NOT NULL DEFAULT '180',
  `multiple_score_policy` varchar(30) NOT NULL DEFAULT 'ALL_OR_NOTHING',
  `random_enabled` tinyint NOT NULL DEFAULT '0',
  `random_count` int NOT NULL DEFAULT '0',
  `teacher_id` bigint NOT NULL,
  `status` varchar(20) DEFAULT 'DRAFT',
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint DEFAULT '0',
  `deleted` tinyint GENERATED ALWAYS AS (`is_deleted`) STORED,
  PRIMARY KEY (`id`),
  KEY `idx_exam_paper_course_status` (`course_id`,`status`),
  KEY `idx_exam_paper_experiment` (`experiment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_exam_paper_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `score` int NOT NULL,
  `order_num` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_question` (`paper_id`,`question_id`),
  KEY `idx_paper_question_paper` (`paper_id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_exam_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `paper_id` bigint NOT NULL,
  `experiment_id` bigint DEFAULT NULL,
  `total_score` int DEFAULT NULL,
  `objective_score` int DEFAULT NULL,
  `subjective_score` int DEFAULT NULL,
  `status` varchar(20) DEFAULT 'IN_PROGRESS',
  `question_snapshot_json` longtext,
  `auto_submit_flag` tinyint NOT NULL DEFAULT '0',
  `admission_id` bigint DEFAULT NULL,
  `passed` tinyint DEFAULT NULL,
  `start_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `submit_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `last_save_time` datetime DEFAULT NULL,
  `final_grade_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_exam_record_student` (`student_id`),
  KEY `idx_exam_record_paper` (`paper_id`),
  KEY `idx_exam_record_experiment` (`experiment_id`),
  KEY `idx_exam_record_student_exp_status` (`student_id`,`experiment_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_experiment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `exp_code` varchar(50) NOT NULL,
  `direction` varchar(100) DEFAULT NULL,
  `cover_url` varchar(255) DEFAULT NULL,
  `scenario_intro` text,
  `visual_theme` varchar(60) DEFAULT NULL,
  `description` text,
  `exp_name` varchar(120) NOT NULL,
  `objective` text,
  `principle` text,
  `equipment` text,
  `materials` text,
  `location` varchar(200) DEFAULT NULL,
  `applicable_classes` varchar(300) DEFAULT NULL,
  `risk_level` varchar(20) DEFAULT NULL,
  `hazard_sources` text,
  `risk_types` varchar(300) DEFAULT NULL,
  `ppe_requirements` text,
  `prerequisite_knowledge` text,
  `safety_requirement` text,
  `exam_required` tinyint NOT NULL DEFAULT '1',
  `admission_paper_id` bigint DEFAULT NULL,
  `duration_minutes` int DEFAULT '0',
  `safety_pass_score` int DEFAULT '60',
  `data_record_requirement` text,
  `abnormal_handling` text,
  `emergency_procedure` text,
  `report_template_url` varchar(500) DEFAULT NULL,
  `grading_criteria` text,
  `reservation_enabled` tinyint DEFAULT '1',
  `status` tinyint DEFAULT '1',
  `sort` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_exp_code_deleted` (`course_id`,`exp_code`,`deleted`),
  KEY `idx_experiment_course_status` (`course_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_experiment_admission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `experiment_id` bigint NOT NULL,
  `paper_id` bigint DEFAULT NULL,
  `record_id` bigint DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'VALID',
  `issued_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `valid_until` datetime DEFAULT NULL,
  `revoke_time` datetime DEFAULT NULL,
  `revoked_by` bigint DEFAULT NULL,
  `revoke_reason` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_admission_student_exp` (`student_id`,`experiment_id`,`status`,`deleted`),
  KEY `idx_admission_record` (`record_id`),
  KEY `idx_admission_valid_until` (`valid_until`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_experiment_step` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `experiment_id` bigint NOT NULL,
  `step_no` int NOT NULL,
  `title` varchar(120) NOT NULL,
  `content` text NOT NULL,
  `safety_tip` text,
  `media_type` varchar(20) NOT NULL DEFAULT 'TEXT',
  `media_url` varchar(500) DEFAULT NULL,
  `flowchart_data` text,
  `required_flag` tinyint DEFAULT '1',
  `estimated_minutes` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_experiment_step_deleted` (`experiment_id`,`step_no`,`deleted`),
  KEY `idx_step_experiment` (`experiment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_lab_course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_code` varchar(50) NOT NULL,
  `course_name` varchar(100) NOT NULL,
  `direction` varchar(50) DEFAULT NULL,
  `teacher_id` bigint DEFAULT NULL,
  `semester` varchar(20) DEFAULT NULL,
  `description` text,
  `cover_url` varchar(255) DEFAULT NULL,
  `tagline` varchar(160) DEFAULT NULL,
  `highlight_tags` varchar(300) DEFAULT NULL,
  `visual_theme` varchar(60) DEFAULT NULL,
  `status` tinyint DEFAULT '1',
  `sort` int DEFAULT '0',
  `credit` decimal(4,1) NOT NULL DEFAULT '0.0',
  `hours` int NOT NULL DEFAULT '0',
  `assessment_method` varchar(500) DEFAULT NULL,
  `learning_requirement` text,
  `allow_empty_publish` tinyint NOT NULL DEFAULT '0',
  `archive_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  KEY `idx_teacher_status_semester` (`teacher_id`,`status`,`semester`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_lab_time_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lab_id` bigint NOT NULL,
  `experiment_id` bigint DEFAULT NULL,
  `date` date NOT NULL,
  `slot_date` date GENERATED ALWAYS AS (`date`) STORED,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `capacity` int NOT NULL,
  `booked_count` int DEFAULT '0',
  `status` varchar(20) DEFAULT 'AVAILABLE',
  `create_by` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_time_slot_lab_date` (`lab_id`,`date`,`status`),
  KEY `idx_time_slot_experiment` (`experiment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_learning_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `resource_id` bigint NOT NULL,
  `experiment_id` bigint NOT NULL,
  `progress` decimal(5,2) DEFAULT '0.00',
  `duration_seconds` int DEFAULT '0',
  `last_position_seconds` int NOT NULL DEFAULT '0',
  `note` varchar(1000) DEFAULT NULL,
  `finish_flag` tinyint DEFAULT '0',
  `first_time` datetime DEFAULT NULL,
  `last_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_resource_deleted` (`student_id`,`resource_id`,`deleted`),
  KEY `idx_learning_experiment` (`experiment_id`),
  KEY `idx_learning_finish` (`finish_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_learning_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `experiment_id` bigint NOT NULL,
  `task_name` varchar(120) NOT NULL,
  `task_type` varchar(40) NOT NULL,
  `target_resource_id` bigint DEFAULT NULL,
  `target_paper_id` bigint DEFAULT NULL,
  `prerequisite_task_id` bigint DEFAULT NULL,
  `required_flag` tinyint NOT NULL DEFAULT '1',
  `sort` int NOT NULL DEFAULT '0',
  `open_time` datetime DEFAULT NULL,
  `deadline` datetime DEFAULT NULL,
  `completion_rule` varchar(30) NOT NULL DEFAULT 'AUTO',
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_learning_task_experiment` (`experiment_id`,`status`,`sort`),
  KEY `idx_learning_task_course` (`course_id`),
  KEY `idx_learning_task_prerequisite` (`prerequisite_task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_learning_task_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'COMPLETED',
  `start_time` datetime DEFAULT NULL,
  `complete_time` datetime DEFAULT NULL,
  `source_type` varchar(40) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_learning_task_student_deleted` (`task_id`,`student_id`,`deleted`),
  KEY `idx_learning_task_record_student` (`student_id`,`status`),
  KEY `idx_task_record_student_task` (`student_id`,`task_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL,
  `module` varchar(80) NOT NULL,
  `action` varchar(80) NOT NULL,
  `content` varchar(500) DEFAULT NULL,
  `result` varchar(20) DEFAULT 'SUCCESS',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operation_user_time` (`user_id`,`create_time`),
  KEY `idx_operation_module_time` (`module`,`create_time`),
  KEY `idx_operation_log_query` (`create_time`,`module`,`action`,`result`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `code` varchar(100) NOT NULL,
  `type` tinyint NOT NULL,
  `parent_id` bigint DEFAULT '0',
  `path` varchar(255) DEFAULT NULL,
  `icon` varchar(100) DEFAULT NULL,
  `sort` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_portal_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `biz_type` varchar(50) DEFAULT NULL,
  `biz_id` bigint DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `read_flag` tinyint DEFAULT '0',
  `read_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_message_user_read_time` (`user_id`,`read_flag`,`create_time`),
  KEY `idx_message_biz` (`biz_type`,`biz_id`),
  KEY `idx_message_user_biz` (`user_id`,`biz_type`,`biz_id`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_portal_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `target_role` varchar(50) DEFAULT 'ALL',
  `priority` varchar(20) DEFAULT 'MEDIUM',
  `status` tinyint DEFAULT '1',
  `publish_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `expire_time` datetime DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_notice_role_status_time` (`target_role`,`status`,`publish_time`),
  KEY `idx_notice_deleted` (`deleted`),
  KEY `idx_notice_admin_query` (`deleted`,`status`,`target_role`,`priority`,`publish_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(20) NOT NULL,
  `content` text NOT NULL,
  `options` json DEFAULT NULL,
  `answer` varchar(500) NOT NULL,
  `score` int DEFAULT '0',
  `analysis` varchar(500) DEFAULT NULL,
  `knowledge_point` varchar(200) DEFAULT NULL,
  `knowledge_id` bigint DEFAULT NULL,
  `experiment_id` bigint DEFAULT NULL,
  `risk_type` varchar(100) DEFAULT NULL,
  `related_resource_id` bigint DEFAULT NULL,
  `difficulty` varchar(20) DEFAULT 'MEDIUM',
  `course_id` bigint DEFAULT NULL,
  `create_by` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_question_course` (`course_id`),
  KEY `idx_question_knowledge` (`knowledge_point`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_recent_visit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(120) NOT NULL,
  `path` varchar(255) NOT NULL,
  `module` varchar(50) DEFAULT NULL,
  `visit_count` int DEFAULT '1',
  `last_visit_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recent_user_path` (`user_id`,`path`),
  KEY `idx_recent_user_time` (`user_id`,`last_visit_time`)
) ENGINE=InnoDB AUTO_INCREMENT=126 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_recommend_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `experiment_id` bigint DEFAULT NULL,
  `resource_id` bigint NOT NULL,
  `total_score` decimal(5,2) NOT NULL,
  `score_breakdown` json NOT NULL,
  `reason` varchar(500) NOT NULL,
  `clicked` tinyint DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recommend_student_experiment` (`student_id`,`experiment_id`),
  KEY `idx_recommend_resource` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `experiment_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `file_url` varchar(500) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'DRAFT',
  `submit_time` datetime DEFAULT NULL,
  `latest_submit_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint DEFAULT '0',
  `deleted` tinyint GENERATED ALWAYS AS (`is_deleted`) STORED,
  PRIMARY KEY (`id`),
  KEY `idx_report_student_status` (`student_id`,`status`),
  KEY `idx_report_experiment` (`experiment_id`),
  KEY `idx_report_student_experiment` (`student_id`,`experiment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_report_rubric_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `experiment_id` bigint NOT NULL,
  `item_name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `max_score` int NOT NULL,
  `order_no` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_rubric_experiment` (`experiment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_report_score` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  `score` int NOT NULL,
  `comment` varchar(500) DEFAULT NULL,
  `is_latest` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `grade_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_score_report_latest` (`report_id`,`is_latest`),
  KEY `idx_report_score_grade_time` (`grade_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_report_score_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_score_id` bigint NOT NULL,
  `rubric_item_id` bigint NOT NULL,
  `score` decimal(6,2) NOT NULL,
  `comment` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_score_rubric` (`report_score_id`,`rubric_item_id`),
  KEY `idx_score_item_score` (`report_score_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_report_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `experiment_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `schema_json` longtext NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_template_experiment` (`experiment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `time_slot_id` bigint NOT NULL,
  `lab_id` bigint NOT NULL,
  `experiment_id` bigint DEFAULT NULL,
  `purpose` varchar(500) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `teacher_id` bigint DEFAULT NULL,
  `review_comment` varchar(500) DEFAULT NULL,
  `review_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_reservation_student_status` (`student_id`,`status`),
  KEY `idx_reservation_slot_status` (`time_slot_id`,`status`),
  KEY `idx_reservation_experiment` (`experiment_id`),
  KEY `idx_reservation_student_exp_status` (`student_id`,`experiment_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint DEFAULT NULL,
  `experiment_id` bigint NOT NULL,
  `title` varchar(150) NOT NULL,
  `resource_type` varchar(20) DEFAULT NULL,
  `knowledge_point` varchar(150) DEFAULT NULL,
  `risk_type` varchar(100) DEFAULT NULL,
  `tags` varchar(300) DEFAULT NULL,
  `category` varchar(30) NOT NULL DEFAULT 'EXTENSION',
  `description` text,
  `url` varchar(500) DEFAULT NULL,
  `file_path` varchar(500) DEFAULT NULL,
  `original_filename` varchar(255) DEFAULT NULL,
  `content_type` varchar(100) DEFAULT NULL,
  `file_size` bigint DEFAULT '0',
  `required_flag` tinyint DEFAULT '0',
  `completion_rule` varchar(30) NOT NULL DEFAULT 'CONFIRM',
  `min_study_seconds` int NOT NULL DEFAULT '0',
  `min_progress` int NOT NULL DEFAULT '100',
  `open_time` datetime DEFAULT NULL,
  `close_time` datetime DEFAULT NULL,
  `open_scope` varchar(30) NOT NULL DEFAULT 'COURSE',
  `invalid_flag` tinyint NOT NULL DEFAULT '0',
  `invalid_check_time` datetime DEFAULT NULL,
  `view_count` int DEFAULT '0',
  `download_count` int NOT NULL DEFAULT '0',
  `favorite_count` int NOT NULL DEFAULT '0',
  `like_count` int NOT NULL DEFAULT '0',
  `comment_count` int NOT NULL DEFAULT '0',
  `rating_avg` decimal(3,2) NOT NULL DEFAULT '0.00',
  `rating_count` int NOT NULL DEFAULT '0',
  `status` tinyint DEFAULT '1',
  `sort` int DEFAULT '0',
  `upload_user_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_resource_experiment_type_status` (`experiment_id`,`resource_type`,`status`),
  KEY `idx_resource_public_course` (`status`,`invalid_flag`,`open_scope`,`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_resource_interaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `favorite_flag` tinyint NOT NULL DEFAULT '0',
  `like_flag` tinyint NOT NULL DEFAULT '0',
  `rating` decimal(2,1) DEFAULT NULL,
  `comment` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_user_deleted` (`resource_id`,`user_id`,`deleted`),
  KEY `idx_resource_interaction_resource` (`resource_id`),
  KEY `idx_resource_interaction_user` (`user_id`,`favorite_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_resource_submission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submitter_id` bigint NOT NULL,
  `title` varchar(180) NOT NULL,
  `resource_type` varchar(40) NOT NULL,
  `knowledge_point` varchar(200) DEFAULT NULL,
  `risk_type` varchar(120) DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `url` varchar(500) DEFAULT NULL,
  `file_path` varchar(500) DEFAULT NULL,
  `original_filename` varchar(255) DEFAULT NULL,
  `content_type` varchar(120) DEFAULT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'PENDING',
  `reviewer_id` bigint DEFAULT NULL,
  `review_comment` varchar(500) DEFAULT NULL,
  `review_time` datetime DEFAULT NULL,
  `public_resource_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_submission_status_time` (`status`,`create_time`,`deleted`),
  KEY `idx_submission_submitter` (`submitter_id`,`status`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公共资源投稿';

CREATE TABLE `t_resource_timeline_note` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_id` bigint NOT NULL COMMENT '资源ID',
  `experiment_id` bigint DEFAULT NULL COMMENT '实验ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `position_seconds` int NOT NULL DEFAULT '0' COMMENT '资源时间点秒数',
  `note_type` varchar(20) NOT NULL DEFAULT 'NOTE' COMMENT 'NOTE/QUESTION/RISK',
  `content` varchar(1000) NOT NULL COMMENT '笔记或问题内容',
  `visibility` varchar(20) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE/COURSE',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_resource_position` (`resource_id`,`position_seconds`,`deleted`),
  KEY `idx_experiment_type` (`experiment_id`,`note_type`,`deleted`),
  KEY `idx_user_resource` (`user_id`,`resource_id`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资源时间点笔记与问题';

CREATE TABLE `t_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

CREATE TABLE `t_role_permission` (
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_step_learning_record` (
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_teacher_certification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `school` varchar(120) NOT NULL,
  `employee_no` varchar(80) NOT NULL,
  `education_email` varchar(120) NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'PENDING',
  `reviewer_id` bigint DEFAULT NULL,
  `review_comment` varchar(500) DEFAULT NULL,
  `review_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_teacher_cert_user_status` (`user_id`,`status`,`deleted`),
  KEY `idx_teacher_cert_status_time` (`status`,`create_time`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师认证申请';

CREATE TABLE `t_teaching_class` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `class_name` varchar(100) NOT NULL,
  `teacher_id` bigint NOT NULL,
  `assistant_id` bigint DEFAULT NULL,
  `admin_class` varchar(200) DEFAULT NULL,
  `semester` varchar(20) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_teaching_class_course` (`course_id`,`deleted`),
  KEY `idx_teaching_class_teacher` (`teacher_id`,`assistant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(64) NOT NULL,
  `expire_time` datetime NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`)
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='令牌表';

CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '学号/工号',
  `password` varchar(64) NOT NULL COMMENT 'MD5加密密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '电话',
  `avatar_url` varchar(500) DEFAULT NULL,
  `major` varchar(100) DEFAULT NULL,
  `class_name` varchar(100) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `status` tinyint DEFAULT '1' COMMENT '1启用 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE `t_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';

CREATE TABLE `t_user_shortcut` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(80) NOT NULL,
  `path` varchar(255) NOT NULL,
  `icon` varchar(50) DEFAULT NULL,
  `sort` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_shortcut_user_sort` (`user_id`,`sort`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 总计 43 张表
