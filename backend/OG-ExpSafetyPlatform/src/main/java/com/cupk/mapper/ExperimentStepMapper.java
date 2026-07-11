package com.cupk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cupk.pojo.ExperimentStep;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExperimentStepMapper extends BaseMapper<ExperimentStep> {
    @Delete("DELETE FROM t_experiment_step WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
