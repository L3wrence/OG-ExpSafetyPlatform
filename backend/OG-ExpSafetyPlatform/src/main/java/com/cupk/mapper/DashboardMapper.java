package com.cupk.mapper;

import com.cupk.vo.CapacityUsageVO;
import com.cupk.vo.CourseCompletionVO;
import com.cupk.vo.DashboardOverviewVO;
import com.cupk.vo.ExamPassRateVO;
import com.cupk.vo.PieItemVO;
import com.cupk.vo.ResourceCompletionVO;
import com.cupk.vo.ResourceRankingVO;
import com.cupk.vo.TrendItemVO;
import com.cupk.vo.WrongKnowledgeRankingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DashboardMapper {
    @Select("""
        SELECT
          (SELECT COUNT(*)
             FROM t_lab_course c
            WHERE c.deleted = 0
              AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
              AND (#{courseId} IS NULL OR c.id = #{courseId})
              AND (#{studentId} IS NULL OR EXISTS (
                    SELECT 1 FROM t_course_student cs
                     WHERE cs.course_id = c.id AND cs.student_id = #{studentId}
                       AND cs.status = 1 AND cs.deleted = 0))) AS course_count,
          (SELECT COUNT(*)
             FROM t_experiment e
             JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
            WHERE e.deleted = 0
              AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
              AND (#{courseId} IS NULL OR c.id = #{courseId})
              AND (#{experimentId} IS NULL OR e.id = #{experimentId})
              AND (#{studentId} IS NULL OR EXISTS (
                    SELECT 1 FROM t_course_student cs
                     WHERE cs.course_id = c.id AND cs.student_id = #{studentId}
                       AND cs.status = 1 AND cs.deleted = 0))) AS experiment_count,
          (SELECT COUNT(*)
             FROM t_resource r
             JOIN t_experiment e ON e.id = r.experiment_id AND e.deleted = 0
             JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
            WHERE r.deleted = 0
              AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
              AND (#{courseId} IS NULL OR c.id = #{courseId})
              AND (#{experimentId} IS NULL OR e.id = #{experimentId})
              AND (#{studentId} IS NULL OR EXISTS (
                    SELECT 1 FROM t_course_student cs
                     WHERE cs.course_id = c.id AND cs.student_id = #{studentId}
                       AND cs.status = 1 AND cs.deleted = 0))) AS resource_count,
          (SELECT COUNT(DISTINCT cs.student_id)
             FROM t_course_student cs
             JOIN t_lab_course c ON c.id = cs.course_id AND c.deleted = 0
            WHERE cs.deleted = 0 AND cs.status = 1
              AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
              AND (#{courseId} IS NULL OR c.id = #{courseId})
              AND (#{studentId} IS NULL OR cs.student_id = #{studentId})) AS student_count,
          (SELECT COUNT(*)
             FROM t_reservation rv
             JOIN t_experiment e ON e.id = rv.experiment_id AND e.deleted = 0
             JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
            WHERE rv.deleted = 0
              AND rv.create_time >= #{monthStart}
              AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
              AND (#{courseId} IS NULL OR c.id = #{courseId})
              AND (#{experimentId} IS NULL OR e.id = #{experimentId})
              AND (#{studentId} IS NULL OR rv.student_id = #{studentId})) AS month_reservation_count,
          (SELECT COUNT(*)
             FROM t_reservation rv
             JOIN t_experiment e ON e.id = rv.experiment_id AND e.deleted = 0
             JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
            WHERE rv.deleted = 0
              AND CAST(rv.status AS CHAR) IN ('PENDING', 'WAIT_AUDIT', '待审核', '0')
              AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
              AND (#{courseId} IS NULL OR c.id = #{courseId})
              AND (#{experimentId} IS NULL OR e.id = #{experimentId})
              AND (#{studentId} IS NULL OR rv.student_id = #{studentId})) AS pending_reservation_count,
          (SELECT COUNT(*)
             FROM t_report rp
             JOIN t_experiment e ON e.id = rp.experiment_id AND e.deleted = 0
             JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
            WHERE rp.deleted = 0
              AND CAST(rp.status AS CHAR) IN ('SUBMITTED', 'PENDING_REVIEW', '待批改', '1')
              AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
              AND (#{courseId} IS NULL OR c.id = #{courseId})
              AND (#{experimentId} IS NULL OR e.id = #{experimentId})
              AND (#{studentId} IS NULL OR rp.student_id = #{studentId})) AS pending_report_count,
          (SELECT COUNT(DISTINCT er.id)
             FROM t_exam_record er
             JOIN t_exam_paper ep ON ep.id = er.paper_id AND ep.deleted = 0
             JOIN t_lab_course c ON c.id = ep.course_id AND c.deleted = 0
            WHERE er.deleted = 0
              AND er.status = 'PENDING_REVIEW'
              AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
              AND (#{courseId} IS NULL OR c.id = #{courseId})
              AND (#{experimentId} IS NULL OR er.experiment_id = #{experimentId})
              AND (#{studentId} IS NULL OR er.student_id = #{studentId})) AS pending_subjective_count,
          (SELECT COALESCE(ROUND(100 * SUM(CASE WHEN er.passed = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 2), 0)
             FROM t_exam_record er
             JOIN t_experiment e ON e.id = er.experiment_id AND e.deleted = 0
             JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
            WHERE er.deleted = 0
              AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
              AND (#{courseId} IS NULL OR c.id = #{courseId})
              AND (#{experimentId} IS NULL OR e.id = #{experimentId})
              AND (#{studentId} IS NULL OR er.student_id = #{studentId})) AS exam_pass_rate
        """)
    DashboardOverviewVO selectOverview(@Param("teacherId") Long teacherId,
                                       @Param("studentId") Long studentId,
                                       @Param("courseId") Long courseId,
                                       @Param("experimentId") Long experimentId,
                                       @Param("monthStart") LocalDateTime monthStart);

    @Select("""
        SELECT c.id AS course_id,
               c.course_name,
               COUNT(DISTINCT s.student_id) AS student_count,
               COUNT(DISTINCT r.id) AS required_resource_count,
               COUNT(DISTINCT CASE WHEN lr.finish_flag = 1 THEN CONCAT(lr.student_id, '-', lr.resource_id) END) AS finished_count,
               COALESCE(ROUND(
                 100 * COUNT(DISTINCT CASE WHEN lr.finish_flag = 1 THEN CONCAT(lr.student_id, '-', lr.resource_id) END)
                 / NULLIF(COUNT(DISTINCT s.student_id) * COUNT(DISTINCT r.id), 0), 2), 0) AS completion_rate
          FROM t_lab_course c
          JOIN t_experiment e ON e.course_id = c.id AND e.deleted = 0
          JOIN t_resource r ON r.experiment_id = e.id AND r.required_flag = 1 AND r.status = 1 AND r.deleted = 0
          LEFT JOIN (
                SELECT course_id, student_id FROM t_course_student
                 WHERE deleted = 0 AND status = 1
                UNION
                SELECT e2.course_id, lr2.student_id
                  FROM t_learning_record lr2
                  JOIN t_experiment e2 ON e2.id = lr2.experiment_id AND e2.deleted = 0
                 WHERE lr2.deleted = 0
          ) s ON s.course_id = c.id
          LEFT JOIN t_learning_record lr ON lr.resource_id = r.id
                AND lr.student_id = s.student_id AND lr.deleted = 0
         WHERE c.deleted = 0
           AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{courseId} IS NULL OR c.id = #{courseId})
           AND (#{experimentId} IS NULL OR e.id = #{experimentId})
           AND (#{studentId} IS NULL OR s.student_id = #{studentId})
         GROUP BY c.id, c.course_name
         ORDER BY c.id DESC
        """)
    List<CourseCompletionVO> selectCourseCompletion(@Param("teacherId") Long teacherId,
                                                    @Param("studentId") Long studentId,
                                                    @Param("courseId") Long courseId,
                                                    @Param("experimentId") Long experimentId);

    @Select("""
        SELECT COALESCE(r.resource_type, 'UNKNOWN') AS name,
               CAST(COUNT(*) AS DECIMAL(20,2)) AS value
          FROM t_resource r
          JOIN t_experiment e ON e.id = r.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE r.deleted = 0
           AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{courseId} IS NULL OR c.id = #{courseId})
           AND (#{experimentId} IS NULL OR e.id = #{experimentId})
         GROUP BY r.resource_type
         ORDER BY value DESC
        """)
    List<PieItemVO> selectResourceTypeDistribution(@Param("teacherId") Long teacherId,
                                                   @Param("courseId") Long courseId,
                                                   @Param("experimentId") Long experimentId);

    @Select("""
        SELECT r.id AS resource_id,
               r.title,
               r.resource_type,
               COALESCE(r.view_count, 0) AS view_count
          FROM t_resource r
          JOIN t_experiment e ON e.id = r.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE r.deleted = 0
           AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{courseId} IS NULL OR c.id = #{courseId})
           AND (#{experimentId} IS NULL OR e.id = #{experimentId})
         ORDER BY COALESCE(r.view_count, 0) DESC, r.id DESC
         LIMIT #{limit}
        """)
    List<ResourceRankingVO> selectHotResources(@Param("teacherId") Long teacherId,
                                               @Param("courseId") Long courseId,
                                               @Param("experimentId") Long experimentId,
                                               @Param("limit") Integer limit);

    @Select("""
        SELECT r.id AS resource_id,
               r.title,
               COUNT(DISTINCT s.student_id) AS student_count,
               COUNT(DISTINCT CASE WHEN lr.finish_flag = 1 THEN lr.student_id END) AS finished_student_count,
               COALESCE(ROUND(100 * COUNT(DISTINCT CASE WHEN lr.finish_flag = 1 THEN lr.student_id END)
                 / NULLIF(COUNT(DISTINCT s.student_id), 0), 2), 0) AS completion_rate
          FROM t_resource r
          JOIN t_experiment e ON e.id = r.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
          LEFT JOIN (
                SELECT course_id, student_id FROM t_course_student
                 WHERE deleted = 0 AND status = 1
                UNION
                SELECT e2.course_id, lr2.student_id
                  FROM t_learning_record lr2
                  JOIN t_experiment e2 ON e2.id = lr2.experiment_id AND e2.deleted = 0
                 WHERE lr2.deleted = 0
          ) s ON s.course_id = c.id
          LEFT JOIN t_learning_record lr ON lr.resource_id = r.id
                AND lr.student_id = s.student_id AND lr.deleted = 0
         WHERE r.deleted = 0 AND r.required_flag = 1 AND r.status = 1
           AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{courseId} IS NULL OR c.id = #{courseId})
           AND (#{experimentId} IS NULL OR e.id = #{experimentId})
           AND (#{studentId} IS NULL OR s.student_id = #{studentId})
         GROUP BY r.id, r.title
         ORDER BY completion_rate ASC, r.id DESC
        """)
    List<ResourceCompletionVO> selectResourceCompletion(@Param("teacherId") Long teacherId,
                                                        @Param("studentId") Long studentId,
                                                        @Param("courseId") Long courseId,
                                                        @Param("experimentId") Long experimentId);

    @Select("""
        SELECT e.id AS experiment_id,
               e.exp_name,
               COUNT(*) AS student_count,
               SUM(CASE WHEN best.best_passed = 1 OR best.best_score >= ep.pass_score THEN 1 ELSE 0 END) AS passed_count,
               COALESCE(ROUND(100 * SUM(CASE WHEN best.best_passed = 1 OR best.best_score >= ep.pass_score THEN 1 ELSE 0 END)
                 / NULLIF(COUNT(*), 0), 2), 0) AS pass_rate
          FROM (
                SELECT er.experiment_id, er.paper_id, er.student_id,
                       MAX(er.total_score) AS best_score,
                       MAX(CASE WHEN er.passed = 1 THEN 1 ELSE 0 END) AS best_passed
                  FROM t_exam_record er
                 WHERE er.deleted = 0
                   AND (#{studentId} IS NULL OR er.student_id = #{studentId})
                   AND (#{startTime} IS NULL OR er.submit_time >= #{startTime})
                   AND (#{endTime} IS NULL OR er.submit_time < #{endTime})
                 GROUP BY er.experiment_id, er.paper_id, er.student_id
          ) best
          JOIN t_exam_paper ep ON ep.id = best.paper_id AND ep.deleted = 0
          JOIN t_experiment e ON e.id = best.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{courseId} IS NULL OR c.id = #{courseId})
           AND (#{experimentId} IS NULL OR e.id = #{experimentId})
         GROUP BY e.id, e.exp_name
         ORDER BY pass_rate DESC
        """)
    List<ExamPassRateVO> selectExamPassRate(@Param("teacherId") Long teacherId,
                                            @Param("studentId") Long studentId,
                                            @Param("courseId") Long courseId,
                                            @Param("experimentId") Long experimentId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    @Select("""
        SELECT COALESCE(ea.knowledge_id, ea.question_id) AS knowledge_id,
               COALESCE(q.knowledge_point, sk.knowledge_point, CONCAT('知识点', COALESCE(ea.knowledge_id, ea.question_id))) AS knowledge_point,
               SUM(CASE WHEN ea.correct_flag = 0 THEN 1 ELSE 0 END) AS wrong_count,
               COUNT(*) AS answer_count,
               COALESCE(ROUND(100 * SUM(CASE WHEN ea.correct_flag = 0 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 2), 0) AS wrong_rate
          FROM t_exam_answer ea
          JOIN t_exam_record er ON er.id = ea.record_id AND er.deleted = 0
          JOIN t_experiment e ON e.id = er.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
          LEFT JOIN t_question q ON q.id = ea.question_id AND q.is_deleted = 0
          LEFT JOIN t_safety_knowledge sk ON sk.id = ea.knowledge_id AND sk.deleted = 0
         WHERE (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{studentId} IS NULL OR er.student_id = #{studentId})
           AND (#{courseId} IS NULL OR c.id = #{courseId})
           AND (#{experimentId} IS NULL OR e.id = #{experimentId})
           AND (#{startTime} IS NULL OR er.submit_time >= #{startTime})
           AND (#{endTime} IS NULL OR er.submit_time < #{endTime})
         GROUP BY COALESCE(ea.knowledge_id, ea.question_id),
                  COALESCE(q.knowledge_point, sk.knowledge_point, CONCAT('知识点', COALESCE(ea.knowledge_id, ea.question_id)))
         HAVING wrong_count > 0
         ORDER BY wrong_count DESC, wrong_rate DESC
         LIMIT #{limit}
        """)
    List<WrongKnowledgeRankingVO> selectWrongKnowledgeRanking(@Param("teacherId") Long teacherId,
                                                              @Param("studentId") Long studentId,
                                                              @Param("courseId") Long courseId,
                                                              @Param("experimentId") Long experimentId,
                                                              @Param("startTime") LocalDateTime startTime,
                                                              @Param("endTime") LocalDateTime endTime,
                                                              @Param("limit") Integer limit);

    @Select("""
        SELECT DATE_FORMAT(rv.create_time, '%Y-%m-%d') AS stat_date,
               COUNT(*) AS value
          FROM t_reservation rv
          JOIN t_experiment e ON e.id = rv.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE rv.deleted = 0
           AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{studentId} IS NULL OR rv.student_id = #{studentId})
           AND (#{courseId} IS NULL OR c.id = #{courseId})
           AND (#{experimentId} IS NULL OR e.id = #{experimentId})
           AND (#{startTime} IS NULL OR rv.create_time >= #{startTime})
           AND (#{endTime} IS NULL OR rv.create_time < #{endTime})
         GROUP BY DATE_FORMAT(rv.create_time, '%Y-%m-%d')
         ORDER BY stat_date ASC
        """)
    List<TrendItemVO> selectReservationTrend(@Param("teacherId") Long teacherId,
                                             @Param("studentId") Long studentId,
                                             @Param("courseId") Long courseId,
                                             @Param("experimentId") Long experimentId,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    @Select("""
        SELECT CAST(rv.status AS CHAR) AS name,
               CAST(COUNT(*) AS DECIMAL(20,2)) AS value
          FROM t_reservation rv
          JOIN t_experiment e ON e.id = rv.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE rv.deleted = 0
           AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{studentId} IS NULL OR rv.student_id = #{studentId})
           AND (#{courseId} IS NULL OR c.id = #{courseId})
           AND (#{experimentId} IS NULL OR e.id = #{experimentId})
           AND (#{startTime} IS NULL OR rv.create_time >= #{startTime})
           AND (#{endTime} IS NULL OR rv.create_time < #{endTime})
         GROUP BY rv.status
         ORDER BY value DESC
        """)
    List<PieItemVO> selectReservationStatusDistribution(@Param("teacherId") Long teacherId,
                                                        @Param("studentId") Long studentId,
                                                        @Param("courseId") Long courseId,
                                                        @Param("experimentId") Long experimentId,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    @Select("""
        SELECT ts.id AS time_slot_id,
               e.id AS experiment_id,
               e.exp_name,
               DATE_FORMAT(ts.slot_date, '%Y-%m-%d') AS slot_date,
               CAST(ts.start_time AS CHAR) AS start_time,
               CAST(ts.end_time AS CHAR) AS end_time,
               ts.capacity,
               ts.booked_count,
               COALESCE(ROUND(100 * ts.booked_count / NULLIF(ts.capacity, 0), 2), 0) AS usage_rate
          FROM t_lab_time_slot ts
          JOIN t_experiment e ON e.id = ts.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{courseId} IS NULL OR c.id = #{courseId})
           AND (#{experimentId} IS NULL OR e.id = #{experimentId})
           AND (#{startTime} IS NULL OR ts.slot_date >= DATE(#{startTime}))
           AND (#{endTime} IS NULL OR ts.slot_date < DATE(#{endTime}))
         ORDER BY ts.slot_date ASC, ts.start_time ASC
         LIMIT #{limit}
        """)
    List<CapacityUsageVO> selectCapacityUsage(@Param("teacherId") Long teacherId,
                                              @Param("courseId") Long courseId,
                                              @Param("experimentId") Long experimentId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              @Param("limit") Integer limit);

    @Select("""
        SELECT bucket AS name,
               CAST(COUNT(*) AS DECIMAL(20,2)) AS value
          FROM (
                SELECT CASE
                         WHEN rs.score < 60 THEN '0-59'
                         WHEN rs.score < 70 THEN '60-69'
                         WHEN rs.score < 80 THEN '70-79'
                         WHEN rs.score < 90 THEN '80-89'
                         ELSE '90-100'
                       END AS bucket
                  FROM t_report_score rs
                  JOIN t_report rp ON rp.id = rs.report_id AND rp.deleted = 0
                  JOIN t_experiment e ON e.id = rp.experiment_id AND e.deleted = 0
                  JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
                 WHERE rs.is_latest = 1
                   AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
                   AND (#{studentId} IS NULL OR rp.student_id = #{studentId})
                   AND (#{courseId} IS NULL OR c.id = #{courseId})
                   AND (#{experimentId} IS NULL OR e.id = #{experimentId})
                   AND (#{startTime} IS NULL OR rs.grade_time >= #{startTime})
                   AND (#{endTime} IS NULL OR rs.grade_time < #{endTime})
          ) x
         GROUP BY bucket
         ORDER BY FIELD(bucket, '0-59', '60-69', '70-79', '80-89', '90-100')
        """)
    List<PieItemVO> selectReportScoreDistribution(@Param("teacherId") Long teacherId,
                                                  @Param("studentId") Long studentId,
                                                  @Param("courseId") Long courseId,
                                                  @Param("experimentId") Long experimentId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    @Select("""
        SELECT stat_date,
               COUNT(DISTINCT student_id) AS value
          FROM (
                SELECT DATE_FORMAT(lr.last_time, '%Y-%m-%d') AS stat_date, lr.student_id, e.course_id, lr.experiment_id
                  FROM t_learning_record lr
                  JOIN t_experiment e ON e.id = lr.experiment_id AND e.deleted = 0
                 WHERE lr.deleted = 0 AND lr.last_time IS NOT NULL
                UNION ALL
                SELECT DATE_FORMAT(er.submit_time, '%Y-%m-%d') AS stat_date, er.student_id, e.course_id, er.experiment_id
                  FROM t_exam_record er
                  JOIN t_experiment e ON e.id = er.experiment_id AND e.deleted = 0
                 WHERE er.deleted = 0 AND er.submit_time IS NOT NULL
                UNION ALL
                SELECT DATE_FORMAT(rv.create_time, '%Y-%m-%d') AS stat_date, rv.student_id, e.course_id, rv.experiment_id
                  FROM t_reservation rv
                  JOIN t_experiment e ON e.id = rv.experiment_id AND e.deleted = 0
                 WHERE rv.deleted = 0 AND rv.create_time IS NOT NULL
                UNION ALL
                SELECT DATE_FORMAT(rp.submit_time, '%Y-%m-%d') AS stat_date, rp.student_id, e.course_id, rp.experiment_id
                  FROM t_report rp
                  JOIN t_experiment e ON e.id = rp.experiment_id AND e.deleted = 0
                 WHERE rp.deleted = 0 AND rp.submit_time IS NOT NULL
                UNION ALL
                SELECT DATE_FORMAT(rr.create_time, '%Y-%m-%d') AS stat_date, rr.student_id, e.course_id, rr.experiment_id
                  FROM t_recommend_record rr
                  JOIN t_experiment e ON e.id = rr.experiment_id AND e.deleted = 0
                 WHERE rr.clicked = 1 AND rr.create_time IS NOT NULL
          ) activity
          JOIN t_lab_course c ON c.id = activity.course_id AND c.deleted = 0
         WHERE (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{studentId} IS NULL OR activity.student_id = #{studentId})
           AND (#{courseId} IS NULL OR activity.course_id = #{courseId})
           AND (#{experimentId} IS NULL OR activity.experiment_id = #{experimentId})
           AND (#{startTime} IS NULL OR activity.stat_date >= DATE_FORMAT(#{startTime}, '%Y-%m-%d'))
           AND (#{endTime} IS NULL OR activity.stat_date < DATE_FORMAT(#{endTime}, '%Y-%m-%d'))
         GROUP BY stat_date
         ORDER BY stat_date ASC
        """)
    List<TrendItemVO> selectLearningActivityTrend(@Param("teacherId") Long teacherId,
                                                  @Param("studentId") Long studentId,
                                                  @Param("courseId") Long courseId,
                                                  @Param("experimentId") Long experimentId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
}
