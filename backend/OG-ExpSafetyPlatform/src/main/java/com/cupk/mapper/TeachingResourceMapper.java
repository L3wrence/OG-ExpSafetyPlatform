package com.cupk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cupk.pojo.TeachingResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TeachingResourceMapper extends BaseMapper<TeachingResource> {
    @Update("UPDATE t_resource SET view_count = COALESCE(view_count, 0) + 1, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int incrementViewCount(@Param("id") Long id);
}
