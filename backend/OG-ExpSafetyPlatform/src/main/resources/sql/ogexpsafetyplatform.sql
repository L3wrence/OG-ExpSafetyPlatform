CREATE DATABASE  IF NOT EXISTS `ogexpsafetyplatform` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `ogexpsafetyplatform`;
-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: ogexpsafetyplatform
-- ------------------------------------------------------
-- Server version	8.0.46

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
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_user_scene_time` (`user_id`,`scene`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_ai_chat_record`
--

LOCK TABLES `t_ai_chat_record` WRITE;
/*!40000 ALTER TABLE `t_ai_chat_record` DISABLE KEYS */;
INSERT INTO `t_ai_chat_record` VALUES (1,5,'RESOURCE_EXPLAIN','用一句话解释钻井液密度为什么影响井控？','钻井液密度决定井底压力窗口，过低会诱发溢流，过高可能压漏地层。','AI解释',1,NULL,'2026-07-08 12:04:06');
/*!40000 ALTER TABLE `t_ai_chat_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_class_invite`
--

DROP TABLE IF EXISTS `t_class_invite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_class_invite`
--

LOCK TABLES `t_class_invite` WRITE;
/*!40000 ALTER TABLE `t_class_invite` DISABLE KEYS */;
INSERT INTO `t_class_invite` VALUES (1,1,1,'MUD2026','2026-09-06 12:04:06',100,2,1,3,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,2,2,'PIPE2026','2026-09-06 12:04:06',100,1,1,3,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(3,3,3,'HSE2026','2026-09-06 12:04:06',100,1,1,4,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_class_invite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_course_student`
--

DROP TABLE IF EXISTS `t_course_student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_course_student`
--

LOCK TABLES `t_course_student` WRITE;
/*!40000 ALTER TABLE `t_course_student` DISABLE KEYS */;
INSERT INTO `t_course_student` VALUES (1,1,1,5,'2026秋','A组','演示数据',1,'2026-06-28 12:04:06','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,1,1,7,'2026秋','B组','演示数据',1,'2026-06-28 12:04:06','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(3,2,2,6,'2026秋','管输小组','演示数据',1,'2026-06-28 12:04:06','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(4,2,2,8,'2026秋','旁听项目组','演示数据',1,'2026-06-28 12:04:06','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(5,3,3,7,'2026秋','HSE复盘组','演示数据',1,'2026-06-28 12:04:06','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(6,3,3,9,'2026秋','公开加入组','演示数据',1,'2026-06-28 12:04:06','2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_course_student` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_discussion_reply`
--

DROP TABLE IF EXISTS `t_discussion_reply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_discussion_reply`
--

LOCK TABLES `t_discussion_reply` WRITE;
/*!40000 ALTER TABLE `t_discussion_reply` DISABLE KEYS */;
INSERT INTO `t_discussion_reply` VALUES (1,1,3,'优先检查样品气泡和密度计刀口是否清洁，再记录环境温度。',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_discussion_reply` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_discussion_topic`
--

DROP TABLE IF EXISTS `t_discussion_topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_discussion_topic`
--

LOCK TABLES `t_discussion_topic` WRITE;
/*!40000 ALTER TABLE `t_discussion_topic` DISABLE KEYS */;
INSERT INTO `t_discussion_topic` VALUES (1,1,1,5,'密度计读数为什么会波动？','同一杯样品重复读数有小幅变化，是气泡还是温度影响？','OPEN',0,1,1,'2026-07-07 12:04:06','2026-07-08 12:04:06',0),(2,2,3,8,'公共学习用户加入课堂后可以提交报告吗？','已通过邀请码加入，想确认是否能完整参与课堂任务。','OPEN',0,0,0,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_discussion_topic` ENABLE KEYS */;
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
  `knowledge_id` bigint DEFAULT NULL,
  `student_answer` varchar(500) DEFAULT NULL,
  `is_correct` tinyint DEFAULT NULL,
  `correct_flag` tinyint DEFAULT NULL,
  `score` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_exam_answer_record` (`record_id`),
  KEY `idx_exam_answer_question` (`question_id`),
  KEY `idx_exam_answer_knowledge` (`knowledge_id`),
  KEY `idx_exam_answer_record_question` (`record_id`,`question_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_exam_answer`
--

LOCK TABLES `t_exam_answer` WRITE;
/*!40000 ALTER TABLE `t_exam_answer` DISABLE KEYS */;
INSERT INTO `t_exam_answer` VALUES (1,1,1,NULL,'A',1,1,50),(2,1,2,NULL,'A,B,D',1,1,40),(3,2,1,NULL,NULL,0,0,0),(4,2,2,NULL,NULL,0,0,0),(5,4,1,NULL,'',NULL,NULL,0),(6,4,2,NULL,'',NULL,NULL,0),(7,4,3,NULL,'',NULL,NULL,0),(8,5,1,NULL,'A',NULL,NULL,0),(9,5,2,NULL,'A',NULL,NULL,0),(10,5,3,NULL,'TRUE',NULL,NULL,0),(11,6,1,NULL,'A',1,1,50),(12,6,2,NULL,'B',0,0,0),(13,6,3,NULL,'FALSE',1,1,20),(14,7,3,NULL,'TRUE',0,0,0),(15,7,2,NULL,'A,B,D',1,1,30),(16,7,1,NULL,'A',1,1,20);
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
  `course_id` bigint DEFAULT NULL,
  `experiment_id` bigint DEFAULT NULL,
  `total_score` int DEFAULT '100',
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_exam_paper`
--

LOCK TABLES `t_exam_paper` WRITE;
/*!40000 ALTER TABLE `t_exam_paper` DISABLE KEYS */;
INSERT INTO `t_exam_paper` (`id`, `title`, `description`, `course_id`, `experiment_id`, `total_score`, `pass_score`, `duration`, `attempt_limit`, `show_answer_after_submit`, `admission_validity_days`, `multiple_score_policy`, `random_enabled`, `random_count`, `teacher_id`, `status`, `start_time`, `end_time`, `create_time`, `update_time`, `is_deleted`) VALUES (1,'钻井液实验安全准入考试','完成后可进入实验预约。',1,1,100,70,30,3,1,90,'ALL_OR_NOTHING',0,0,3,'PUBLISHED','2026-07-03 12:04:06','2026-10-06 12:04:06','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,'1234','222333',1,2,60,60,30,1,1,180,'ALL_OR_NOTHING',0,0,3,'PUBLISHED',NULL,NULL,'2026-07-10 16:17:44','2026-07-10 16:22:52',1);
/*!40000 ALTER TABLE `t_exam_paper` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_exam_paper_question`
--

DROP TABLE IF EXISTS `t_exam_paper_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_exam_paper_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `score` int NOT NULL,
  `order_num` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_question` (`paper_id`,`question_id`),
  KEY `idx_paper_question_paper` (`paper_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_exam_paper_question`
--

LOCK TABLES `t_exam_paper_question` WRITE;
/*!40000 ALTER TABLE `t_exam_paper_question` DISABLE KEYS */;
INSERT INTO `t_exam_paper_question` VALUES (1,1,1,50,1),(2,1,2,50,2),(3,1,3,20,3),(5,2,3,20,1),(6,2,2,30,2),(7,2,1,20,3);
/*!40000 ALTER TABLE `t_exam_paper_question` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_exam_record`
--

LOCK TABLES `t_exam_record` WRITE;
/*!40000 ALTER TABLE `t_exam_record` DISABLE KEYS */;
INSERT INTO `t_exam_record` VALUES (1,5,1,1,90,90,0,'GRADED','[{\"id\": 1, \"type\": \"SINGLE\", \"content\": \"钻井液密度测试时首先应确认什么？\", \"answer\": \"A\", \"score\": 50}, {\"id\": 2, \"type\": \"MULTIPLE\", \"content\": \"井控风险识别应关注哪些信号？\", \"answer\": \"A,B,D\", \"score\": 50}]',0,NULL,1,'2026-07-06 11:04:06','2026-07-06 12:04:06','2026-07-06 11:04:06','2026-07-06 12:04:06','2026-07-06 12:04:06','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,5,1,1,0,0,NULL,'EXPIRED','[{\"id\":1,\"type\":\"SINGLE\",\"content\":\"钻井液密度测试时首先应确认什么？\",\"options\":\"[{\\\"key\\\": \\\"A\\\", \\\"label\\\": \\\"密度计已校准\\\"}, {\\\"key\\\": \\\"B\\\", \\\"label\\\": \\\"随意取样\\\"}]\",\"answer\":\"A\",\"analysis\":\"密度计校准是读数可靠的前提。\",\"knowledgePoint\":\"密度计校准\",\"knowledgeId\":null,\"riskType\":\"飞溅\",\"difficulty\":\"EASY\",\"relatedResourceId\":1,\"score\":50,\"orderNum\":1},{\"id\":2,\"type\":\"MULTIPLE\",\"content\":\"井控风险识别应关注哪些信号？\",\"options\":\"[{\\\"key\\\": \\\"A\\\", \\\"label\\\": \\\"立压变化\\\"}, {\\\"key\\\": \\\"B\\\", \\\"label\\\": \\\"返出流量异常\\\"}, {\\\"key\\\": \\\"D\\\", \\\"label\\\": \\\"泥浆池液面变化\\\"}]\",\"answer\":\"A,B,D\",\"analysis\":\"压力、流量和液面变化是关键判断依据。\",\"knowledgePoint\":\"井控异常信号\",\"knowledgeId\":null,\"riskType\":\"高压\",\"difficulty\":\"MEDIUM\",\"relatedResourceId\":2,\"score\":50,\"orderNum\":2}]',1,NULL,0,'2026-07-09 10:32:51','2026-07-10 11:50:19','2026-07-09 11:02:51',NULL,'2026-07-10 11:50:19','2026-07-09 10:32:50','2026-07-09 10:32:50',0),(6,5,1,1,70,70,NULL,'GRADED','[{\"id\":1,\"type\":\"SINGLE\",\"content\":\"钻井液密度测试时首先应确认什么？\",\"options\":\"[{\\\"key\\\": \\\"A\\\", \\\"label\\\": \\\"密度计已校准\\\"}, {\\\"key\\\": \\\"B\\\", \\\"label\\\": \\\"随意取样\\\"}]\",\"answer\":\"A\",\"analysis\":\"密度计校准是读数可靠的前提。\",\"knowledgePoint\":\"密度计校准\",\"knowledgeId\":null,\"riskType\":\"飞溅\",\"difficulty\":\"EASY\",\"relatedResourceId\":1,\"score\":50,\"orderNum\":1},{\"id\":2,\"type\":\"MULTIPLE\",\"content\":\"井控风险识别应关注哪些信号？\",\"options\":\"[{\\\"key\\\": \\\"A\\\", \\\"label\\\": \\\"立压变化\\\"}, {\\\"key\\\": \\\"B\\\", \\\"label\\\": \\\"返出流量异常\\\"}, {\\\"key\\\": \\\"D\\\", \\\"label\\\": \\\"泥浆池液面变化\\\"}]\",\"answer\":\"A,B,D\",\"analysis\":\"压力、流量和液面变化是关键判断依据。\",\"knowledgePoint\":\"井控异常信号\",\"knowledgeId\":null,\"riskType\":\"高压\",\"difficulty\":\"MEDIUM\",\"relatedResourceId\":2,\"score\":50,\"orderNum\":2},{\"id\":3,\"type\":\"JUDGE\",\"content\":\"未完成安全准入考试也可以直接预约高风险实验。\",\"options\":\"[{\\\"key\\\": \\\"TRUE\\\", \\\"label\\\": \\\"正确\\\"}, {\\\"key\\\": \\\"FALSE\\\", \\\"label\\\": \\\"错误\\\"}]\",\"answer\":\"FALSE\",\"analysis\":\"高风险实验必须先完成安全准入考试并达到及格线。\",\"knowledgePoint\":\"安全准入\",\"knowledgeId\":null,\"riskType\":\"准入\",\"difficulty\":\"EASY\",\"relatedResourceId\":1,\"score\":20,\"orderNum\":3}]',0,2,1,'2026-07-10 16:12:51','2026-07-10 16:13:14','2026-07-10 16:42:51','2026-07-10 16:13:02','2026-07-10 16:13:14','2026-07-10 16:12:50','2026-07-10 16:12:50',0),(7,5,2,2,50,50,NULL,'GRADED','[{\"id\":3,\"type\":\"JUDGE\",\"content\":\"未完成安全准入考试也可以直接预约高风险实验。\",\"options\":\"[{\\\"key\\\": \\\"TRUE\\\", \\\"label\\\": \\\"正确\\\"}, {\\\"key\\\": \\\"FALSE\\\", \\\"label\\\": \\\"错误\\\"}]\",\"answer\":\"FALSE\",\"analysis\":\"高风险实验必须先完成安全准入考试并达到及格线。\",\"knowledgePoint\":\"安全准入\",\"knowledgeId\":null,\"riskType\":\"准入\",\"difficulty\":\"EASY\",\"relatedResourceId\":1,\"score\":20,\"orderNum\":1},{\"id\":2,\"type\":\"MULTIPLE\",\"content\":\"井控风险识别应关注哪些信号？\",\"options\":\"[{\\\"key\\\": \\\"A\\\", \\\"label\\\": \\\"立压变化\\\"}, {\\\"key\\\": \\\"B\\\", \\\"label\\\": \\\"返出流量异常\\\"}, {\\\"key\\\": \\\"D\\\", \\\"label\\\": \\\"泥浆池液面变化\\\"}]\",\"answer\":\"A,B,D\",\"analysis\":\"压力、流量和液面变化是关键判断依据。\",\"knowledgePoint\":\"井控异常信号\",\"knowledgeId\":null,\"riskType\":\"高压\",\"difficulty\":\"MEDIUM\",\"relatedResourceId\":2,\"score\":30,\"orderNum\":2},{\"id\":1,\"type\":\"SINGLE\",\"content\":\"钻井液密度测试时首先应确认什么？\",\"options\":\"[{\\\"key\\\": \\\"A\\\", \\\"label\\\": \\\"密度计已校准\\\"}, {\\\"key\\\": \\\"B\\\", \\\"label\\\": \\\"随意取样\\\"}]\",\"answer\":\"A\",\"analysis\":\"密度计校准是读数可靠的前提。\",\"knowledgePoint\":\"密度计校准\",\"knowledgeId\":null,\"riskType\":\"飞溅\",\"difficulty\":\"EASY\",\"relatedResourceId\":1,\"score\":20,\"orderNum\":3}]',0,NULL,0,'2026-07-10 16:20:10','2026-07-10 16:20:27','2026-07-10 16:50:10',NULL,'2026-07-10 16:20:27','2026-07-10 16:20:09','2026-07-10 16:20:09',0);
/*!40000 ALTER TABLE `t_exam_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_experiment`
--

DROP TABLE IF EXISTS `t_experiment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_experiment`
--

LOCK TABLES `t_experiment` WRITE;
/*!40000 ALTER TABLE `t_experiment` DISABLE KEYS */;
INSERT INTO `t_experiment` VALUES (1,1,'MUD-DENSITY','油气工程','/src/assets/amazing/procedure-safety.png','钻井液密度与流变性能测试工程情境导入。','mud_density','钻井液密度、黏度与滤失量测试。','钻井液密度与流变性能测试','理解工程参数、设备操作与安全风险之间的关系。','通过实验数据和流程节点建立工程判断。','实验台、传感器、记录表、个人防护用品','模拟样品与案例数据','油气工程实验楼 101','钻井液实验1班','MEDIUM','高压、旋转设备、样品飞溅或误操作','高压,机械,飞溅,HSE','实验服、护目镜、手套、防滑鞋','基础实验安全、设备认知、风险识别','完成准入学习与考试后操作。',1,90,70,'完整记录实验数据和异常现象。','设备异常立即停机并报告教师。','按应急预案撤离或处置。',NULL,'数据完整30，分析40，安全复盘30',1,1,1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,1,'WELL-CONTROL','油气工程','/src/assets/amazing/procedure-safety.png','井控风险识别与关井流程演示工程情境导入。','well_control','溢流信号识别与关井流程模拟。','井控风险识别与关井流程演示','理解工程参数、设备操作与安全风险之间的关系。','通过实验数据和流程节点建立工程判断。','实验台、传感器、记录表、个人防护用品','模拟样品与案例数据','油气工程实验楼 201','钻井液实验1班','HIGH','高压、旋转设备、样品飞溅或误操作','高压,机械,飞溅,HSE','实验服、护目镜、手套、防滑鞋','基础实验安全、设备认知、风险识别','完成准入学习与考试后操作。',1,90,70,'完整记录实验数据和异常现象。','设备异常立即停机并报告教师。','按应急预案撤离或处置。',NULL,'数据完整30，分析40，安全复盘30',1,1,2,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(3,2,'PIPE-DROP','油气工程','/src/assets/amazing/procedure-safety.png','管输流量与压降关系实验工程情境导入。','pipe_drop','测量不同流量下管路压降。','管输流量与压降关系实验','理解工程参数、设备操作与安全风险之间的关系。','通过实验数据和流程节点建立工程判断。','实验台、传感器、记录表、个人防护用品','模拟样品与案例数据','油气工程实验楼 301','2026秋课堂','MEDIUM','高压、旋转设备、样品飞溅或误操作','高压,机械,飞溅,HSE','实验服、护目镜、手套、防滑鞋','基础实验安全、设备认知、风险识别','完成准入学习与考试后操作。',1,90,70,'完整记录实验数据和异常现象。','设备异常立即停机并报告教师。','按应急预案撤离或处置。',NULL,'数据完整30，分析40，安全复盘30',1,1,3,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(4,3,'HSE-CASE','油气工程','/src/assets/amazing/procedure-safety.png','油气实验室事故案例复盘工程情境导入。','hse_case','事故链分析与应急卡片制作。','油气实验室事故案例复盘','理解工程参数、设备操作与安全风险之间的关系。','通过实验数据和流程节点建立工程判断。','实验台、传感器、记录表、个人防护用品','模拟样品与案例数据','油气工程实验楼 401','2026秋课堂','LOW','高压、旋转设备、样品飞溅或误操作','高压,机械,飞溅,HSE','实验服、护目镜、手套、防滑鞋','基础实验安全、设备认知、风险识别','完成准入学习与考试后操作。',0,90,70,'完整记录实验数据和异常现象。','设备异常立即停机并报告教师。','按应急预案撤离或处置。',NULL,'数据完整30，分析40，安全复盘30',0,1,4,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_experiment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_experiment_admission`
--

DROP TABLE IF EXISTS `t_experiment_admission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_experiment_admission`
--

LOCK TABLES `t_experiment_admission` WRITE;
/*!40000 ALTER TABLE `t_experiment_admission` DISABLE KEYS */;
INSERT INTO `t_experiment_admission` VALUES (1,5,1,1,1,'REPLACED','2026-07-06 12:04:06','2026-10-04 12:04:06',NULL,NULL,NULL,'2026-07-08 12:04:06','2026-07-10 16:13:14',0),(2,5,1,1,6,'VALID','2026-07-10 16:13:14','2026-10-08 16:13:14',NULL,NULL,NULL,'2026-07-10 16:13:14','2026-07-10 16:13:14',0);
/*!40000 ALTER TABLE `t_experiment_admission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_experiment_step`
--

DROP TABLE IF EXISTS `t_experiment_step`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_experiment_step`
--

LOCK TABLES `t_experiment_step` WRITE;
/*!40000 ALTER TABLE `t_experiment_step` DISABLE KEYS */;
INSERT INTO `t_experiment_step` VALUES (1,1,1,'工程情境导入','进入钻井液密度与流变性能测试的现场问题。','先读风险提示，再进入操作。','VIDEO','/demo/mud_density.mp4',NULL,1,12,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,1,2,'设备与风险识别','识别关键设备、危险源和个人防护要求。','未经教师确认不得启动设备。','IMAGE','/demo/mud_density.png',NULL,1,15,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(3,2,1,'工程情境导入','进入井控风险识别与关井流程演示的现场问题。','先读风险提示，再进入操作。','VIDEO','/demo/well_control.mp4',NULL,1,12,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(4,2,2,'设备与风险识别','识别关键设备、危险源和个人防护要求。','未经教师确认不得启动设备。','IMAGE','/demo/well_control.png',NULL,1,15,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(5,3,1,'工程情境导入','进入管输流量与压降关系实验的现场问题。','先读风险提示，再进入操作。','VIDEO','/demo/pipe_drop.mp4',NULL,1,12,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(6,3,2,'设备与风险识别','识别关键设备、危险源和个人防护要求。','未经教师确认不得启动设备。','IMAGE','/demo/pipe_drop.png',NULL,1,15,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(7,4,1,'工程情境导入','进入油气实验室事故案例复盘的现场问题。','先读风险提示，再进入操作。','VIDEO','/demo/hse_case.mp4',NULL,1,12,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(8,4,2,'设备与风险识别','识别关键设备、危险源和个人防护要求。','未经教师确认不得启动设备。','IMAGE','/demo/hse_case.png',NULL,1,15,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_experiment_step` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_lab_course`
--

DROP TABLE IF EXISTS `t_lab_course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_lab_course`
--

LOCK TABLES `t_lab_course` WRITE;
/*!40000 ALTER TABLE `t_lab_course` DISABLE KEYS */;
INSERT INTO `t_lab_course` VALUES (1,'OG-LAB-101','钻井液性能测试实验课堂','石油工程',3,'2026秋','钻井液性能测试实验课堂演示课堂，覆盖资源预习、风险认知、准入考核、预约、报告和答疑。','/src/assets/amazing/lab-hero.png','让油气实验知识看得懂、能操作、可考核。','钻井液,井控,安全准入','mud',1,1,1.5,24,'资源预习30% + 安全准入30% + 报告40%','完成预习资源、风险识别与准入任务后进入实验预约。',1,NULL,'2026-06-09 12:04:06','2026-07-08 12:04:06',0),(2,'OG-LAB-202','油气集输与管输压降实验课堂','油气储运',3,'2026秋','油气集输与管输压降实验课堂演示课堂，覆盖资源预习、风险认知、准入考核、预约、报告和答疑。','/src/assets/amazing/lab-hero.png','让油气实验知识看得懂、能操作、可考核。','管输,压降,阀门','pipe',1,2,1.5,24,'资源预习30% + 安全准入30% + 报告40%','完成预习资源、风险识别与准入任务后进入实验预约。',1,NULL,'2026-06-10 12:04:06','2026-07-08 12:04:06',0),(3,'OG-LAB-303','HSE风险识别与应急处置课堂','安全工程',4,'2026秋','HSE风险识别与应急处置课堂演示课堂，覆盖资源预习、风险认知、准入考核、预约、报告和答疑。','/src/assets/amazing/lab-hero.png','让油气实验知识看得懂、能操作、可考核。','HSE,应急,风险识别','hse',1,3,1.5,24,'资源预习30% + 安全准入30% + 报告40%','完成预习资源、风险识别与准入任务后进入实验预约。',1,NULL,'2026-06-11 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_lab_course` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_lab_time_slot`
--

DROP TABLE IF EXISTS `t_lab_time_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_lab_time_slot`
--

LOCK TABLES `t_lab_time_slot` WRITE;
/*!40000 ALTER TABLE `t_lab_time_slot` DISABLE KEYS */;
INSERT INTO `t_lab_time_slot` (`id`, `lab_id`, `experiment_id`, `date`, `start_time`, `end_time`, `capacity`, `booked_count`, `status`, `create_by`, `create_time`, `update_time`) VALUES (1,101,1,'2026-07-11','09:00:00','11:00:00',24,1,'AVAILABLE',2,'2026-07-08 12:04:06','2026-07-08 12:04:06'),(2,102,3,'2026-07-12','14:00:00','16:00:00',20,1,'AVAILABLE',2,'2026-07-08 12:04:06','2026-07-08 12:04:06');
/*!40000 ALTER TABLE `t_lab_time_slot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_learning_record`
--

DROP TABLE IF EXISTS `t_learning_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_learning_record`
--

LOCK TABLES `t_learning_record` WRITE;
/*!40000 ALTER TABLE `t_learning_record` DISABLE KEYS */;
INSERT INTO `t_learning_record` VALUES (1,5,1,1,96.00,620,600,'密度计读数前要排气泡。',1,'2026-07-05 12:04:06','2026-07-09 11:21:58','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,6,3,3,55.00,260,120,'压降曲线还需复习。',0,'2026-07-06 12:04:06','2026-07-08 12:04:06','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(3,7,1,1,0.00,0,0,NULL,0,'2026-07-08 16:13:46','2026-07-08 16:13:46','2026-07-08 16:13:46','2026-07-08 16:13:46',0),(4,5,2,2,100.00,60,0,'',0,'2026-07-08 17:15:40','2026-07-09 11:22:00','2026-07-08 17:15:40','2026-07-08 17:15:40',0),(5,3,1,1,0.00,0,0,NULL,0,'2026-07-08 17:50:11','2026-07-09 00:47:40','2026-07-08 17:50:11','2026-07-08 17:50:11',0),(6,5,5,1,0.00,0,0,NULL,0,'2026-07-09 09:38:20','2026-07-09 11:22:02','2026-07-09 09:38:20','2026-07-09 09:38:20',0),(7,3,4,4,0.00,0,0,NULL,0,'2026-07-09 18:28:48','2026-07-10 16:27:33','2026-07-09 18:28:48','2026-07-09 18:28:48',0),(8,3,5,1,0.00,0,0,NULL,0,'2026-07-09 18:28:50','2026-07-09 18:28:50','2026-07-09 18:28:50','2026-07-09 18:28:50',0);
/*!40000 ALTER TABLE `t_learning_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_learning_task`
--

DROP TABLE IF EXISTS `t_learning_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_learning_task`
--

LOCK TABLES `t_learning_task` WRITE;
/*!40000 ALTER TABLE `t_learning_task` DISABLE KEYS */;
INSERT INTO `t_learning_task` VALUES (1,1,1,'观看钻井液预习视频','RESOURCE',1,NULL,NULL,1,1,'2026-07-03 12:04:06','2026-07-28 12:04:06','AUTO',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,1,1,'提交钻井液实验报告','REPORT',NULL,NULL,NULL,1,2,'2026-07-03 12:04:06','2026-07-28 12:04:06','AUTO',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(3,2,3,'阅读管输压降指导书','RESOURCE',3,NULL,NULL,1,1,'2026-07-03 12:04:06','2026-07-28 12:04:06','AUTO',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(4,3,4,'学习HSE事故案例','RESOURCE',4,NULL,NULL,1,1,'2026-07-03 12:04:06','2026-07-28 12:04:06','AUTO',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_learning_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_learning_task_record`
--

DROP TABLE IF EXISTS `t_learning_task_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_learning_task_record`
--

LOCK TABLES `t_learning_task_record` WRITE;
/*!40000 ALTER TABLE `t_learning_task_record` DISABLE KEYS */;
INSERT INTO `t_learning_task_record` VALUES (1,1,5,'COMPLETED','2026-07-05 12:04:06','2026-07-10 16:20:29','RESOURCE','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,3,6,'IN_PROGRESS','2026-07-05 12:04:06',NULL,'DEMO','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(3,4,7,'COMPLETED','2026-07-05 12:04:06','2026-07-07 12:04:06','DEMO','2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_learning_task_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_operation_log`
--

DROP TABLE IF EXISTS `t_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=147 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_operation_log`
--

LOCK TABLES `t_operation_log` WRITE;
/*!40000 ALTER TABLE `t_operation_log` DISABLE KEYS */;
INSERT INTO `t_operation_log` VALUES (1,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-08 12:10:08'),(2,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 12:10:44'),(3,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-08 12:11:04'),(4,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 12:12:07'),(5,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-08 12:12:30'),(6,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 12:13:03'),(7,6,'student_li','用户中心','登录','登录成功','SUCCESS','2026-07-08 12:13:17'),(8,6,'student_li','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 16:12:50'),(9,7,'student_chen','用户中心','登录','登录成功','SUCCESS','2026-07-08 16:13:41'),(10,7,'student_chen','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 16:14:39'),(11,7,'student_chen','用户中心','登录','登录成功','SUCCESS','2026-07-08 16:15:11'),(12,7,'student_chen','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 16:30:23'),(13,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-08 16:31:20'),(14,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 16:31:33'),(15,2,'lab_admin','用户中心','登录','登录成功','SUCCESS','2026-07-08 16:32:02'),(16,2,'lab_admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 16:32:14'),(17,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-08 16:32:30'),(18,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 16:32:55'),(19,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-08 16:33:15'),(20,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 16:34:09'),(21,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-08 16:34:42'),(22,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 17:08:00'),(23,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-08 17:08:11'),(24,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 17:08:20'),(25,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-08 17:08:32'),(26,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 17:11:22'),(27,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-08 17:11:30'),(28,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 17:15:21'),(29,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-08 17:15:32'),(30,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 17:16:12'),(31,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-08 17:16:24'),(32,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 17:16:31'),(33,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-08 17:16:38'),(34,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 17:23:51'),(35,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-08 17:23:59'),(36,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 17:24:33'),(37,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-08 17:24:45'),(38,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 17:49:26'),(39,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-08 17:49:36'),(40,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-08 17:49:59'),(41,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-08 17:50:08'),(42,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-09 00:47:13'),(43,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 00:47:27'),(44,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 00:47:34'),(45,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 00:48:04'),(46,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 00:48:31'),(47,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-09 09:36:14'),(48,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 09:36:32'),(49,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 09:36:37'),(50,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 09:37:11'),(51,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 09:37:25'),(52,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 10:31:48'),(53,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 10:56:20'),(54,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 10:56:33'),(55,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 11:04:56'),(56,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 11:05:09'),(57,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 11:07:17'),(58,4,'teacher_li','用户中心','登录','登录成功','SUCCESS','2026-07-09 11:07:38'),(59,4,'teacher_li','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 11:10:48'),(60,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 11:11:05'),(61,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 11:18:27'),(62,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 11:18:38'),(63,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 11:43:12'),(64,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 11:43:30'),(65,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 12:05:27'),(66,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 12:05:37'),(67,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 12:09:07'),(68,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 12:09:22'),(69,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 12:13:30'),(70,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 12:13:42'),(71,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 12:14:07'),(72,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 12:14:17'),(73,3,'teacher_wang','COURSE_ORGANIZATION','CREATE','创建课程：111111111','SUCCESS','2026-07-09 12:22:32'),(74,3,'teacher_wang','COURSE_ORGANIZATION','DELETE','删除课程：111111111','SUCCESS','2026-07-09 12:23:04'),(75,3,'teacher_wang','EXPERIMENT_PROCEDURE','STATUS','实验状态更新：钻井液密度与流变性能测试 -> 2','SUCCESS','2026-07-09 12:29:54'),(76,3,'teacher_wang','EXPERIMENT_PROCEDURE','STATUS','实验状态更新：钻井液密度与流变性能测试 -> 1','SUCCESS','2026-07-09 12:29:57'),(77,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 12:38:21'),(78,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 12:38:33'),(79,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 12:39:17'),(80,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 12:39:27'),(81,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 12:40:56'),(82,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 12:41:10'),(83,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 12:41:28'),(84,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 12:41:41'),(85,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 12:45:56'),(86,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 12:46:08'),(87,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 12:47:39'),(88,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 12:47:50'),(89,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 12:48:02'),(90,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 12:48:48'),(91,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 16:12:44'),(92,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 16:13:05'),(93,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 16:14:06'),(94,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 16:14:29'),(95,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 16:21:49'),(96,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 16:24:17'),(97,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 16:24:31'),(98,3,'teacher_wang','EXPERIMENT_PROCEDURE','UPDATE','修改实验项目：钻井液密度与流变性能测试','SUCCESS','2026-07-09 17:35:41'),(99,3,'teacher_wang','EXPERIMENT_PROCEDURE','UPDATE','修改实验项目：钻井液密度与流变性能测试','SUCCESS','2026-07-09 17:54:10'),(100,3,'teacher_wang','EXPERIMENT_PROCEDURE','UPDATE','修改实验项目：井控风险识别与关井流程演示','SUCCESS','2026-07-09 17:54:21'),(101,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 18:11:14'),(102,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 18:11:27'),(103,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 18:12:18'),(104,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 18:12:30'),(105,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 18:29:20'),(106,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-09 18:29:35'),(107,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 18:36:07'),(108,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-09 18:36:17'),(109,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 18:37:15'),(110,4,'teacher_li','用户中心','登录','登录成功','SUCCESS','2026-07-09 18:37:26'),(111,4,'teacher_li','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 18:51:39'),(112,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-09 19:00:23'),(113,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 19:01:15'),(114,8,'user_oilfan','用户中心','登录','登录成功','SUCCESS','2026-07-09 19:01:22'),(115,8,'user_oilfan','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 19:01:46'),(116,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-09 19:01:57'),(117,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 19:20:21'),(118,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-09 19:20:30'),(119,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 19:20:41'),(120,9,'user_guest','用户中心','登录','登录成功','SUCCESS','2026-07-09 19:20:47'),(121,9,'user_guest','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-09 19:24:23'),(122,1,'admin','用户中心','登录','登录成功','SUCCESS','2026-07-09 19:24:32'),(123,1,'admin','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-10 09:41:33'),(124,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-10 09:41:50'),(125,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-10 11:49:20'),(126,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-10 11:49:41'),(127,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-10 11:51:25'),(128,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-10 11:51:38'),(129,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-10 11:52:36'),(130,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-10 11:52:51'),(131,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-10 11:57:00'),(132,6,'student_li','用户中心','登录','登录成功','SUCCESS','2026-07-10 11:57:16'),(133,6,'student_li','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-10 11:57:33'),(134,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-10 11:57:45'),(135,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-10 12:08:04'),(136,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-10 12:08:24'),(137,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-10 12:20:14'),(138,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-10 12:20:48'),(139,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-10 12:27:31'),(140,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-10 12:27:51'),(141,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-10 16:13:39'),(142,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-10 16:13:51'),(143,3,'teacher_wang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-10 16:19:48'),(144,5,'student_zhang','用户中心','登录','登录成功','SUCCESS','2026-07-10 16:20:03'),(145,5,'student_zhang','用户中心','退出登录','用户主动退出','SUCCESS','2026-07-10 16:22:08'),(146,3,'teacher_wang','用户中心','登录','登录成功','SUCCESS','2026-07-10 16:22:20');
/*!40000 ALTER TABLE `t_operation_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_permission`
--

DROP TABLE IF EXISTS `t_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_permission`
--

LOCK TABLES `t_permission` WRITE;
/*!40000 ALTER TABLE `t_permission` DISABLE KEYS */;
INSERT INTO `t_permission` VALUES (1,'门户访问','portal:view',2,0,NULL,NULL,10,'2026-07-08 12:04:06'),(2,'门户搜索','portal:search',2,0,NULL,NULL,20,'2026-07-08 12:04:06'),(3,'消息日程','portal:message',2,0,NULL,NULL,30,'2026-07-08 12:04:06'),(4,'公告管理','portal:notice:manage',2,0,NULL,NULL,40,'2026-07-08 12:04:06'),(5,'个人资料','profile:update',2,0,NULL,NULL,50,'2026-07-08 12:04:06'),(6,'修改密码','profile:password',2,0,NULL,NULL,60,'2026-07-08 12:04:06'),(7,'仪表盘','dashboard:view',2,0,NULL,NULL,70,'2026-07-08 12:04:06'),(8,'AI问答','ai:ask',2,0,NULL,NULL,80,'2026-07-08 12:04:06'),(9,'课程查看','course:view',2,0,NULL,NULL,90,'2026-07-08 12:04:06'),(10,'课程创建','course:create',2,0,NULL,NULL,100,'2026-07-08 12:04:06'),(11,'课程更新','course:update',2,0,NULL,NULL,110,'2026-07-08 12:04:06'),(12,'课程删除','course:delete',2,0,NULL,NULL,120,'2026-07-08 12:04:06'),(13,'课程发布','course:publish',2,0,NULL,NULL,130,'2026-07-08 12:04:06'),(14,'课程归档','course:archive',2,0,NULL,NULL,140,'2026-07-08 12:04:06'),(15,'课程复制','course:copy',2,0,NULL,NULL,150,'2026-07-08 12:04:06'),(16,'教学班管理','course:class:manage',2,0,NULL,NULL,160,'2026-07-08 12:04:06'),(17,'课堂成员管理','course:student:manage',2,0,NULL,NULL,170,'2026-07-08 12:04:06'),(18,'课堂加入','course:join',2,0,NULL,NULL,180,'2026-07-08 12:04:06'),(19,'课堂邀请码管理','course:invite:manage',2,0,NULL,NULL,190,'2026-07-08 12:04:06'),(20,'实验查看','experiment:view',2,0,NULL,NULL,200,'2026-07-08 12:04:06'),(21,'实验创建','experiment:create',2,0,NULL,NULL,210,'2026-07-08 12:04:06'),(22,'实验更新','experiment:update',2,0,NULL,NULL,220,'2026-07-08 12:04:06'),(23,'实验删除','experiment:delete',2,0,NULL,NULL,230,'2026-07-08 12:04:06'),(24,'资源查看','resource:view',2,0,NULL,NULL,240,'2026-07-08 12:04:06'),(25,'资源创建','resource:create',2,0,NULL,NULL,250,'2026-07-08 12:04:06'),(26,'资源更新','resource:update',2,0,NULL,NULL,260,'2026-07-08 12:04:06'),(27,'资源删除','resource:delete',2,0,NULL,NULL,270,'2026-07-08 12:04:06'),(28,'资源投稿','resource-submission:create',2,0,NULL,NULL,280,'2026-07-08 12:04:06'),(29,'资源投稿审核','resource-submission:review',2,0,NULL,NULL,290,'2026-07-08 12:04:06'),(30,'学习记录','learning:update:self',2,0,NULL,NULL,300,'2026-07-08 12:04:06'),(31,'考试参加','exam:take',2,0,NULL,NULL,310,'2026-07-08 12:04:06'),(32,'试卷查看','exam-paper:view',2,0,NULL,NULL,320,'2026-07-08 12:04:06'),(33,'试卷创建','exam-paper:create',2,0,NULL,NULL,330,'2026-07-08 12:04:06'),(34,'试卷更新','exam-paper:update',2,0,NULL,NULL,340,'2026-07-08 12:04:06'),(35,'试卷删除','exam-paper:delete',2,0,NULL,NULL,350,'2026-07-08 12:04:06'),(36,'主观题评分','exam:grade',2,0,NULL,NULL,360,'2026-07-08 12:04:06'),(37,'预约查看','reservation:view',2,0,NULL,NULL,370,'2026-07-08 12:04:06'),(38,'预约管理','reservation:manage',2,0,NULL,NULL,380,'2026-07-08 12:04:06'),(39,'预约审核','reservation:review',2,0,NULL,NULL,390,'2026-07-08 12:04:06'),(40,'报告查看','report:view',2,0,NULL,NULL,400,'2026-07-08 12:04:06'),(41,'报告批改','report:review',2,0,NULL,NULL,410,'2026-07-08 12:04:06'),(42,'报告评分','report:grade',2,0,NULL,NULL,420,'2026-07-08 12:04:06'),(43,'用户查看','user:view',2,0,NULL,NULL,430,'2026-07-08 12:04:06'),(44,'用户创建','user:create',2,0,NULL,NULL,440,'2026-07-08 12:04:06'),(45,'用户更新','user:update',2,0,NULL,NULL,450,'2026-07-08 12:04:06'),(46,'用户删除','user:delete',2,0,NULL,NULL,460,'2026-07-08 12:04:06'),(47,'角色查看','role:view',2,0,NULL,NULL,470,'2026-07-08 12:04:06'),(48,'角色权限','role:permission:update',2,0,NULL,NULL,480,'2026-07-08 12:04:06'),(49,'权限查看','permission:view',2,0,NULL,NULL,490,'2026-07-08 12:04:06'),(50,'操作日志','operation-log:view',2,0,NULL,NULL,500,'2026-07-08 12:04:06'),(55,'教师认证申请','teacher-certification:apply',2,0,NULL,NULL,550,'2026-07-08 12:04:06'),(56,'教师认证审核','teacher-certification:review',2,0,NULL,NULL,560,'2026-07-08 12:04:06'),(57,'报告提交','report:submit',2,0,NULL,NULL,140,'2026-07-08 17:45:55'),(58,'题库查看','question:view',2,0,NULL,NULL,401,'2026-07-10 10:25:57'),(59,'题库创建','question:create',2,0,NULL,NULL,403,'2026-07-10 10:25:57'),(60,'题库更新','question:update',2,0,NULL,NULL,404,'2026-07-10 10:25:57'),(61,'题库删除','question:delete',2,0,NULL,NULL,405,'2026-07-10 10:25:57'),(62,'考试创建','exam:create',2,0,NULL,NULL,413,'2026-07-10 10:25:57'),(63,'考试更新','exam:update',2,0,NULL,NULL,414,'2026-07-10 10:25:57'),(64,'考试删除','exam:delete',2,0,NULL,NULL,415,'2026-07-10 10:25:57'),(65,'考试统计','exam:statistics',2,0,NULL,NULL,422,'2026-07-10 10:25:57');
/*!40000 ALTER TABLE `t_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_portal_message`
--

DROP TABLE IF EXISTS `t_portal_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_portal_message`
--

LOCK TABLES `t_portal_message` WRITE;
/*!40000 ALTER TABLE `t_portal_message` DISABLE KEYS */;
INSERT INTO `t_portal_message` VALUES (1,5,'钻井液实验预约已通过','请按预约时间到 A203 实验室。','DEMO',NULL,'/student/reserve',1,'2026-07-09 09:46:57','2026-07-08 12:04:06',0),(2,3,'有新的课堂问题','学生提出密度计读数波动问题。','DEMO',NULL,'/discussions',0,NULL,'2026-07-08 12:04:06',0),(3,1,'有待审核教师认证','请审核普通用户提交的教师认证。','DEMO',NULL,'/admin/teacher-certifications',0,NULL,'2026-07-08 12:04:06',0),(4,9,'教师认证申请已提交','管理员审核后将获得创建课堂权限。','DEMO',NULL,'/profile',0,NULL,'2026-07-08 12:04:06',0),(5,5,'预约即将开始：钻井液密度与流变性能测试','你的实验预约将于 2026-07-11 09:00 开始，请按要求到场。','RESERVATION_START_REMINDER',1,'/classrooms/1/learn?module=reservation',0,NULL,'2026-07-10 10:00:00',0);
/*!40000 ALTER TABLE `t_portal_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_portal_notice`
--

DROP TABLE IF EXISTS `t_portal_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_portal_notice`
--

LOCK TABLES `t_portal_notice` WRITE;
/*!40000 ALTER TABLE `t_portal_notice` DISABLE KEYS */;
INSERT INTO `t_portal_notice` VALUES (1,'平台演示数据已重置','所有演示账号密码均为 123456。','ALL','HIGH',1,'2026-07-08 12:04:06','2026-08-07 12:04:06',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_portal_notice` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_question`
--

LOCK TABLES `t_question` WRITE;
/*!40000 ALTER TABLE `t_question` DISABLE KEYS */;
INSERT INTO `t_question` VALUES (1,'SINGLE','钻井液密度测试时首先应确认什么？','[{\"key\": \"A\", \"label\": \"密度计已校准\"}, {\"key\": \"B\", \"label\": \"随意取样\"}]','A',20,'密度计校准是读数可靠的前提。','密度计校准',NULL,1,'飞溅',1,'EASY',1,3,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,'MULTIPLE','井控风险识别应关注哪些信号？','[{\"key\": \"A\", \"label\": \"立压变化\"}, {\"key\": \"B\", \"label\": \"返出流量异常\"}, {\"key\": \"D\", \"label\": \"泥浆池液面变化\"}]','A,B,D',30,'压力、流量和液面变化是关键判断依据。','井控异常信号',NULL,2,'高压',2,'MEDIUM',1,3,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(3,'JUDGE','未完成安全准入考试也可以直接预约高风险实验。','[{\"key\": \"TRUE\", \"label\": \"正确\"}, {\"key\": \"FALSE\", \"label\": \"错误\"}]','FALSE',20,'高风险实验必须先完成安全准入考试并达到及格线。','安全准入',NULL,1,'准入',1,'EASY',1,3,'2026-07-10 10:28:23','2026-07-10 10:28:23',0);
/*!40000 ALTER TABLE `t_question` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_recent_visit`
--

DROP TABLE IF EXISTS `t_recent_visit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=112 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_recent_visit`
--

LOCK TABLES `t_recent_visit` WRITE;
/*!40000 ALTER TABLE `t_recent_visit` DISABLE KEYS */;
INSERT INTO `t_recent_visit` VALUES (1,5,'我的课堂','/classrooms','UnifiedClassroomList',84,'2026-07-10 16:22:06','2026-07-08 12:04:06','2026-07-10 16:22:06'),(2,8,'资源学习','/resources','UnifiedResourceCenter',2,'2026-07-09 19:01:23','2026-07-08 12:04:06','2026-07-09 19:01:23'),(3,3,'课堂管理','/teacher/courses','TeacherCourseManagement',49,'2026-07-09 16:16:48','2026-07-08 12:04:06','2026-07-09 16:16:48'),(4,1,'教师认证审核','/admin/teacher-certifications','AdminTeacherCertificationReview',9,'2026-07-10 09:39:08','2026-07-08 12:04:06','2026-07-10 09:39:08'),(5,1,'管理员首页','/admin/home','AdminHome',25,'2026-07-10 09:41:32','2026-07-08 12:10:09','2026-07-10 09:41:32'),(6,1,'用户管理','/admin/users','AdminUserManagement',20,'2026-07-10 09:39:48','2026-07-08 12:10:16','2026-07-10 09:39:48'),(7,1,'角色管理','/admin/roles','AdminRoleManagement',12,'2026-07-10 09:39:45','2026-07-08 12:10:30','2026-07-10 09:39:45'),(8,1,'权限管理','/admin/permissions','AdminPermissionManagement',12,'2026-07-10 09:39:03','2026-07-08 12:10:32','2026-07-10 09:39:03'),(9,1,'公告管理','/admin/notices','AdminNoticeManagement',9,'2026-07-10 09:39:07','2026-07-08 12:10:34','2026-07-10 09:39:07'),(10,1,'操作日志','/admin/logs','AdminOperationLog',9,'2026-07-10 09:39:07','2026-07-08 12:10:37','2026-07-10 09:39:07'),(11,1,'资源投稿审核','/admin/resource-submissions','AdminResourceSubmissionReview',6,'2026-07-10 09:39:08','2026-07-08 12:10:39','2026-07-10 09:39:08'),(12,3,'教师课程建设台','/teacher/home','TeacherHome',7,'2026-07-08 16:32:48','2026-07-08 12:11:04','2026-07-08 16:32:48'),(13,3,'资源学习','/resources','UnifiedResourceCenter',58,'2026-07-10 16:27:46','2026-07-08 12:11:14','2026-07-10 16:27:46'),(14,3,'我的课堂','/classrooms','UnifiedClassroomList',131,'2026-07-10 16:27:45','2026-07-08 12:11:17','2026-07-10 16:27:45'),(15,3,'课堂学习路径','/classrooms/1/learn','UnifiedClassroomLearning',9,'2026-07-08 17:50:16','2026-07-08 12:11:24','2026-07-08 17:50:16'),(16,3,'课堂详细','/teacher/courses/1/edit','TeacherCourseEditor',152,'2026-07-10 16:22:22','2026-07-08 12:11:38','2026-07-10 16:22:22'),(17,3,'学习交流','/discussions','DiscussionCenter',9,'2026-07-10 09:48:26','2026-07-08 12:11:46','2026-07-10 09:48:26'),(18,5,'公共资源学习首页','/user/home','UserHome',60,'2026-07-10 16:20:03','2026-07-08 12:12:30','2026-07-10 16:20:03'),(19,5,'资源学习','/resources','UnifiedResourceCenter',54,'2026-07-10 11:56:04','2026-07-08 12:12:34','2026-07-10 11:56:04'),(20,5,'课堂学习路径','/classrooms/1/learn','UnifiedClassroomLearning',54,'2026-07-10 16:20:06','2026-07-08 12:12:52','2026-07-10 16:20:06'),(21,5,'消息与日程','/messages','MessageCenter',8,'2026-07-09 09:46:55','2026-07-08 12:12:57','2026-07-09 09:46:55'),(22,6,'公共资源学习首页','/user/home','UserHome',4,'2026-07-10 11:57:16','2026-07-08 12:13:17','2026-07-10 11:57:16'),(23,6,'资源学习','/resources','UnifiedResourceCenter',3,'2026-07-08 16:12:48','2026-07-08 12:13:19','2026-07-08 16:12:48'),(24,6,'我的课堂','/classrooms','UnifiedClassroomList',4,'2026-07-10 11:57:30','2026-07-08 12:13:22','2026-07-10 11:57:30'),(25,6,'课堂学习路径','/classrooms/2/learn','UnifiedClassroomLearning',2,'2026-07-10 11:57:19','2026-07-08 12:13:24','2026-07-10 11:57:19'),(26,6,'课程讨论','/discussions','DiscussionCenter',2,'2026-07-08 12:13:33','2026-07-08 12:13:27','2026-07-08 12:13:33'),(27,6,'消息与日程','/messages','MessageCenter',2,'2026-07-08 12:13:34','2026-07-08 12:13:28','2026-07-08 12:13:34'),(28,7,'公共资源学习首页','/user/home','UserHome',9,'2026-07-08 16:30:21','2026-07-08 16:13:41','2026-07-08 16:30:21'),(29,7,'资源学习','/resources','UnifiedResourceCenter',8,'2026-07-08 16:15:30','2026-07-08 16:13:43','2026-07-08 16:15:30'),(30,7,'我的课堂','/classrooms','UnifiedClassroomList',6,'2026-07-08 16:15:29','2026-07-08 16:13:56','2026-07-08 16:15:29'),(31,7,'课程讨论','/discussions','DiscussionCenter',2,'2026-07-08 16:14:10','2026-07-08 16:13:58','2026-07-08 16:14:10'),(32,7,'消息与日程','/messages','MessageCenter',1,'2026-07-08 16:14:02','2026-07-08 16:14:02','2026-07-08 16:14:02'),(33,7,'个人中心','/profile','ProfileCenter',1,'2026-07-08 16:14:12','2026-07-08 16:14:12','2026-07-08 16:14:12'),(34,7,'课堂学习路径','/classrooms/1/learn','UnifiedClassroomLearning',1,'2026-07-08 16:15:27','2026-07-08 16:15:27','2026-07-08 16:15:27'),(35,2,'实验室首页','/lab/home','LabAdminHome',3,'2026-07-08 16:32:12','2026-07-08 16:32:02','2026-07-08 16:32:12'),(36,2,'预约审核','/teacher/reservations','TeacherReservationReview',2,'2026-07-08 16:32:10','2026-07-08 16:32:06','2026-07-08 16:32:10'),(37,3,'消息与日程','/messages','MessageCenter',6,'2026-07-08 17:11:19','2026-07-08 16:32:42','2026-07-08 17:11:19'),(38,3,'公共资源学习首页','/user/home','UserHome',58,'2026-07-10 16:27:46','2026-07-08 17:08:32','2026-07-10 16:27:46'),(39,3,'课堂学习路径','/classrooms/2/learn','UnifiedClassroomLearning',2,'2026-07-08 17:20:24','2026-07-08 17:09:09','2026-07-08 17:20:24'),(40,5,'课程讨论','/discussions','DiscussionCenter',15,'2026-07-09 18:12:15','2026-07-08 17:16:00','2026-07-09 18:12:15'),(41,5,'安全考试','/student/exams','StudentExamCenter',11,'2026-07-09 10:07:23','2026-07-09 00:49:04','2026-07-09 10:07:23'),(42,5,'个人中心','/profile?panel=teacher-certification','ProfileCenter',1,'2026-07-09 09:38:56','2026-07-09 09:38:56','2026-07-09 09:38:56'),(43,5,'实验预约','/student/reserve','StudentReservationCenter',1,'2026-07-09 09:46:59','2026-07-09 09:46:59','2026-07-09 09:46:59'),(44,5,'课堂学习路径','/classrooms/1/learn?experimentId=1','UnifiedClassroomLearning',1,'2026-07-09 09:55:35','2026-07-09 09:55:35','2026-07-09 09:55:35'),(45,5,'课程讨论','/discussions?courseId=1&experimentId=1','DiscussionCenter',1,'2026-07-09 09:55:45','2026-07-09 09:55:45','2026-07-09 09:55:45'),(46,5,'课堂学习路径','/classrooms/1/learn?module=tasks','UnifiedClassroomLearning',41,'2026-07-10 12:20:06','2026-07-09 10:31:57','2026-07-10 12:20:06'),(47,5,'课堂学习路径','/classrooms/1/learn?module=ai','UnifiedClassroomLearning',23,'2026-07-10 12:20:11','2026-07-09 10:31:58','2026-07-10 12:20:11'),(48,5,'课堂学习路径','/classrooms/1/learn?module=chapters','UnifiedClassroomLearning',73,'2026-07-10 12:20:07','2026-07-09 10:32:01','2026-07-10 12:20:07'),(49,5,'章节详情','/classrooms/1/chapters/2','UnifiedClassroomChapterLearning',12,'2026-07-09 18:12:11','2026-07-09 10:32:04','2026-07-09 18:12:11'),(50,5,'章节详情','/classrooms/1/chapters/1','UnifiedClassroomChapterLearning',30,'2026-07-09 18:29:42','2026-07-09 10:32:07','2026-07-09 18:29:42'),(51,5,'课堂学习路径','/classrooms/1/learn?module=discussion','UnifiedClassroomLearning',27,'2026-07-10 12:19:57','2026-07-09 10:32:45','2026-07-10 12:19:57'),(52,5,'课堂学习路径','/classrooms/1/learn?module=report','UnifiedClassroomLearning',35,'2026-07-10 16:12:48','2026-07-09 10:32:46','2026-07-10 16:12:48'),(53,5,'课堂学习路径','/classrooms/1/learn?module=exam','UnifiedClassroomLearning',46,'2026-07-10 16:20:08','2026-07-09 10:32:48','2026-07-10 16:20:08'),(54,5,'课堂学习路径','/classrooms/1/learn?module=resources','UnifiedClassroomLearning',18,'2026-07-10 12:19:55','2026-07-09 10:32:53','2026-07-10 12:19:55'),(55,5,'课堂学习路径','/classrooms/1/learn?module=reservation','UnifiedClassroomLearning',19,'2026-07-10 12:19:55','2026-07-09 10:32:55','2026-07-10 12:19:55'),(56,5,'课堂学习路径','/classrooms/1/learn?module=records','UnifiedClassroomLearning',13,'2026-07-10 12:19:53','2026-07-09 10:32:56','2026-07-10 12:19:53'),(57,3,'资源管理','/teacher/resources?courseId=1','TeacherResourceManagement',11,'2026-07-09 17:02:45','2026-07-09 10:57:20','2026-07-09 17:02:45'),(58,3,'课堂详细','/teacher/courses/2/edit','TeacherCourseEditor',5,'2026-07-09 19:20:12','2026-07-09 11:04:15','2026-07-09 19:20:12'),(59,3,'资源管理','/teacher/resources?courseId=2','TeacherResourceManagement',1,'2026-07-09 11:04:18','2026-07-09 11:04:18','2026-07-09 11:04:18'),(60,4,'公共资源学习首页','/user/home','UserHome',4,'2026-07-09 18:51:37','2026-07-09 11:07:38','2026-07-09 18:51:37'),(61,4,'资源学习','/resources','UnifiedResourceCenter',4,'2026-07-09 18:51:37','2026-07-09 11:07:40','2026-07-09 18:51:37'),(62,4,'我的课堂','/classrooms','UnifiedClassroomList',5,'2026-07-09 18:51:36','2026-07-09 11:07:42','2026-07-09 18:51:36'),(63,4,'课堂建设','/teacher/courses/1/edit','TeacherCourseEditor',1,'2026-07-09 11:07:44','2026-07-09 11:07:44','2026-07-09 11:07:44'),(64,5,'章节详情','/classrooms/1/chapters/2?step=step-1','UnifiedClassroomChapterLearning',8,'2026-07-09 18:29:48','2026-07-09 11:30:12','2026-07-09 18:29:48'),(65,5,'章节详情','/classrooms/1/chapters/1?step=step-1','UnifiedClassroomChapterLearning',15,'2026-07-09 12:46:55','2026-07-09 11:30:15','2026-07-09 12:46:55'),(66,5,'章节详情','/classrooms/1/chapters/1?step=step-2','UnifiedClassroomChapterLearning',12,'2026-07-09 18:29:46','2026-07-09 11:33:42','2026-07-09 18:29:46'),(67,5,'章节详情','/classrooms/1/chapters/2?step=step-2','UnifiedClassroomChapterLearning',4,'2026-07-09 18:29:49','2026-07-09 11:34:25','2026-07-09 18:29:49'),(68,3,'报告批改','/teacher/reports','TeacherReportReview',6,'2026-07-09 16:34:01','2026-07-09 11:45:10','2026-07-09 16:34:01'),(69,3,'课堂学习路径','/classrooms/1/learn?experimentId=1','UnifiedClassroomLearning',1,'2026-07-09 11:51:43','2026-07-09 11:51:43','2026-07-09 11:51:43'),(70,3,'课堂管理','/teacher/courses?create=1','TeacherCourseManagement',3,'2026-07-09 12:22:14','2026-07-09 12:11:08','2026-07-09 12:22:14'),(71,3,'课堂建设','/teacher/courses/4/edit','TeacherCourseEditor',2,'2026-07-09 12:22:55','2026-07-09 12:22:48','2026-07-09 12:22:55'),(72,3,'实验路径','/teacher/experiments?courseId=1','TeacherExperimentManagement',17,'2026-07-09 16:29:34','2026-07-09 12:28:49','2026-07-09 16:29:34'),(73,3,'试卷管理','/teacher/exam-papers?courseId=1','TeacherExamPaperManagement',5,'2026-07-09 16:58:59','2026-07-09 16:15:18','2026-07-09 16:58:59'),(74,3,'资源管理','/teacher/resources?experimentId=1','TeacherResourceManagement',1,'2026-07-09 16:24:56','2026-07-09 16:24:56','2026-07-09 16:24:56'),(75,3,'课堂学习路径','/classrooms/1/learn?module=report','UnifiedClassroomLearning',2,'2026-07-09 18:13:17','2026-07-09 16:47:50','2026-07-09 18:13:17'),(76,3,'报告批改','/teacher/reports?courseId=1','TeacherReportReview',1,'2026-07-09 17:02:40','2026-07-09 17:02:40','2026-07-09 17:02:40'),(77,3,'个人中心','/profile','ProfileCenter',1,'2026-07-09 18:12:33','2026-07-09 18:12:33','2026-07-09 18:12:33'),(78,4,'课堂详细','/teacher/courses/3/edit','TeacherCourseEditor',1,'2026-07-09 18:37:33','2026-07-09 18:37:33','2026-07-09 18:37:33'),(79,4,'学习交流','/discussions','DiscussionCenter',2,'2026-07-09 18:51:36','2026-07-09 18:51:36','2026-07-09 18:51:36'),(80,4,'消息与日程','/messages','MessageCenter',1,'2026-07-09 18:51:36','2026-07-09 18:51:36','2026-07-09 18:51:36'),(81,8,'公共资源学习首页','/user/home','UserHome',2,'2026-07-09 19:01:44','2026-07-09 19:01:22','2026-07-09 19:01:44'),(82,8,'我的课堂','/classrooms','UnifiedClassroomList',2,'2026-07-09 19:01:32','2026-07-09 19:01:24','2026-07-09 19:01:32'),(83,8,'课堂学习路径','/classrooms/2/learn','UnifiedClassroomLearning',1,'2026-07-09 19:01:29','2026-07-09 19:01:29','2026-07-09 19:01:29'),(84,8,'课堂学习路径','/classrooms/2/learn?module=tasks','UnifiedClassroomLearning',1,'2026-07-09 19:01:30','2026-07-09 19:01:30','2026-07-09 19:01:30'),(85,8,'课堂学习路径','/classrooms/2/learn?module=ai','UnifiedClassroomLearning',1,'2026-07-09 19:01:31','2026-07-09 19:01:31','2026-07-09 19:01:31'),(86,9,'公共资源学习首页','/user/home','UserHome',1,'2026-07-09 19:20:47','2026-07-09 19:20:47','2026-07-09 19:20:47'),(87,9,'资源学习','/resources','UnifiedResourceCenter',1,'2026-07-09 19:20:48','2026-07-09 19:20:48','2026-07-09 19:20:48'),(88,9,'我的课堂','/classrooms','UnifiedClassroomList',3,'2026-07-09 19:21:05','2026-07-09 19:20:48','2026-07-09 19:21:05'),(89,9,'课堂学习路径','/classrooms/3/learn','UnifiedClassroomLearning',2,'2026-07-09 19:21:00','2026-07-09 19:20:52','2026-07-09 19:21:00'),(90,9,'课堂学习路径','/classrooms/3/learn?module=records','UnifiedClassroomLearning',1,'2026-07-09 19:21:02','2026-07-09 19:21:02','2026-07-09 19:21:02'),(91,9,'课堂学习路径','/classrooms/3/learn?module=reservation','UnifiedClassroomLearning',1,'2026-07-09 19:21:02','2026-07-09 19:21:02','2026-07-09 19:21:02'),(92,9,'课堂学习路径','/classrooms/3/learn?module=chapters','UnifiedClassroomLearning',1,'2026-07-09 19:21:03','2026-07-09 19:21:03','2026-07-09 19:21:03'),(93,1,'消息与日程','/messages','MessageCenter',1,'2026-07-10 09:39:09','2026-07-10 09:39:09','2026-07-10 09:39:09'),(94,3,'安全题库与组卷','/teacher/safety-exams','TeacherSafetyExamManager',10,'2026-07-10 10:40:07','2026-07-10 10:29:39','2026-07-10 10:40:07'),(95,3,'安全知识考核','/safety-exams','StudentSafetyExamCenter',1,'2026-07-10 10:37:00','2026-07-10 10:37:00','2026-07-10 10:37:00'),(96,3,'课堂题库与组卷','/teacher/courses/1/safety-exams','TeacherCourseSafetyExamManager',10,'2026-07-10 16:26:30','2026-07-10 11:45:53','2026-07-10 16:26:30'),(97,3,'课堂详细','/teacher/courses/1/edit?step=exam','TeacherCourseEditor',7,'2026-07-10 16:26:32','2026-07-10 11:46:29','2026-07-10 16:26:32'),(98,5,'安全知识考核','/classrooms/1/safety-exams?paperId=1','ClassroomSafetyExamCenter',3,'2026-07-10 11:50:27','2026-07-10 11:49:57','2026-07-10 11:50:27'),(99,5,'安全知识考核','/classrooms/1/safety-exams','ClassroomSafetyExamCenter',11,'2026-07-10 11:56:22','2026-07-10 11:50:47','2026-07-10 11:56:22'),(100,6,'课堂学习路径','/classrooms/2/learn?module=exam','UnifiedClassroomLearning',2,'2026-07-10 11:57:29','2026-07-10 11:57:21','2026-07-10 11:57:29'),(101,6,'安全知识考核','/classrooms/2/safety-exams','ClassroomSafetyExamCenter',1,'2026-07-10 11:57:24','2026-07-10 11:57:24','2026-07-10 11:57:24'),(102,5,'试卷答题','/classrooms/1/exams/1/take','ClassroomExamTaking',4,'2026-07-10 16:13:04','2026-07-10 12:17:44','2026-07-10 16:13:04'),(103,5,'课堂学习路径','/classrooms/1/learn?module=exam&examTab=records','UnifiedClassroomLearning',5,'2026-07-10 16:21:59','2026-07-10 16:13:15','2026-07-10 16:21:59'),(104,5,'课堂学习路径','/classrooms/1/learn?module=report&examTab=records','UnifiedClassroomLearning',4,'2026-07-10 16:22:03','2026-07-10 16:13:35','2026-07-10 16:22:03'),(105,5,'课堂学习路径','/classrooms/1/learn?module=discussion&examTab=records','UnifiedClassroomLearning',2,'2026-07-10 16:22:04','2026-07-10 16:13:35','2026-07-10 16:22:04'),(106,5,'课堂学习路径','/classrooms/1/learn?module=ai&examTab=records','UnifiedClassroomLearning',1,'2026-07-10 16:13:36','2026-07-10 16:13:36','2026-07-10 16:13:36'),(107,3,'课堂题库与组卷','/teacher/courses/1/safety-exams?tab=papers&create=paper','TeacherCourseSafetyExamManager',2,'2026-07-10 16:26:33','2026-07-10 16:17:14','2026-07-10 16:26:33'),(108,5,'试卷答题','/classrooms/1/exams/2/take','ClassroomExamTaking',1,'2026-07-10 16:20:10','2026-07-10 16:20:10','2026-07-10 16:20:10'),(109,5,'课堂学习路径','/classrooms/1/learn?module=chapters&examTab=records','UnifiedClassroomLearning',3,'2026-07-10 16:22:05','2026-07-10 16:21:35','2026-07-10 16:22:05'),(110,5,'课堂学习路径','/classrooms/1/learn?module=tasks&examTab=records','UnifiedClassroomLearning',2,'2026-07-10 16:22:05','2026-07-10 16:21:41','2026-07-10 16:22:05'),(111,5,'课堂学习路径','/classrooms/1/learn?module=reservation&examTab=records','UnifiedClassroomLearning',2,'2026-07-10 16:21:58','2026-07-10 16:21:46','2026-07-10 16:21:58');
/*!40000 ALTER TABLE `t_recent_visit` ENABLE KEYS */;
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
  `clicked` tinyint DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recommend_student_experiment` (`student_id`,`experiment_id`),
  KEY `idx_recommend_resource` (`resource_id`)
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_report`
--

LOCK TABLES `t_report` WRITE;
/*!40000 ALTER TABLE `t_report` DISABLE KEYS */;
INSERT INTO `t_report` (`id`, `student_id`, `experiment_id`, `title`, `content`, `file_url`, `status`, `submit_time`, `latest_submit_time`, `create_time`, `update_time`, `is_deleted`) VALUES (1,5,1,'钻井液密度与流变性能测试报告','数据完整，结果满足目标窗口，需进一步关注读数误差。',NULL,'GRADED','2026-07-07 12:04:06','2026-07-07 12:04:06','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,6,3,'管输压降实验预报告','已完成指导书阅读，等待实验数据。',NULL,'DRAFT',NULL,NULL,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_report_rubric_item`
--

DROP TABLE IF EXISTS `t_report_rubric_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_report_rubric_item`
--

LOCK TABLES `t_report_rubric_item` WRITE;
/*!40000 ALTER TABLE `t_report_rubric_item` DISABLE KEYS */;
INSERT INTO `t_report_rubric_item` VALUES (1,1,'数据与证据','数据完整、单位清晰、证据充分',40,1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,1,'分析与复盘','能解释现象并提出安全改进',60,2,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(3,3,'数据与证据','数据完整、单位清晰、证据充分',40,1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(4,3,'分析与复盘','能解释现象并提出安全改进',60,2,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(5,4,'数据与证据','数据完整、单位清晰、证据充分',40,1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(6,4,'分析与复盘','能解释现象并提出安全改进',60,2,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_report_rubric_item` ENABLE KEYS */;
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
  `is_latest` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `grade_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_score_report_latest` (`report_id`,`is_latest`),
  KEY `idx_report_score_grade_time` (`grade_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_report_score`
--

LOCK TABLES `t_report_score` WRITE;
/*!40000 ALTER TABLE `t_report_score` DISABLE KEYS */;
INSERT INTO `t_report_score` VALUES (1,1,3,88,'数据完整，风险复盘可以再具体。',1,'2026-07-08 12:04:06','2026-07-08 12:04:06');
/*!40000 ALTER TABLE `t_report_score` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_report_score_item`
--

DROP TABLE IF EXISTS `t_report_score_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_report_score_item`
--

LOCK TABLES `t_report_score_item` WRITE;
/*!40000 ALTER TABLE `t_report_score_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_report_score_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_report_template`
--

DROP TABLE IF EXISTS `t_report_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_report_template`
--

LOCK TABLES `t_report_template` WRITE;
/*!40000 ALTER TABLE `t_report_template` DISABLE KEYS */;
INSERT INTO `t_report_template` VALUES (1,1,'钻井液性能测试实验报告模板','{\"sections\": [\"实验目的\", \"数据记录\", \"结果分析\", \"风险复盘\"]}',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,3,'管输压降实验报告模板','{\"sections\": [\"实验目的\", \"数据记录\", \"结果分析\", \"风险复盘\"]}',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(3,4,'HSE事故案例复盘报告模板','{\"sections\": [\"实验目的\", \"数据记录\", \"结果分析\", \"风险复盘\"]}',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_report_template` ENABLE KEYS */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_reservation`
--

LOCK TABLES `t_reservation` WRITE;
/*!40000 ALTER TABLE `t_reservation` DISABLE KEYS */;
INSERT INTO `t_reservation` VALUES (1,5,1,101,1,'完成钻井液性能测试实验','APPROVED',3,'准入已通过，按时到场。','2026-07-07 12:04:06','2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,6,2,102,3,'管输压降实验预约','PENDING',3,NULL,NULL,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_reservation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_resource`
--

DROP TABLE IF EXISTS `t_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_resource`
--

LOCK TABLES `t_resource` WRITE;
/*!40000 ALTER TABLE `t_resource` DISABLE KEYS */;
INSERT INTO `t_resource` VALUES (1,1,1,'钻井液性能测试沉浸视频','TEACHING_VIDEO','钻井液密度','飞溅,滑倒','钻井液密度,飞溅,滑倒,油气实验','PREVIEW','钻井液性能测试沉浸视频，用于理解实验目标、设备和风险节点。','https://example.com/mud_video',NULL,NULL,'video/mp4',0,1,'PROGRESS_TIME',300,85,NULL,NULL,'COURSE',0,NULL,32,1,2,4,0,4.60,3,1,1,3,'2026-07-08 12:04:06','2026-07-09 11:21:57',0),(2,1,2,'井控关井流程动画','TEACHING_VIDEO','井控关井','高压,误操作','井控关井,高压,误操作,油气实验','PREVIEW','井控关井流程动画，用于理解实验目标、设备和风险节点。','https://example.com/well_video',NULL,NULL,'video/mp4',0,1,'PROGRESS_TIME',300,85,NULL,NULL,'COURSE',0,NULL,25,2,3,5,0,4.60,3,1,2,3,'2026-07-08 12:04:06','2026-07-09 11:21:59',0),(3,2,3,'管输压降实验指导书','DOCUMENT','管输压降','压力,机械','管输压降,压力,机械,油气实验','PREVIEW','管输压降实验指导书，用于理解实验目标、设备和风险节点。','https://example.com/pipe_doc',NULL,NULL,'video/mp4',0,1,'PROGRESS_TIME',300,85,NULL,NULL,'COURSE',0,NULL,23,3,4,6,0,4.60,3,1,3,3,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(4,3,4,'油气实验室事故案例开放课','LINK','事故链分析','HSE','事故链分析,HSE,油气实验','EXTENSION','油气实验室事故案例开放课，用于理解实验目标、设备和风险节点。','https://example.com/hse_open',NULL,NULL,'text/html',0,0,'CONFIRM',0,100,NULL,NULL,'PUBLIC',0,NULL,26,4,5,7,0,4.60,3,1,4,3,'2026-07-08 12:04:06','2026-07-10 16:27:33',0),(5,1,1,'马氏漏斗操作图解','IMAGE','漏斗黏度','玻璃器皿','漏斗黏度,玻璃器皿,油气实验','EXTENSION','马氏漏斗操作图解，用于理解实验目标、设备和风险节点。','https://example.com/mud_image',NULL,NULL,'video/mp4',0,0,'CONFIRM',0,100,NULL,NULL,'PUBLIC',0,NULL,29,5,6,8,0,4.60,3,1,5,3,'2026-07-08 12:04:06','2026-07-09 18:28:50',0);
/*!40000 ALTER TABLE `t_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_resource_interaction`
--

DROP TABLE IF EXISTS `t_resource_interaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_resource_interaction`
--

LOCK TABLES `t_resource_interaction` WRITE;
/*!40000 ALTER TABLE `t_resource_interaction` DISABLE KEYS */;
INSERT INTO `t_resource_interaction` VALUES (1,1,5,1,1,4.5,'视频步骤清楚。','2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_resource_interaction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_resource_submission`
--

DROP TABLE IF EXISTS `t_resource_submission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_resource_submission`
--

LOCK TABLES `t_resource_submission` WRITE;
/*!40000 ALTER TABLE `t_resource_submission` DISABLE KEYS */;
INSERT INTO `t_resource_submission` VALUES (1,8,'井筒压力平衡公开动画','LINK','油气工程','HSE','油气,HSE,公开资源','井筒压力平衡公开动画演示投稿。','https://example.com/user_oilfan-approved',NULL,NULL,'text/html','APPROVED',3,'资源适合公开学习，审核通过','2026-07-08 12:04:06',NULL,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,9,'海上平台HSE短视频合集','LINK','油气工程','HSE','油气,HSE,公开资源','海上平台HSE短视频合集演示投稿。','https://example.com/user_guest-pending',NULL,NULL,'text/html','PENDING',NULL,NULL,NULL,NULL,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(3,6,'无关娱乐网站链接','LINK','油气工程','HSE','油气,HSE,公开资源','无关娱乐网站链接演示投稿。','https://example.com/student_li-rejected',NULL,NULL,'text/html','REJECTED',1,'与油气工程学习无关','2026-07-08 12:04:06',NULL,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_resource_submission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_resource_timeline_note`
--

DROP TABLE IF EXISTS `t_resource_timeline_note`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_resource_timeline_note`
--

LOCK TABLES `t_resource_timeline_note` WRITE;
/*!40000 ALTER TABLE `t_resource_timeline_note` DISABLE KEYS */;
INSERT INTO `t_resource_timeline_note` VALUES (1,1,1,5,180,'QUESTION','密度计读数前气泡如何快速排除？','COURSE',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_resource_timeline_note` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_role`
--

DROP TABLE IF EXISTS `t_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_role`
--

LOCK TABLES `t_role` WRITE;
/*!40000 ALTER TABLE `t_role` DISABLE KEYS */;
INSERT INTO `t_role` VALUES (1,'普通用户','USER','公共资源学习、交流、投稿和加入课堂','2026-07-08 12:04:06'),(5,'系统管理员','ADMIN','平台治理、权限、审核与日志','2026-07-08 12:04:06');
/*!40000 ALTER TABLE `t_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_role_permission`
--

DROP TABLE IF EXISTS `t_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_role_permission` (
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_role_permission`
--

LOCK TABLES `t_role_permission` WRITE;
/*!40000 ALTER TABLE `t_role_permission` DISABLE KEYS */;
INSERT INTO `t_role_permission` VALUES (1,1),(1,2),(1,3),(1,5),(1,6),(1,8),(1,9),(1,18),(1,20),(1,24),(1,28),(1,30),(1,31),(1,37),(1,40),(1,55),(1,57),(5,1),(5,2),(5,3),(5,4),(5,5),(5,6),(5,7),(5,8),(5,9),(5,10),(5,11),(5,12),(5,13),(5,14),(5,15),(5,16),(5,17),(5,18),(5,19),(5,20),(5,21),(5,22),(5,23),(5,24),(5,25),(5,26),(5,27),(5,28),(5,29),(5,30),(5,31),(5,32),(5,33),(5,34),(5,35),(5,36),(5,37),(5,38),(5,39),(5,40),(5,41),(5,42),(5,43),(5,44),(5,45),(5,46),(5,47),(5,48),(5,49),(5,50),(5,55),(5,56),(5,58),(5,59),(5,60),(5,61),(5,62),(5,63),(5,64),(5,65);
/*!40000 ALTER TABLE `t_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_teacher_certification`
--

DROP TABLE IF EXISTS `t_teacher_certification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_teacher_certification`
--

LOCK TABLES `t_teacher_certification` WRITE;
/*!40000 ALTER TABLE `t_teacher_certification` DISABLE KEYS */;
INSERT INTO `t_teacher_certification` VALUES (1,3,'中国石油大学','T2026001','wanghf@cupk.edu.cn','APPROVED',1,'教师身份核验通过','2026-06-18 12:04:06','2026-06-17 12:04:06','2026-07-08 12:04:06',0),(2,4,'中国石油大学','T2026002','licx@cupk.edu.cn','APPROVED',1,'教师身份核验通过','2026-06-20 12:04:06','2026-06-19 12:04:06','2026-07-08 12:04:06',0),(3,9,'西部能源学院','EN202612','guest@edu.cn','PENDING',NULL,NULL,NULL,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_teacher_certification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_teaching_class`
--

DROP TABLE IF EXISTS `t_teaching_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_teaching_class`
--

LOCK TABLES `t_teaching_class` WRITE;
/*!40000 ALTER TABLE `t_teaching_class` DISABLE KEYS */;
INSERT INTO `t_teaching_class` VALUES (1,1,'钻井液实验1班',3,4,'油工2301','2026秋',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(2,2,'集输实验2班',3,NULL,'储运2302','2026秋',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0),(3,3,'HSE案例研讨班',4,NULL,'安工2301','2026秋',1,'2026-07-08 12:04:06','2026-07-08 12:04:06',0);
/*!40000 ALTER TABLE `t_teaching_class` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_token`
--

DROP TABLE IF EXISTS `t_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(64) NOT NULL,
  `expire_time` datetime NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`)
) ENGINE=InnoDB AUTO_INCREMENT=73 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='令牌表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_token`
--

LOCK TABLES `t_token` WRITE;
/*!40000 ALTER TABLE `t_token` DISABLE KEYS */;
INSERT INTO `t_token` VALUES (72,3,'20a628985df44b3e92a49ed497c1dc44','2026-07-11 16:22:20','2026-07-10 16:22:19');
/*!40000 ALTER TABLE `t_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_user`
--

DROP TABLE IF EXISTS `t_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_user`
--

LOCK TABLES `t_user` WRITE;
/*!40000 ALTER TABLE `t_user` DISABLE KEYS */;
INSERT INTO `t_user` VALUES (1,'admin','e10adc3949ba59abbe56e057f20f883e','系统管理员','13800000001',NULL,'平台治理中心',NULL,'admin@cupk.edu.cn',1,'2026-07-08 12:04:06','2026-07-08 12:04:06'),(3,'teacher_wang','e10adc3949ba59abbe56e057f20f883e','王海峰','13800000003',NULL,'石油工程学院',NULL,'wanghf@cupk.edu.cn',1,'2026-07-08 12:04:06','2026-07-08 12:04:06'),(4,'teacher_li','e10adc3949ba59abbe56e057f20f883e','李晨曦','13800000004',NULL,'安全工程学院',NULL,'licx@cupk.edu.cn',1,'2026-07-08 12:04:06','2026-07-08 12:04:06'),(5,'student_zhang','e10adc3949ba59abbe56e057f20f883e','张雨辰','13800000005',NULL,'石油工程','油工2301','zhangyc@example.com',1,'2026-07-08 12:04:06','2026-07-08 12:04:06'),(6,'student_li','e10adc3949ba59abbe56e057f20f883e','李思源','13800000006',NULL,'油气储运','储运2302','lisy@example.com',1,'2026-07-08 12:04:06','2026-07-08 12:04:06'),(7,'student_chen','e10adc3949ba59abbe56e057f20f883e','陈若冰','13800000007',NULL,'安全工程','安工2301','chenrb@example.com',1,'2026-07-08 12:04:06','2026-07-08 12:04:06'),(8,'user_oilfan','e10adc3949ba59abbe56e057f20f883e','赵一鸣','13800000008',NULL,'油气工程兴趣用户','公开学习者','oilfan@example.com',1,'2026-07-08 12:04:06','2026-07-08 12:04:06'),(9,'user_guest','e10adc3949ba59abbe56e057f20f883e','周晓禾','13800000009',NULL,'跨专业学习','公开学习者','guest@example.com',1,'2026-07-08 12:04:06','2026-07-08 12:04:06');
/*!40000 ALTER TABLE `t_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_user_role`
--

DROP TABLE IF EXISTS `t_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_user_role`
--

LOCK TABLES `t_user_role` WRITE;
/*!40000 ALTER TABLE `t_user_role` DISABLE KEYS */;
INSERT INTO `t_user_role` VALUES (1,5),(3,1),(4,1),(5,1),(6,1),(7,1),(8,1),(9,1);
/*!40000 ALTER TABLE `t_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_user_shortcut`
--

DROP TABLE IF EXISTS `t_user_shortcut`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_user_shortcut`
--

LOCK TABLES `t_user_shortcut` WRITE;
/*!40000 ALTER TABLE `t_user_shortcut` DISABLE KEYS */;
INSERT INTO `t_user_shortcut` VALUES (1,5,'我的课堂','/classrooms','classroom',1,'2026-07-08 12:04:06','2026-07-08 12:04:06'),(2,8,'资源学习','/resources','resource',1,'2026-07-08 12:04:06','2026-07-08 12:04:06'),(3,3,'课堂管理','/teacher/courses','course',1,'2026-07-08 12:04:06','2026-07-08 12:04:06'),(4,1,'教师认证审核','/admin/teacher-certifications','admin',1,'2026-07-08 12:04:06','2026-07-08 12:04:06');
/*!40000 ALTER TABLE `t_user_shortcut` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-10 16:31:52
