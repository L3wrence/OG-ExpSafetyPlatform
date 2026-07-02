package com.cupk.amazingstudy.service;



import com.baomidou.mybatisplus.extension.service.IService;
import com.cupk.amazingstudy.common.CommonResult;
import com.cupk.amazingstudy.dto.LoginDTO;
import com.cupk.amazingstudy.dto.UserCreateDTO;
import com.cupk.amazingstudy.pojo.User;

public interface UserService extends IService<User> {
    CommonResult createUser(UserCreateDTO dto);

    CommonResult listUsers(Integer pageNum, Integer pageSize, String keyword);

    CommonResult login(LoginDTO dto);
}