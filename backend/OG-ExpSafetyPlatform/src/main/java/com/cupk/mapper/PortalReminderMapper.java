package com.cupk.mapper;

import com.cupk.vo.ReminderTargetVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PortalReminderMapper {
    @Select("""
        SELECT cs.student_id AS userId,
               ep.id AS bizId,
               CONCAT('考试即将截止：', ep.title) AS title,
               CONCAT('正式安全考试将于 ', DATE_FORMAT(ep.end_time, '%Y-%m-%d %H:%i'), ' 截止，请及时完成。') AS content,
               CONCAT('/classrooms/', ep.course_id, '/learn?module=exam') AS path,
               ep.end_time AS eventTime
          FROM t_exam_paper ep
          JOIN t_course_student cs ON cs.course_id = ep.course_id
         WHERE ep.deleted = 0
           AND ep.status = 'PUBLISHED'
           AND ep.end_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 24 HOUR)
           AND cs.status = 1 AND cs.deleted = 0
           AND NOT EXISTS (
               SELECT 1 FROM t_exam_record er
                WHERE er.paper_id = ep.id
                  AND er.student_id = cs.student_id
                  AND er.deleted = 0
                  AND er.status = 'GRADED'
                  AND er.passed = 1
           )
        """)
    List<ReminderTargetVO> examDeadlineTargets();

    @Select("""
        SELECT rv.student_id AS userId,
               rv.id AS bizId,
               CONCAT('预约即将开始：', COALESCE(e.exp_name, '实验')) AS title,
               CONCAT('你的实验预约将于 ', DATE_FORMAT(TIMESTAMP(COALESCE(ts.slot_date, ts.date), ts.start_time), '%Y-%m-%d %H:%i'), ' 开始，请按要求到场。') AS content,
               COALESCE(CONCAT('/classrooms/', e.course_id, '/learn?module=reservation'), '/classrooms') AS path,
               TIMESTAMP(COALESCE(ts.slot_date, ts.date), ts.start_time) AS eventTime
          FROM t_reservation rv
          JOIN t_lab_time_slot ts ON ts.id = rv.time_slot_id
          LEFT JOIN t_experiment e ON e.id = rv.experiment_id AND e.deleted = 0
         WHERE rv.deleted = 0
           AND rv.status = 'APPROVED'
           AND TIMESTAMP(COALESCE(ts.slot_date, ts.date), ts.start_time)
               BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 24 HOUR)
        """)
    List<ReminderTargetVO> reservationStartTargets();

    @Select("""
        SELECT cs.student_id AS userId,
               lt.id AS bizId,
               CONCAT('报告即将截止：', COALESCE(e.exp_name, lt.task_name)) AS title,
               CONCAT('报告相关任务将于 ', DATE_FORMAT(lt.deadline, '%Y-%m-%d %H:%i'), ' 截止，请及时提交。') AS content,
               CONCAT('/classrooms/', lt.course_id, '/learn?module=reports') AS path,
               lt.deadline AS eventTime
          FROM t_learning_task lt
          JOIN t_course_student cs ON cs.course_id = lt.course_id
          LEFT JOIN t_experiment e ON e.id = lt.experiment_id AND e.deleted = 0
         WHERE lt.deleted = 0
           AND lt.status = 1
           AND lt.deadline BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 24 HOUR)
           AND cs.status = 1 AND cs.deleted = 0
           AND (lt.task_type IN ('REPORT', 'SUBMIT_REPORT')
                OR lt.task_name LIKE '%报告%')
           AND NOT EXISTS (
               SELECT 1 FROM t_learning_task_record ltr
                WHERE ltr.task_id = lt.id
                  AND ltr.student_id = cs.student_id
                  AND ltr.deleted = 0
                  AND ltr.status = 'COMPLETED'
           )
        """)
    List<ReminderTargetVO> reportDeadlineTargets();

    @Select("""
        SELECT ea.student_id AS userId,
               ea.id AS bizId,
               CONCAT('准入资格即将过期：', COALESCE(e.exp_name, '实验')) AS title,
               CONCAT('你的实验准入资格将于 ', DATE_FORMAT(ea.valid_until, '%Y-%m-%d %H:%i'), ' 过期，如需参加实验请尽快完成预约。') AS content,
               CONCAT('/classrooms/', e.course_id, '/learn?module=reservation&experimentId=', e.id) AS path,
               ea.valid_until AS eventTime
          FROM t_experiment_admission ea
          JOIN t_experiment e ON e.id = ea.experiment_id AND e.deleted = 0
         WHERE ea.deleted = 0
           AND ea.status = 'VALID'
           AND ea.valid_until BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 24 HOUR)
        """)
    List<ReminderTargetVO> admissionExpiringTargets();
}
