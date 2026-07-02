package com.cupk.amazingstudy.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.amazingstudy.common.CommonResult;
import com.cupk.amazingstudy.common.PageResult;
import com.cupk.amazingstudy.dto.LoginDTO;
import com.cupk.amazingstudy.dto.UserCreateDTO;
import com.cupk.amazingstudy.pojo.User;
import com.cupk.amazingstudy.pojo.UserRole;
import com.cupk.amazingstudy.exception.BusinessException;
import com.cupk.amazingstudy.mapper.UserMapper;
import com.cupk.amazingstudy.service.TokenService;
import com.cupk.amazingstudy.service.UserRoleService;
import com.cupk.amazingstudy.service.UserService;
import com.cupk.amazingstudy.vo.LoginResultVO;
import com.cupk.amazingstudy.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private TokenService tokenService;

    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    @Override
    public CommonResult createUser(UserCreateDTO dto) {
        User existUser = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (existUser != null) {
            throw new BusinessException(400, "该学号/工号已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(md5Hex(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        this.save(user);

        if (dto.getRoleId() != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(dto.getRoleId());
            userRoleService.save(userRole);
        }

        return CommonResult.success("用户创建成功");
    }
    @Override
    public CommonResult listUsers(Integer pageNum, Integer pageSize, String keyword) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword).or().like(User::getRealName, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        this.page(page, wrapper);

        PageResult<User> pageResult = new PageResult<>();
        pageResult.setRecords(page.getRecords());
        pageResult.setTotal(page.getTotal());
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);
        return CommonResult.success(pageResult);
    }

    @Override
    public CommonResult login(LoginDTO dto) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!user.getPassword().equals(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes()))){
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 生成token
        String token = tokenService.createToken(user.getId());

        // 组装返回
        UserInfoVO userInfo = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfo);

        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        result.setUserInfo(userInfo);
        result.setMenus(new ArrayList<>());      // 菜单暂时空
        result.setPermissions(new ArrayList<>()); // 权限暂时空
        return CommonResult.success(result);
    }
}
