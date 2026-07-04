package com.cupk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cupk.pojo.LearningRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {

    @Select("""
        SELECT COALESCE(ROUND(AVG(x.student_progress), 2), 0)
        FROM (
            SELECT cs.student_id,
                   100.0 * SUM(CASE WHEN lr.finish_flag = 1 THEN 1 ELSE 0 END)
                   / NULLIF(COUNT(r.id), 0) AS student_progress
            FROM t_course_student cs
            JOIN t_experiment e ON e.course_id = cs.course_id AND e.deleted = 0
            JOIN t_resource r ON r.experiment_id = e.id AND r.required_flag = 1 AND r.deleted = 0
            LEFT JOIN t_learning_record lr ON lr.resource_id = r.id
                 AND lr.student_id = cs.student_id AND lr.deleted = 0
            WHERE cs.course_id = #{courseId} AND cs.deleted = 0
            GROUP BY cs.student_id
        ) x
        """)
    BigDecimal selectCourseAverageProgress(@Param("courseId") Long courseId);
}
