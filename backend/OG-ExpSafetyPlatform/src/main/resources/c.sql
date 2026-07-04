CREATE DATABASE  IF NOT EXISTS `og-expsafetyplatform` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `og-expsafetyplatform`;
-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: og-expsafetyplatform
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;



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
--
-- Table structure for table ` t_exam_paper_question`
--

DROP TABLE IF EXISTS `t_exam_paper_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_exam_paper_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `score` int NOT NULL,
  `order_num` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table ` t_exam_paper_question`
--

LOCK TABLES `t_exam_paper_question` WRITE;
/*!40000 ALTER TABLE `t_exam_paper_question` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_exam_paper_question` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table ` t_lab_time_slot`
--

DROP TABLE IF EXISTS `t_lab_time_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_lab_time_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lab_id` bigint NOT NULL,
  `date` date NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `capacity` int NOT NULL,
  `booked_count` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'AVAILABLE',
  `create_by` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table ` t_lab_time_slot`
--

LOCK TABLES `t_lab_time_slot` WRITE;
/*!40000 ALTER TABLE `t_lab_time_slot` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_lab_time_slot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_ai_chat_record`
--

DROP TABLE IF EXISTS `t_ai_chat_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_ai_chat_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `scene` varchar(50) NOT NULL,
  `question` text NOT NULL,
  `answer` text NOT NULL,
  `tool_name` varchar(100) DEFAULT NULL,
  `experiment_id` bigint DEFAULT NULL,
  `manual_revision` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_ai_chat_record`
--

LOCK TABLES `t_ai_chat_record` WRITE;
/*!40000 ALTER TABLE `t_ai_chat_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_ai_chat_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_exam_answer`
--

DROP TABLE IF EXISTS `t_exam_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_exam_answer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `student_answer` varchar(500) DEFAULT NULL,
  `is_correct` tinyint(1) DEFAULT NULL,
  `score` int DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_exam_answer`
--

LOCK TABLES `t_exam_answer` WRITE;
/*!40000 ALTER TABLE `t_exam_answer` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_exam_answer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_exam_paper`
--

DROP TABLE IF EXISTS `t_exam_paper`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_exam_paper` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `course_id` bigint NOT NULL,
  `total_score` int NOT NULL DEFAULT '100',
  `pass_score` int NOT NULL DEFAULT '60',
  `duration` int NOT NULL DEFAULT '30',
  `teacher_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_exam_paper`
--

LOCK TABLES `t_exam_paper` WRITE;
/*!40000 ALTER TABLE `t_exam_paper` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_exam_paper` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_exam_record`
--

DROP TABLE IF EXISTS `t_exam_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_exam_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `paper_id` bigint NOT NULL,
  `total_score` int DEFAULT NULL,
  `objective_score` int DEFAULT NULL,
  `subjective_score` int DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'IN_PROGRESS',
  `passed` tinyint(1) DEFAULT NULL,
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `submit_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_exam_record`
--

LOCK TABLES `t_exam_record` WRITE;
/*!40000 ALTER TABLE `t_exam_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_exam_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_question`
--

DROP TABLE IF EXISTS `t_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(20) NOT NULL,
  `content` text NOT NULL,
  `options` json DEFAULT NULL,
  `answer` varchar(500) NOT NULL,
  `score` int NOT NULL DEFAULT '0',
  `analysis` varchar(500) DEFAULT NULL,
  `knowledge_point` varchar(200) DEFAULT NULL,
  `difficulty` varchar(10) NOT NULL DEFAULT 'MEDIUM',
  `course_id` bigint DEFAULT NULL,
  `create_by` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_question`
--

LOCK TABLES `t_question` WRITE;
/*!40000 ALTER TABLE `t_question` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_question` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_recommend_record`
--

DROP TABLE IF EXISTS `t_recommend_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_recommend_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `experiment_id` bigint DEFAULT NULL,
  `resource_id` bigint NOT NULL,
  `total_score` decimal(5,2) NOT NULL,
  `score_breakdown` json NOT NULL,
  `reason` varchar(500) NOT NULL,
  `clicked` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_recommend_record`
--

LOCK TABLES `t_recommend_record` WRITE;
/*!40000 ALTER TABLE `t_recommend_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_recommend_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_report`
--

DROP TABLE IF EXISTS `t_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `experiment_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `file_url` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `submit_time` datetime DEFAULT NULL,
  `latest_submit_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_report`
--

LOCK TABLES `t_report` WRITE;
/*!40000 ALTER TABLE `t_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_report_score`
--

DROP TABLE IF EXISTS `t_report_score`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_report_score` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  `score` int NOT NULL,
  `comment` varchar(500) DEFAULT NULL,
  `is_latest` tinyint(1) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_report_score`
--

LOCK TABLES `t_report_score` WRITE;
/*!40000 ALTER TABLE `t_report_score` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_report_score` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_reservation`
--

DROP TABLE IF EXISTS `t_reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `time_slot_id` bigint NOT NULL,
  `lab_id` bigint NOT NULL,
  `experiment_id` bigint DEFAULT NULL,
  `purpose` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `teacher_id` bigint DEFAULT NULL,
  `review_comment` varchar(500) DEFAULT NULL,
  `review_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_reservation`
--

LOCK TABLES `t_reservation` WRITE;
/*!40000 ALTER TABLE `t_reservation` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_reservation` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-02 18:17:46

-- ==================== 成员A：RBAC 权限体系 ====================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '学号/工号',
    password    VARCHAR(64)  NOT NULL COMMENT 'MD5加密密码',
    real_name   VARCHAR(50)  COMMENT '真实姓名',
    phone       VARCHAR(20)  COMMENT '电话',
    status      TINYINT      DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户表';

-- 2. 角色表
CREATE TABLE IF NOT EXISTS t_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(50)  NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(200) COMMENT '描述',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
) COMMENT '角色表';

-- 3. 权限表
CREATE TABLE IF NOT EXISTS t_permission (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL COMMENT '权限名称',
    code        VARCHAR(100) NOT NULL UNIQUE COMMENT '权限标识，如 user:create',
    type        TINYINT      NOT NULL COMMENT '1菜单 2按钮',
    parent_id   BIGINT       DEFAULT 0 COMMENT '父级ID',
    path        VARCHAR(200) COMMENT '路由路径',
    icon        VARCHAR(100) COMMENT '图标',
    sort        INT          DEFAULT 0 COMMENT '排序',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
) COMMENT '权限表';

-- 4. 用户角色关联表
CREATE TABLE IF NOT EXISTS t_user_role (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
) COMMENT '用户角色关联表';

-- 5. 角色权限关联表
CREATE TABLE IF NOT EXISTS t_role_permission (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL
) COMMENT '角色权限关联表';

-- 6. Token表
CREATE TABLE IF NOT EXISTS t_token (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    token       VARCHAR(64) NOT NULL UNIQUE,
    expire_time DATETIME    NOT NULL,
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP
) COMMENT '令牌表';

-- ==================== 初始数据 ====================

-- 角色数据（INSERT IGNORE：已存在则跳过，可重复执行）
INSERT IGNORE INTO t_role (role_name, role_code, description) VALUES
('系统管理员', 'ROLE_ADMIN',   '管理系统用户和权限'),
('教师',       'ROLE_TEACHER', '管理课程、考试和实验'),
('学生',       'ROLE_STUDENT', '参加实验学习和考试');

-- 权限数据（菜单）
INSERT IGNORE INTO t_permission (name, code, type, parent_id, path, icon, sort) VALUES
('系统管理', 'system',    1, 0, '/system',            'Setting',    1),
('用户管理', 'user:list', 1, 1, '/system/users',       'User',       1),
('角色管理', 'role:list', 1, 1, '/system/roles',       'UserFilled', 2),
('权限管理', 'perm:list', 1, 1, '/system/permissions', 'Lock',       3);

-- 权限数据（按钮）
INSERT IGNORE INTO t_permission (name, code, type, parent_id, sort) VALUES
('创建用户', 'user:create', 2, 2, 1),
('编辑用户', 'user:update', 2, 2, 2),
('删除用户', 'user:delete', 2, 2, 3),
('创建角色', 'role:create', 2, 3, 1),
('编辑角色', 'role:update', 2, 3, 2),
('删除角色', 'role:delete', 2, 3, 3),
('分配权限', 'role:assign', 2, 3, 4),
('创建权限', 'perm:create', 2, 4, 1),
('编辑权限', 'perm:update', 2, 4, 2),
('删除权限', 'perm:delete', 2, 4, 3);

-- 用户数据（密码 123456 的 MD5）
INSERT IGNORE INTO t_user (username, password, real_name, phone, status) VALUES
('admin',     'e10adc3949ba59abbe56e057f20f883e', '系统管理员', '13800000000', 1),
('T2024001',  'e10adc3949ba59abbe56e057f20f883e', '张老师',     '13800000001', 1),
('T2024002',  'e10adc3949ba59abbe56e057f20f883e', '李老师',     '13800000002', 1),
('S2024001',  'e10adc3949ba59abbe56e057f20f883e', '张三',       '13900000001', 1),
('S2024002',  'e10adc3949ba59abbe56e057f20f883e', '李四',       '13900000002', 1),
('S2024003',  'e10adc3949ba59abbe56e057f20f883e', '王五',       '13900000003', 1);

-- 用户角色关联
INSERT IGNORE INTO t_user_role (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 2), (4, 3), (5, 3), (6, 3);

-- 给管理员分配所有权限
INSERT IGNORE INTO t_role_permission (role_id, permission_id)
SELECT 1, id FROM t_permission;

-- ==================== 成员C：测试数据（可重复执行） ====================

-- 关闭安全更新模式，允许无 WHERE 的 DELETE
SET SQL_SAFE_UPDATES = 0;

-- 先清理旧数据（按外键依赖顺序删除，避免外键约束报错）
DELETE FROM t_recommend_record;
DELETE FROM t_ai_chat_record;
DELETE FROM t_report_score;
DELETE FROM t_report;
DELETE FROM t_reservation;
DELETE FROM t_lab_time_slot;
DELETE FROM t_exam_answer;
DELETE FROM t_exam_record;
DELETE FROM t_exam_paper_question;
DELETE FROM t_exam_paper;
DELETE FROM t_question;

-- ---------- 考题（t_question） ----------
INSERT INTO t_question (id, type, content, options, answer, score, analysis, knowledge_point, difficulty, course_id, create_by) VALUES
(1, 'SINGLE', '钻井作业中，防止井喷最关键的措施是？',
    '[{"key":"A","label":"增加钻压"},{"key":"B","label":"控制钻井液密度"},{"key":"C","label":"提高转速"},{"key":"D","label":"加大排量"}]',
    'B', 5, '钻井液密度是平衡地层压力的关键参数，密度过低会导致地层流体侵入井筒引发井喷。', '井控安全', 'EASY', 1, 2),
(2, 'SINGLE', 'H2S气体在空气中的致死浓度约为？',
    '[{"key":"A","label":"10ppm"},{"key":"B","label":"100ppm"},{"key":"C","label":"500ppm"},{"key":"D","label":"1000ppm"}]',
    'D', 5, 'H2S浓度达到1000ppm时，吸入一口即可导致瞬间死亡。', '有毒气体防护', 'MEDIUM', 1, 2),
(3, 'MULTIPLE', '下列哪些属于钻井作业中的个人防护装备？',
    '[{"key":"A","label":"安全帽"},{"key":"B","label":"防砸鞋"},{"key":"C","label":"防护眼镜"},{"key":"D","label":"蓝牙耳机"}]',
    'A,B,C', 5, '钻井现场必须佩戴安全帽、防砸鞋和防护眼镜，蓝牙耳机不属于安全装备。', '个人防护', 'EASY', 1, 2),
(4, 'JUDGE', '在防喷器安装完成后，可以直接进行下一步作业，不需要试压。',
    null, 'B', 5, '防喷器安装后必须进行试压检验，确认密封性能合格后方可继续作业。', '井控设备', 'MEDIUM', 1, 2),
(5, 'SHORT_ANSWER', '简述钻井作业中发生溢流时的应急处理流程。',
    null, '关闭防喷器→停泵→观察套压和立管压力→采用司钻法或工程师法压井→恢复正常循环', 10,
    '溢流是井喷的前兆，必须按照正确的关井和压井程序处理。', '应急处理', 'HARD', 1, 2),

(6, 'SINGLE', '采油井口装置中，用于悬挂油管的是？',
    '[{"key":"A","label":"套管头"},{"key":"B","label":"油管头"},{"key":"C","label":"采油树"},{"key":"D","label":"法兰"}]',
    'B', 5, '油管头安装在套管头上方，用于悬挂油管并密封油套环空。', '井口装置', 'EASY', 2, 2),
(7, 'MULTIPLE', '下列哪些措施可以预防抽油机伤害事故？',
    '[{"key":"A","label":"安装防护栏"},{"key":"B","label":"张贴警示标志"},{"key":"C","label":"不停机进行检修"},{"key":"D","label":"定期检查刹车系统"}]',
    'A,B,D', 5, '检修抽油机时必须停机并切断电源，禁止在运转状态下进行检修作业。', '机械安全', 'MEDIUM', 2, 2),
(8, 'JUDGE', '采油过程中，井口回压越高，油井产量越大。',
    null, 'A', 5, '井口回压越高，生产压差越小，油井产量越低。适当降低回压有利于提高产量。', '采油工艺', 'EASY', 2, 2),
(9, 'SINGLE', '油气储运中，油罐的防火堤容积应不小于罐区最大储罐容量的？',
    '[{"key":"A","label":"50%"},{"key":"B","label":"80%"},{"key":"C","label":"100%"},{"key":"D","label":"120%"}]',
    'C', 5, '根据GB 50183规定，防火堤有效容积不应小于罐组内最大储罐的容量。', '储运安全', 'MEDIUM', 3, 2),
(10, 'SHORT_ANSWER', '简述输油管道泄漏时应采取哪些应急措施。',
    null, '立即关闭泄漏点上下游截断阀→启动应急预案→疏散无关人员→设置警戒区→通知相关部门→组织抢修', 10,
    '管道泄漏可能引发火灾爆炸，快速截断和疏散是首要任务。', '应急处理', 'HARD', 3, 2);

-- ---------- 试卷（t_exam_paper） ----------
INSERT INTO t_exam_paper (id, title, description, course_id, total_score, pass_score, duration, teacher_id, status, start_time, end_time) VALUES
(1, '钻井工程安全考核', '钻井作业安全操作规范与应急处理知识考核，共5题，满分30分，18分及格。',
    1, 30, 18, 30, 2, 'PUBLISHED', '2026-07-01 08:00:00', '2026-08-01 23:59:59'),
(2, '采油与储运安全考核', '采油工程和油气储运安全知识考核，共5题，满分30分，18分及格。',
    2, 30, 18, 25, 2, 'PUBLISHED', '2026-07-01 08:00:00', '2026-08-01 23:59:59');

-- ---------- 试卷-题目关联（t_exam_paper_question） ----------
-- 试卷1：题目1-5
INSERT INTO t_exam_paper_question (paper_id, question_id, score, order_num) VALUES
(1, 1, 5, 1), (1, 2, 5, 2), (1, 3, 5, 3), (1, 4, 5, 4), (1, 5, 10, 5);
-- 试卷2：题目6-10
INSERT INTO t_exam_paper_question (paper_id, question_id, score, order_num) VALUES
(2, 6, 5, 1), (2, 7, 5, 2), (2, 8, 5, 3), (2, 9, 5, 4), (2, 10, 10, 5);

-- ---------- 考试记录（t_exam_record） ----------
-- 张三(S2024001=4)：考了试卷1，通过了（错1题，得25分）
INSERT INTO t_exam_record (id, student_id, paper_id, total_score, objective_score, status, passed, start_time, submit_time) VALUES
(1, 4, 1, 25, 25, 'SUBMITTED', 1, '2026-07-03 09:00:00', '2026-07-03 09:25:00');
-- 李四(S2024002=5)：考了试卷1，未通过（错3题，得15分）
INSERT INTO t_exam_record (id, student_id, paper_id, total_score, objective_score, status, passed, start_time, submit_time) VALUES
(2, 5, 1, 15, 15, 'SUBMITTED', 0, '2026-07-03 09:10:00', '2026-07-03 09:30:00');
-- 王五(S2024003=6)：考了试卷2，通过了（全对，得30分）
INSERT INTO t_exam_record (id, student_id, paper_id, total_score, objective_score, status, passed, start_time, submit_time) VALUES
(3, 6, 2, 30, 30, 'SUBMITTED', 1, '2026-07-03 10:00:00', '2026-07-03 10:20:00');

-- ---------- 答题明细（t_exam_answer） ----------
-- 张三的答题（试卷1，记录1）：第2题答错了
INSERT INTO t_exam_answer (record_id, question_id, student_answer, is_correct, score) VALUES
(1, 1, 'B', 1, 5),
(1, 2, 'C', 0, 0),  -- 答错了（正确答案D）
(1, 3, 'A,B,C', 1, 5),
(1, 4, 'B', 1, 5),
(1, 5, '关闭防喷器，停泵观察压力，采用工程师法压井', 1, 10);

-- 李四的答题（试卷1，记录2）：第1、2、4题答错
INSERT INTO t_exam_answer (record_id, question_id, student_answer, is_correct, score) VALUES
(2, 1, 'A', 0, 0),
(2, 2, 'B', 0, 0),
(2, 3, 'A,B,C', 1, 5),
(2, 4, 'A', 0, 0),
(2, 5, '关闭防喷器后等待上级指示', 1, 10);

-- 王五的答题（试卷2，记录3）：全部正确
INSERT INTO t_exam_answer (record_id, question_id, student_answer, is_correct, score) VALUES
(3, 6, 'B', 1, 5),
(3, 7, 'A,B,D', 1, 5),
(3, 8, 'A', 1, 5),
(3, 9, 'C', 1, 5),
(3, 10, '关闭上下游截断阀，启动应急预案，疏散人员，设警戒区，通知相关部门组织抢修', 1, 10);

-- ---------- 实验时间段（t_lab_time_slot） ----------
INSERT INTO t_lab_time_slot (id, lab_id, date, start_time, end_time, capacity, booked_count, status, create_by) VALUES
(1, 1, '2026-07-06', '08:00', '10:00', 20, 1, 'AVAILABLE', 2),
(2, 1, '2026-07-06', '10:00', '12:00', 20, 0, 'AVAILABLE', 2),
(3, 1, '2026-07-06', '14:00', '16:00', 15, 0, 'AVAILABLE', 2),
(4, 2, '2026-07-07', '08:00', '10:00', 20, 0, 'AVAILABLE', 2),
(5, 2, '2026-07-07', '10:00', '12:00', 20, 0, 'AVAILABLE', 2),
(6, 1, '2026-07-08', '08:00', '10:00', 20, 0, 'CLOSED',  2);

-- ---------- 预约记录（t_reservation） ----------
INSERT INTO t_reservation (id, student_id, time_slot_id, lab_id, experiment_id, purpose, status, teacher_id, review_comment, review_time) VALUES
(1, 4, 1, 1, 1, '钻井液性能测试实验', 'APPROVED', 2, '已通过安全考核，请准时参加', '2026-07-03 10:30:00'),
(2, 5, 1, 1, 1, '钻井液性能测试实验', 'PENDING',  NULL, NULL, NULL);

-- ---------- 实验报告（t_report） ----------
INSERT INTO t_report (id, student_id, experiment_id, title, content, status, submit_time, latest_submit_time) VALUES
(1, 4, 1, '钻井液密度对井壁稳定性影响实验报告',
    '## 实验目的\n研究钻井液密度变化对井壁稳定性的影响规律。\n\n## 实验步骤\n1. 配制不同密度的钻井液\n2. 测定各密度下的井壁失稳临界压差\n3. 记录数据并分析\n\n## 数据记录\n| 密度(g/cm³) | 临界压差(MPa) |\n|------------|-------------|\n| 1.10 | 2.3 |\n| 1.20 | 3.1 |\n| 1.30 | 4.0 |\n\n## 结论\n钻井液密度每增加0.1g/cm³，井壁稳定性临界压差约提高0.8MPa。',
    'GRADED', '2026-07-03 14:00:00', '2026-07-03 14:00:00');

-- ---------- 评分记录（t_report_score） ----------
INSERT INTO t_report_score (report_id, teacher_id, score, comment, is_latest) VALUES
(1, 2, 88, '实验步骤记录清晰，数据分析合理。建议增加误差分析和安全注意事项部分，使报告更加完整。', 1);

-- ---------- AI问答记录（t_ai_chat_record） ----------
INSERT INTO t_ai_chat_record (user_id, scene, question, answer, tool_name, experiment_id) VALUES
(4, 'SAFETY_QA', '进行高压反应釜实验时应注意哪些安全事项？',
    '【AI辅助生成】高压反应釜实验安全注意事项：1. 实验前检查釜体密封性和安全阀状态；2. 严禁超压超温运行；3. 反应过程中必须有人值守；4. 泄压操作时应缓慢进行；5. 穿戴防爆面罩和耐高温手套。',
    'LocalKB+Template', 1),
(5, 'ERROR_EXPLAIN', '为什么钻井液密度过低会导致井涌？',
    '【AI辅助生成】当钻井液密度过低时，井筒内液柱压力小于地层孔隙压力，地层流体（油、气、水）会侵入井筒，形成溢流。若不及时控制，溢流发展为井涌，最终可能导致井喷。因此必须根据地层压力系数合理设计钻井液密度，保持井底压力略大于地层压力。',
    'LocalKB+Template', 1);

-- ---------- 推荐记录（t_recommend_record） ----------
INSERT INTO t_recommend_record (student_id, experiment_id, resource_id, total_score, score_breakdown, reason) VALUES
(5, 1, 1, 88.50, '{"knowledgeMatch":80,"errorRelevance":85,"newness":100,"popularity":60,"difficultyMatch":90}',
    '因为您在井控安全和有毒气体防护知识点有错题，该资源尚未学习，建议优先学习。'),
(5, 1, 2, 72.00, '{"knowledgeMatch":75,"errorRelevance":60,"newness":80,"popularity":45,"difficultyMatch":70}',
    '与当前实验内容匹配，根据您的学习情况自动推荐。');
-- ==================== 成员B数据：课程+实验+资源（推荐算法依赖） ====================

SET SQL_SAFE_UPDATES = 0;
DELETE FROM t_resource;
DELETE FROM t_experiment;
DELETE FROM t_lab_course;

INSERT INTO t_lab_course (id, course_code, course_name, direction, teacher_id, semester, description, status, sort) VALUES
(1, 'DRILL001', '钻井工程实验', '钻井方向', 2, '2026-2027-1', '学习钻井液配制、性能测试及井控安全操作', 1, 1),
(2, 'PROD001',  '采油工程实验', '采油方向', 2, '2026-2027-1', '掌握采油工艺、抽油机操作与维护', 1, 2),
(3, 'STOR001',  '油气储运实验', '储运方向', 3, '2026-2027-1', '学习油气储存、运输安全规范及应急处理', 1, 3);

INSERT INTO t_experiment (id, course_id, exp_code, exp_name, objective, principle, equipment, risk_level, duration_minutes, safety_pass_score, status, sort) VALUES
(1, 1, 'DRILL-E01', '钻井液性能测试', '掌握钻井液密度、粘度测定方法', '钻井液性能直接影响井壁稳定和井控安全', '密度计、粘度计、六速旋转粘度计', 'MEDIUM', 90, 60, 1, 1),
(2, 1, 'DRILL-E02', '防喷器拆装与试压', '掌握防喷器结构及安装试压流程', '防喷器是井控核心设备，安装后必须试压', '环形防喷器、闸板防喷器、试压泵', 'HARD', 120, 70, 1, 2),
(3, 2, 'PROD-E01', '抽油机操作与维护', '掌握抽油机启停操作和日常维护', '抽油机是采油主要举升设备，需定期维护', '抽油机模型、示功仪', 'MEDIUM', 90, 60, 1, 1),
(4, 3, 'STOR-E01', '油罐区安全巡检', '掌握油罐区安全巡检要点', '油罐区是储运核心场所，防火防爆是首要任务', '可燃气体检测仪、测温仪', 'HARD', 90, 70, 1, 1);

INSERT INTO t_resource (id, experiment_id, title, resource_type, url, file_path, file_size, required_flag, view_count, status, sort, upload_user_id) VALUES
(1, 1, '钻井液设计与维护实验指导书', 'DOC', '', '/files/drilling-fluid-guide.pdf', 2048000, 1, 156, 1, 1, 2),
(2, 1, '钻井液密度测定操作视频', 'VIDEO', 'https://example.com/video/drilling-density', '', 0, 0, 423, 1, 2, 2),
(3, 1, '井控安全操作规范（GB/T 31033）', 'DOC', '', '/files/well-control-safety.pdf', 5120000, 1, 89, 1, 3, 2),
(4, 2, '防喷器结构与工作原理PPT', 'PPT', '', '/files/bop-structure.pptx', 4096000, 1, 234, 1, 1, 2),
(5, 2, '防喷器试压操作流程', 'DOC', '', '/files/bop-pressure-test.pdf', 1024000, 1, 67, 1, 2, 2),
(6, 3, '抽油机操作规程与维护手册', 'DOC', '', '/files/pumping-unit-manual.pdf', 3072000, 1, 198, 1, 1, 2),
(7, 3, '抽油机示功图分析教程', 'VIDEO', 'https://example.com/video/dynamometer', '', 0, 0, 312, 1, 2, 2),
(8, 4, '油罐区安全管理规范', 'DOC', '', '/files/tank-farm-safety.pdf', 2560000, 1, 145, 1, 1, 3),
(9, 4, '油气储运防火防爆安全知识', 'DOC', '', '/files/storage-fire-safety.pdf', 1840000, 1, 178, 1, 2, 3),
(10, 4, '可燃气体检测仪使用教程', 'VIDEO', 'https://example.com/video/gas-detector', '', 0, 0, 267, 1, 3, 3);

-- 恢复安全更新模式
SET SQL_SAFE_UPDATES = 1;
