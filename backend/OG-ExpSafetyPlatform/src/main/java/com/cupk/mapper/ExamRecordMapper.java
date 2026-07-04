package com.cupk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cupk.pojo.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {
    @Select("""
        SELECT COUNT(*)
        FROM t_exam_record er
        JOIN t_exam_paper ep ON ep.id = er.paper_id AND ep.deleted = 0
        WHERE ep.experiment_id = #{experimentId}
          AND er.student_id = #{studentId}
          AND er.passed = 1
          AND er.deleted = 0
        """)
    Long countPassed(@Param("studentId") Long studentId, @Param("experimentId") Long experimentId);
}
