package com.cupk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cupk.common.PageResult;
import com.cupk.dto.ChangePasswordDTO;
import com.cupk.dto.LoginDTO;
import com.cupk.dto.ProfileUpdateDTO;
import com.cupk.dto.RegisterDTO;
import com.cupk.dto.UserCreateDTO;
import com.cupk.dto.UserUpdateDTO;
import com.cupk.pojo.User;
import com.cupk.vo.LoginResultVO;
import com.cupk.vo.UserInfoVO;

import java.util.List;

public interface UserService extends IService<User> {
    Long register(RegisterDTO dto);     //登录
    Long createUser(UserCreateDTO dto);     //创建用户
    PageResult<UserInfoVO> listUsers(Integer pageNum, Integer pageSize, String keyword);    //用户列表
    LoginResultVO login(LoginDTO dto);  //注册
    void logout(String token);  //登录日志
    void changePassword(ChangePasswordDTO dto);     //修改密码
    UserInfoVO currentUser();   //当前用户
    UserInfoVO updateProfile(ProfileUpdateDTO dto);     //修改资料
    void updateUser(Long id, UserUpdateDTO dto);    //修改用户
    void deleteUser(Long id);   //删除用户
    List<String> getUserPermissionCodes(Long userId);   //
}
