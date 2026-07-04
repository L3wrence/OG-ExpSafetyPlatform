package com.cupk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cupk.common.PageResult;
import com.cupk.dto.LoginDTO;
import com.cupk.dto.UserCreateDTO;
import com.cupk.dto.UserUpdateDTO;
import com.cupk.pojo.User;
import com.cupk.vo.LoginResultVO;
import com.cupk.vo.UserInfoVO;

import java.util.List;

public interface UserService extends IService<User> {
    Long createUser(UserCreateDTO dto);
    PageResult<UserInfoVO> listUsers(Integer pageNum, Integer pageSize, String keyword);
    LoginResultVO login(LoginDTO dto);
    void updateUser(Long id, UserUpdateDTO dto);
    void deleteUser(Long id);
    List<String> getUserPermissionCodes(Long userId);
}
