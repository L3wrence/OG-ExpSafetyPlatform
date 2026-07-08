package com.cupk.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PortalMapper {
    @Select("""
        SELECT COUNT(*) FROM t_course_student cs
         WHERE cs.student_id = #{studentId} AND cs.status = 1 AND cs.deleted = 0
        """)
    Integer countStudentCourses(@Param("studentId") Long studentId);

    @Select("""
        SELECT COUNT(DISTINCT e.id)
          FROM t_course_student cs
          JOIN t_experiment e ON e.course_id = cs.course_id AND e.deleted = 0 AND e.status = 1
         WHERE cs.student_id = #{studentId} AND cs.status = 1 AND cs.deleted = 0
        """)
    Integer countStudentExperiments(@Param("studentId") Long studentId);

    @Select("""
        SELECT COUNT(*)
          FROM t_exam_paper ep
          JOIN t_course_student cs ON cs.course_id = ep.course_id
         WHERE cs.student_id = #{studentId}
           AND cs.status = 1 AND cs.deleted = 0
           AND ep.status = 'PUBLISHED' AND ep.deleted = 0
           AND NOT EXISTS (
               SELECT 1 FROM t_exam_record er
                WHERE er.paper_id = ep.id AND er.student_id = #{studentId}
                  AND er.deleted = 0 AND er.status = 'GRADED' AND er.passed = 1
           )
        """)
    Integer countStudentPendingExams(@Param("studentId") Long studentId);

    @Select("""
        SELECT COUNT(*)
          FROM t_learning_task lt
          JOIN t_experiment e ON e.id = lt.experiment_id AND e.deleted = 0 AND e.status = 1
          JOIN t_course_student cs ON cs.course_id = lt.course_id
         WHERE cs.student_id = #{studentId}
           AND cs.status = 1 AND cs.deleted = 0
           AND lt.status = 1 AND lt.required_flag = 1 AND lt.deleted = 0
           AND (lt.open_time IS NULL OR lt.open_time <= NOW())
           AND NOT EXISTS (
               SELECT 1 FROM t_learning_task_record ltr
                WHERE ltr.task_id = lt.id AND ltr.student_id = #{studentId}
                  AND ltr.status = 'COMPLETED' AND ltr.deleted = 0
           )
        """)
    Integer countStudentPendingLearningTasks(@Param("studentId") Long studentId);

    @Select("""
        SELECT COUNT(*)
          FROM t_reservation
         WHERE student_id = #{studentId} AND deleted = 0
           AND status IN ('PENDING', 'APPROVED')
        """)
    Integer countStudentActiveReservations(@Param("studentId") Long studentId);

    @Select("""
        SELECT COUNT(*)
          FROM t_report
         WHERE student_id = #{studentId} AND deleted = 0
           AND status IN ('DRAFT', 'RETURNED')
        """)
    Integer countStudentPendingReports(@Param("studentId") Long studentId);

    @Select("""
        SELECT COALESCE(ROUND(100 * SUM(CASE WHEN passed = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 0), 0)
          FROM t_exam_record
         WHERE student_id = #{studentId} AND deleted = 0 AND status = 'GRADED'
        """)
    Integer studentExamPassRate(@Param("studentId") Long studentId);

    @Select("""
        SELECT lt.id,
               CONCAT('学习任务：', lt.task_name) AS title,
               lt.deadline AS time,
               'learning' AS type,
               CONCAT('/student/learning/', lt.course_id, '?experimentId=', lt.experiment_id) AS path,
               CASE
                   WHEN lt.deadline IS NOT NULL AND lt.deadline < NOW() THEN 'OVERDUE'
                   WHEN lt.deadline IS NOT NULL AND lt.deadline <= DATE_ADD(NOW(), INTERVAL 2 DAY) THEN 'DUE_SOON'
                   ELSE 'TODO'
               END AS status
          FROM t_learning_task lt
          JOIN t_experiment e ON e.id = lt.experiment_id AND e.deleted = 0 AND e.status = 1
          JOIN t_course_student cs ON cs.course_id = lt.course_id
         WHERE cs.student_id = #{studentId}
           AND cs.status = 1 AND cs.deleted = 0
           AND lt.status = 1 AND lt.required_flag = 1 AND lt.deleted = 0
           AND (lt.open_time IS NULL OR lt.open_time <= NOW())
           AND NOT EXISTS (
               SELECT 1 FROM t_learning_task_record ltr
                WHERE ltr.task_id = lt.id AND ltr.student_id = #{studentId}
                  AND ltr.status = 'COMPLETED' AND ltr.deleted = 0
           )
         ORDER BY CASE WHEN lt.deadline IS NULL THEN 1 ELSE 0 END, lt.deadline ASC, lt.sort ASC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> studentLearningTodos(@Param("studentId") Long studentId, @Param("limit") Integer limit);

    @Select("""
        SELECT ep.id, ep.title, ep.end_time AS time, 'exam' AS type, '/student/exams' AS path
          FROM t_exam_paper ep
          JOIN t_course_student cs ON cs.course_id = ep.course_id
         WHERE cs.student_id = #{studentId}
           AND cs.status = 1 AND cs.deleted = 0
           AND ep.status = 'PUBLISHED' AND ep.deleted = 0
           AND NOT EXISTS (
               SELECT 1 FROM t_exam_record er
                WHERE er.paper_id = ep.id AND er.student_id = #{studentId}
                  AND er.deleted = 0 AND er.status = 'GRADED' AND er.passed = 1
           )
         ORDER BY ep.end_time ASC, ep.id DESC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> studentExamTodos(@Param("studentId") Long studentId, @Param("limit") Integer limit);

    @Select("""
        SELECT rv.id, CONCAT('预约状态：', rv.status) AS title, rv.create_time AS time,
               'reservation' AS type, '/student/reserve' AS path
          FROM t_reservation rv
         WHERE rv.student_id = #{studentId} AND rv.deleted = 0
         ORDER BY rv.create_time DESC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> studentReservationTodos(@Param("studentId") Long studentId, @Param("limit") Integer limit);

    @Select("""
        SELECT rp.id, rp.title, rp.update_time AS time, 'report' AS type, '/student/grades' AS path
          FROM t_report rp
         WHERE rp.student_id = #{studentId} AND rp.deleted = 0
           AND rp.status IN ('DRAFT', 'RETURNED')
         ORDER BY rp.update_time DESC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> studentReportTodos(@Param("studentId") Long studentId, @Param("limit") Integer limit);

    @Select("""
        SELECT ea.id,
               CONCAT('准入即将到期：', e.exp_name) AS title,
               ea.valid_until AS time,
               'admission' AS type,
               CONCAT('/student/learning/', e.course_id, '?experimentId=', e.id) AS path,
               ea.status
          FROM t_experiment_admission ea
          JOIN t_experiment e ON e.id = ea.experiment_id AND e.deleted = 0
         WHERE ea.student_id = #{studentId}
           AND ea.deleted = 0
           AND ea.status = 'VALID'
           AND ea.valid_until IS NOT NULL
           AND ea.valid_until BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 14 DAY)
         ORDER BY ea.valid_until ASC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> studentAdmissionTodos(@Param("studentId") Long studentId, @Param("limit") Integer limit);

    @Select("""
        SELECT COUNT(*)
          FROM t_reservation rv
          JOIN t_experiment e ON e.id = rv.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE rv.deleted = 0 AND rv.status = 'PENDING' AND c.teacher_id = #{teacherId}
        """)
    Integer countTeacherPendingReservations(@Param("teacherId") Long teacherId);

    @Select("""
        SELECT COUNT(*)
          FROM t_report rp
          JOIN t_experiment e ON e.id = rp.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE rp.deleted = 0 AND rp.status = 'SUBMITTED' AND c.teacher_id = #{teacherId}
        """)
    Integer countTeacherPendingReports(@Param("teacherId") Long teacherId);

    @Select("""
        SELECT COUNT(DISTINCT cs.student_id)
          FROM t_course_student cs
          JOIN t_lab_course c ON c.id = cs.course_id AND c.deleted = 0
         WHERE cs.deleted = 0 AND cs.status = 1 AND c.teacher_id = #{teacherId}
        """)
    Integer countTeacherStudents(@Param("teacherId") Long teacherId);

    @Select("""
        SELECT COUNT(*)
          FROM t_exam_paper
         WHERE teacher_id = #{teacherId} AND deleted = 0 AND status = 'DRAFT'
        """)
    Integer countTeacherDraftExams(@Param("teacherId") Long teacherId);

    @Select("""
        SELECT COALESCE(COUNT(*), 0)
          FROM (
                SELECT er.experiment_id
                  FROM t_exam_record er
                  JOIN t_experiment e ON e.id = er.experiment_id AND e.deleted = 0
                  JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
                 WHERE c.teacher_id = #{teacherId} AND er.deleted = 0
                 GROUP BY er.experiment_id
                HAVING 100 * SUM(CASE WHEN er.passed = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0) < 60
          ) x
        """)
    Integer countTeacherLowPassWarnings(@Param("teacherId") Long teacherId);

    @Select("""
        SELECT rv.id, CONCAT('预约待审核：', COALESCE(e.exp_name, '实验')) AS title, rv.create_time AS time,
               'reservation' AS type, '/teacher/reservations' AS path
          FROM t_reservation rv
          JOIN t_experiment e ON e.id = rv.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE rv.deleted = 0 AND rv.status = 'PENDING' AND c.teacher_id = #{teacherId}
         ORDER BY rv.create_time ASC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> teacherReservationTodos(@Param("teacherId") Long teacherId, @Param("limit") Integer limit);

    @Select("""
        SELECT rp.id, CONCAT('报告待批改：', rp.title) AS title, rp.latest_submit_time AS time,
               'report' AS type, '/teacher/reports' AS path
          FROM t_report rp
          JOIN t_experiment e ON e.id = rp.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE rp.deleted = 0 AND rp.status = 'SUBMITTED' AND c.teacher_id = #{teacherId}
         ORDER BY rp.latest_submit_time ASC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> teacherReportTodos(@Param("teacherId") Long teacherId, @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM t_lab_time_slot WHERE slot_date = CURRENT_DATE()")
    Integer countTodaySlots();

    @Select("""
        SELECT COUNT(*)
          FROM t_reservation rv
          JOIN t_lab_time_slot ts ON ts.id = rv.time_slot_id
         WHERE ts.slot_date = CURRENT_DATE() AND rv.deleted = 0
        """)
    Integer countTodayReservations();

    @Select("""
        SELECT COALESCE(ROUND(100 * SUM(booked_count) / NULLIF(SUM(capacity), 0), 0), 0)
          FROM t_lab_time_slot
         WHERE slot_date = CURRENT_DATE()
        """)
    Integer todayCapacityUsage();

    @Select("SELECT COUNT(*) FROM t_user")
    Integer countUsers();

    @Select("SELECT COUNT(*) FROM t_lab_course WHERE deleted = 0")
    Integer countCourses();

    @Select("SELECT COUNT(*) FROM t_experiment WHERE deleted = 0")
    Integer countExperiments();

    @Select("SELECT COUNT(*) FROM t_operation_log WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 DAY)")
    Integer countTodayLogs();

    @Select("""
        SELECT id, title, content, priority, publish_time AS time, 'notice' AS type
          FROM t_portal_notice
         WHERE deleted = 0 AND status = 1
           AND publish_time <= NOW()
           AND (expire_time IS NULL OR expire_time >= NOW())
           AND (target_role IS NULL OR target_role = '' OR target_role = 'ALL' OR target_role = #{roleCode})
         ORDER BY FIELD(priority, 'HIGH', 'MEDIUM', 'LOW'), publish_time DESC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> notices(@Param("roleCode") String roleCode, @Param("limit") Integer limit);

    @Select("""
        SELECT id, title, content, biz_type, biz_id, path, read_flag, create_time AS time, 'message' AS type
          FROM t_portal_message
         WHERE deleted = 0 AND user_id = #{userId}
         ORDER BY read_flag ASC, create_time DESC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> messages(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM t_portal_message WHERE deleted = 0 AND user_id = #{userId} AND read_flag = 0")
    Integer unreadMessages(@Param("userId") Long userId);

    @Select("""
        SELECT id, title, path, module AS type, last_visit_time AS time, visit_count AS value
          FROM t_recent_visit
         WHERE user_id = #{userId}
         ORDER BY last_visit_time DESC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> recentVisits(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("""
        SELECT id, title, path, icon AS type, sort AS value
          FROM t_user_shortcut
         WHERE user_id = #{userId}
         ORDER BY sort ASC, id ASC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> shortcuts(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("""
        SELECT id, title, content AS description, 'notice' AS type, '/home' AS path
          FROM t_portal_notice
         WHERE deleted = 0 AND status = 1
           AND (target_role IS NULL OR target_role = '' OR target_role = 'ALL' OR target_role = #{roleCode})
           AND title LIKE CONCAT('%', #{keyword}, '%')
         ORDER BY publish_time DESC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> searchNotices(@Param("keyword") String keyword,
                                            @Param("roleCode") String roleCode,
                                            @Param("limit") Integer limit);

    @Select("""
        SELECT c.id, c.course_name AS title, c.description, 'course' AS type,
               CASE WHEN #{roleCode} = 'TEACHER' THEN '/teacher/courses' ELSE '/student/courses' END AS path
          FROM t_lab_course c
         WHERE c.deleted = 0
           AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{studentId} IS NULL OR EXISTS (
                 SELECT 1 FROM t_course_student cs
                  WHERE cs.course_id = c.id AND cs.student_id = #{studentId}
                    AND cs.status = 1 AND cs.deleted = 0))
           AND (c.course_name LIKE CONCAT('%', #{keyword}, '%') OR c.course_code LIKE CONCAT('%', #{keyword}, '%'))
         ORDER BY c.update_time DESC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> searchCourses(@Param("keyword") String keyword,
                                            @Param("teacherId") Long teacherId,
                                            @Param("studentId") Long studentId,
                                            @Param("roleCode") String roleCode,
                                            @Param("limit") Integer limit);

    @Select("""
        SELECT e.id, e.exp_name AS title, e.objective AS description, 'experiment' AS type,
               CASE WHEN #{roleCode} = 'TEACHER' THEN '/teacher/courses' ELSE '/student/courses' END AS path
          FROM t_experiment e
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE e.deleted = 0
           AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{studentId} IS NULL OR EXISTS (
                 SELECT 1 FROM t_course_student cs
                  WHERE cs.course_id = c.id AND cs.student_id = #{studentId}
                    AND cs.status = 1 AND cs.deleted = 0))
           AND (e.exp_name LIKE CONCAT('%', #{keyword}, '%') OR e.exp_code LIKE CONCAT('%', #{keyword}, '%'))
         ORDER BY e.update_time DESC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> searchExperiments(@Param("keyword") String keyword,
                                                @Param("teacherId") Long teacherId,
                                                @Param("studentId") Long studentId,
                                                @Param("roleCode") String roleCode,
                                                @Param("limit") Integer limit);

    @Select("""
        SELECT r.id, r.title, r.resource_type AS description, 'resource' AS type,
               CASE WHEN #{roleCode} = 'TEACHER' THEN '/teacher/resources' ELSE '/student/courses' END AS path
          FROM t_resource r
          JOIN t_experiment e ON e.id = r.experiment_id AND e.deleted = 0
          JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE r.deleted = 0 AND r.status = 1
           AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{studentId} IS NULL OR EXISTS (
                 SELECT 1 FROM t_course_student cs
                  WHERE cs.course_id = c.id AND cs.student_id = #{studentId}
                    AND cs.status = 1 AND cs.deleted = 0))
           AND r.title LIKE CONCAT('%', #{keyword}, '%')
         ORDER BY r.update_time DESC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> searchResources(@Param("keyword") String keyword,
                                              @Param("teacherId") Long teacherId,
                                              @Param("studentId") Long studentId,
                                              @Param("roleCode") String roleCode,
                                              @Param("limit") Integer limit);

    @Select("""
        SELECT sk.id, sk.knowledge_point AS title, sk.content AS description, 'knowledge' AS type,
               CASE WHEN #{roleCode} = 'TEACHER' THEN '/teacher/knowledge' ELSE '/student/knowledge' END AS path
          FROM t_safety_knowledge sk
          LEFT JOIN t_experiment e ON e.id = sk.experiment_id AND e.deleted = 0
          LEFT JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE sk.deleted = 0 AND sk.status = 1
           AND (#{teacherId} IS NULL OR c.teacher_id = #{teacherId})
           AND (#{studentId} IS NULL OR sk.experiment_id IS NULL OR EXISTS (
                 SELECT 1 FROM t_course_student cs
                  WHERE cs.course_id = c.id AND cs.student_id = #{studentId}
                    AND cs.status = 1 AND cs.deleted = 0))
           AND (sk.knowledge_point LIKE CONCAT('%', #{keyword}, '%') OR sk.content LIKE CONCAT('%', #{keyword}, '%'))
         ORDER BY sk.update_time DESC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> searchKnowledge(@Param("keyword") String keyword,
                                              @Param("teacherId") Long teacherId,
                                              @Param("studentId") Long studentId,
                                              @Param("roleCode") String roleCode,
                                              @Param("limit") Integer limit);

    @Select("""
        SELECT ep.id, ep.title, 'exam' AS type,
               DATE_FORMAT(ep.start_time, '%Y-%m-%d %H:%i:%s') AS start_time,
               DATE_FORMAT(ep.end_time, '%Y-%m-%d %H:%i:%s') AS end_time,
               ep.status, '/student/exams' AS path
          FROM t_exam_paper ep
          JOIN t_course_student cs ON cs.course_id = ep.course_id
         WHERE #{roleCode} = 'USER'
           AND cs.student_id = #{userId} AND cs.status = 1 AND cs.deleted = 0
           AND ep.status = 'PUBLISHED' AND ep.deleted = 0
           AND ep.end_time IS NOT NULL
         ORDER BY ep.end_time ASC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> studentCalendar(@Param("userId") Long userId,
                                              @Param("roleCode") String roleCode,
                                              @Param("limit") Integer limit);

    @Select("""
        SELECT ts.id, COALESCE(e.exp_name, '实验预约') AS title, 'reservation' AS type,
               CONCAT(DATE_FORMAT(ts.slot_date, '%Y-%m-%d'), ' ', CAST(ts.start_time AS CHAR)) AS start_time,
               CONCAT(DATE_FORMAT(ts.slot_date, '%Y-%m-%d'), ' ', CAST(ts.end_time AS CHAR)) AS end_time,
               ts.status, CASE WHEN #{roleCode} = 'TEACHER' THEN '/teacher/reservations' ELSE '/student/reserve' END AS path
          FROM t_lab_time_slot ts
          LEFT JOIN t_experiment e ON e.id = ts.experiment_id AND e.deleted = 0
          LEFT JOIN t_lab_course c ON c.id = e.course_id AND c.deleted = 0
         WHERE ts.slot_date >= CURRENT_DATE()
           AND ((#{roleCode} = 'TEACHER' AND c.teacher_id = #{userId})
                OR (#{roleCode} = 'USER' AND EXISTS (
                     SELECT 1 FROM t_reservation rv
                      WHERE rv.time_slot_id = ts.id AND rv.student_id = #{userId}
                        AND rv.deleted = 0 AND rv.status IN ('PENDING', 'APPROVED'))))
         ORDER BY ts.slot_date ASC, ts.start_time ASC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> reservationCalendar(@Param("userId") Long userId,
                                                  @Param("roleCode") String roleCode,
                                                  @Param("limit") Integer limit);

    @Select("""
        SELECT id, CONCAT(module, '：', action) AS title, 'log' AS type,
               content AS description, result AS status, create_time AS time, '/admin/logs' AS path
          FROM t_operation_log
         ORDER BY create_time DESC
         LIMIT #{limit}
        """)
    List<Map<String, Object>> recentLogs(@Param("limit") Integer limit);
}
