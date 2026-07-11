-- Keep existing databases aligned with the exam and reservation entities.
ALTER TABLE `t_exam_paper`
  ADD COLUMN `objective_score` int DEFAULT 100 AFTER `total_score`,
  ADD COLUMN `subjective_score` int DEFAULT 0 AFTER `objective_score`;

ALTER TABLE `t_exam_answer`
  ADD COLUMN `grading_comment` varchar(500) DEFAULT NULL AFTER `score`;

ALTER TABLE `t_experiment`
  ADD COLUMN `admission_paper_id` bigint DEFAULT NULL AFTER `exam_required`;
