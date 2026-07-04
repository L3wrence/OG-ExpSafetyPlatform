package com.cupk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cupk.pojo.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    /** 查询学生是否通过了某个实验的安全考试 */
    @Select("""
        SELECT COUNT(*)
        FROM t_exam_record er
        JOIN t_exam_paper ep ON ep.id = er.paper_id AND ep.is_deleted = 0
        WHERE ep.course_id IN (
            SELECT course_id FROM t_experiment WHERE id = #{experimentId} AND deleted = 0
        )
          AND er.student_id = #{studentId}
          AND er.passed = 1
        """)
    Long countPassed(@Param("studentId") Long studentId, @Param("experimentId") Long experimentId);
}
