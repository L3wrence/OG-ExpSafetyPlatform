package com.cupk.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BusinessReferenceMapper {
    @Select("""
        SELECT
          (SELECT COUNT(*) FROM t_exam_paper ep JOIN t_exam_record er ON er.paper_id = ep.id
             WHERE ep.experiment_id = #{experimentId} AND ep.deleted = 0 AND er.deleted = 0)
        + (SELECT COUNT(*) FROM t_reservation r
             WHERE r.experiment_id = #{experimentId} AND r.deleted = 0)
        + (SELECT COUNT(*) FROM t_report rp
             WHERE rp.experiment_id = #{experimentId} AND rp.deleted = 0)
        """)
    Long countExperimentHistory(@Param("experimentId") Long experimentId);
}
