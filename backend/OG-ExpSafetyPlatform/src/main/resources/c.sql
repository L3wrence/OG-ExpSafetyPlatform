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

--
-- Table structure for table ` t_exam_paper_question`
--

DROP TABLE IF EXISTS ` t_exam_paper_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE ` t_exam_paper_question` (
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

LOCK TABLES ` t_exam_paper_question` WRITE;
/*!40000 ALTER TABLE ` t_exam_paper_question` DISABLE KEYS */;
/*!40000 ALTER TABLE ` t_exam_paper_question` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table ` t_lab_time_slot`
--

DROP TABLE IF EXISTS ` t_lab_time_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE ` t_lab_time_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lab_id` bigint NOT NULL,
  `date` date NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `capacity` int NOT NULL,
  `booked_count` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'AVAILABLE',
  ` create_by` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table ` t_lab_time_slot`
--

LOCK TABLES ` t_lab_time_slot` WRITE;
/*!40000 ALTER TABLE ` t_lab_time_slot` DISABLE KEYS */;
/*!40000 ALTER TABLE ` t_lab_time_slot` ENABLE KEYS */;
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
  `id` bigint NOT NULL,
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
