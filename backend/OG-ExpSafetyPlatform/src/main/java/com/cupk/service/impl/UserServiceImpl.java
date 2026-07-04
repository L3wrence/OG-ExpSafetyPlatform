package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.common.PageResult;
import com.cupk.common.Result;
import com.cupk.dto.LoginDTO;
import com.cupk.dto.UserCreateDTO;
import com.cupk.dto.UserUpdateDTO;
import com.cupk.pojo.*;
import com.cupk.exception.BusinessException;
import com.cupk.mapper.UserMapper;
import com.cupk.service.*;
import com.cupk.vo.LoginResultVO;
import com.cupk.vo.MenuVO;
import com.cupk.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现（含认证、RBAC权限）
 * 成员A交付
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleService roleService;

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
    public Result<?> createUser(UserCreateDTO dto) {
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

        return Result.success("用户创建成功");
    }

    @Override
    public Result<?> listUsers(Integer pageNum, Integer pageSize, String keyword) {
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
        return Result.success(pageResult);
    }

    @Override
    public Result<?> login(LoginDTO dto) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!user.getPassword().equals(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes()))) {
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
        result.setMenus(getUserMenus(user.getId()));
        result.setPermissions(getUserPermissionCodes(user.getId()));
        return Result.success(result);
    }

    @Override
    public Result<?> updateUser(Long id, UserUpdateDTO dto) {
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        this.updateById(user);

        if (dto.getRoleId() != null) {
            userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
            UserRole userRole = new UserRole();
            userRole.setUserId(id);
            userRole.setRoleId(dto.getRoleId());
            userRoleService.save(userRole);
        }

        return Result.success("用户更新成功");
    }

    @Override
    public Result<?> deleteUser(Long id) {
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 删除用户
        this.removeById(id);
        // 删除角色关联
        userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        // 删除token
        tokenService.remove(new LambdaQueryWrapper<Token>().eq(Token::getUserId, id));

        return Result.success("用户删除成功");
    }

    // 获取用户角色编码列表
    public List<String> getUserRoleCodes(Long userId) {
        List<Long> roleIds = userRoleService.list(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) return new ArrayList<>();

        return roleService.list(
                        new LambdaQueryWrapper<Role>().in(Role::getId, roleIds))
                .stream().map(Role::getRoleCode).collect(Collectors.toList());
    }

    // 获取用户权限编码列表
    public List<String> getUserPermissionCodes(Long userId) {
        List<Long> roleIds = userRoleService.list(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) return new ArrayList<>();

        List<Long> permIds = rolePermissionService.list(
                        new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds))
                .stream().map(RolePermission::getPermissionId).distinct().collect(Collectors.toList());
        if (permIds.isEmpty()) return new ArrayList<>();

        return permissionService.list(
                        new LambdaQueryWrapper<Permission>().in(Permission::getId, permIds).eq(Permission::getType, 2))
                .stream().map(Permission::getCode).collect(Collectors.toList());
    }

    // 获取用户菜单树
    private List<MenuVO> getUserMenus(Long userId) {
        List<Long> roleIds = userRoleService.list(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) return new ArrayList<>();

        List<Long> permIds = rolePermissionService.list(
                        new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds))
                .stream().map(RolePermission::getPermissionId).distinct().collect(Collectors.toList());
        if (permIds.isEmpty()) return new ArrayList<>();

        List<Permission> menus = permissionService.list(
                new LambdaQueryWrapper<Permission>()
                        .in(Permission::getId, permIds)
                        .eq(Permission::getType, 1)
                        .orderByAsc(Permission::getSort));

        return buildMenuTree(menus, 0L);
    }

    private List<MenuVO> buildMenuTree(List<Permission> menus, Long parentId) {
        List<MenuVO> result = new ArrayList<>();
        for (Permission p : menus) {
            if (p.getParentId().equals(parentId)) {
                MenuVO vo = new MenuVO();
                vo.setId(p.getId());
                vo.setName(p.getName());
                vo.setCode(p.getCode());
                vo.setPath(p.getPath());
                vo.setIcon(p.getIcon());
                vo.setChildren(buildMenuTree(menus, p.getId()));
                result.add(vo);
            }
        }
        return result;
    }
}
