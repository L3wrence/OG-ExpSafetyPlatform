package com.cupk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cupk.common.Result;
import com.cupk.dto.LoginDTO;
import com.cupk.dto.UserCreateDTO;
import com.cupk.dto.UserUpdateDTO;
import com.cupk.pojo.User;

import java.util.List;

public interface UserService extends IService<User> {
    Result<?> createUser(UserCreateDTO dto);

    Result<?> listUsers(Integer pageNum, Integer pageSize, String keyword);

    Result<?> login(LoginDTO dto);

    Result<?> updateUser(Long id, UserUpdateDTO dto);

    Result<?> deleteUser(Long id);

    List<String> getUserPermissionCodes(Long userId);

    List<String> getUserRoleCodes(Long userId);
}
