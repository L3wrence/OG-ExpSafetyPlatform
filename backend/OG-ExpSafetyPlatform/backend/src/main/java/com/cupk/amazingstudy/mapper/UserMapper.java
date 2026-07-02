package com.cupk.amazingstudy.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cupk.amazingstudy.pojo.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}