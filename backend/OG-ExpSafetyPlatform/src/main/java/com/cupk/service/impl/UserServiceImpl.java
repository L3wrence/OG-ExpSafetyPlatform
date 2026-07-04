package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.common.PageResult;
import com.cupk.dto.LoginDTO;
import com.cupk.dto.UserCreateDTO;
import com.cupk.dto.UserUpdateDTO;
import com.cupk.exception.BusinessException;
import com.cupk.mapper.UserMapper;
import com.cupk.pojo.Permission;
import com.cupk.pojo.Role;
import com.cupk.pojo.RolePermission;
import com.cupk.pojo.Token;
import com.cupk.pojo.User;
import com.cupk.pojo.UserRole;
import com.cupk.service.PermissionService;
import com.cupk.service.RolePermissionService;
import com.cupk.service.RoleService;
import com.cupk.service.TokenService;
import com.cupk.service.UserRoleService;
import com.cupk.service.UserService;
import com.cupk.vo.LoginResultVO;
import com.cupk.vo.MenuVO;
import com.cupk.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserRoleService userRoleService;
    private final TokenService tokenService;
    private final RolePermissionService rolePermissionService;
    private final PermissionService permissionService;
    private final RoleService roleService;

    public UserServiceImpl(UserRoleService userRoleService, TokenService tokenService,
                           RolePermissionService rolePermissionService, PermissionService permissionService,
                           RoleService roleService) {
        this.userRoleService = userRoleService;
        this.tokenService = tokenService;
        this.rolePermissionService = rolePermissionService;
        this.permissionService = permissionService;
        this.roleService = roleService;
    }

    @Override
    @Transactional
    public Long createUser(UserCreateDTO dto) {
        User existUser = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (existUser != null) {
            throw new BusinessException(400, "该学号/工号已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(md5(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        save(user);
        saveUserRole(user.getId(), dto.getRoleId());
        return user.getId();
    }

    @Override
    public PageResult<UserInfoVO> listUsers(Integer pageNum, Integer pageSize, String keyword) {
        Page<User> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                .like(User::getUsername, keyword)
                .or().like(User::getRealName, keyword));
        wrapper.orderByDesc(User::getCreateTime);
        page(page, wrapper);
        Map<Long, List<String>> roleCodes = roleCodesByUserId(page.getRecords().stream().map(User::getId).toList());
        List<UserInfoVO> records = page.getRecords().stream()
                .map(user -> toUserInfo(user, roleCodes.getOrDefault(user.getId(), List.of())))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public LoginResultVO login(LoginDTO dto) {
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || Integer.valueOf(0).equals(user.getStatus()) || !md5(dto.getPassword()).equals(user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = tokenService.createToken(user.getId());

        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        result.setUserInfo(toUserInfo(user));
        result.setMenus(getUserMenus(user.getId()));
        result.setPermissions(getUserPermissionCodes(user.getId()));
        return result;
    }

    @Override
    @Transactional
    public void updateUser(Long id, UserUpdateDTO dto) {
        User user = getById(id);
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
        updateById(user);
        if (dto.getRoleId() != null) {
            userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
            saveUserRole(id, dto.getRoleId());
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        removeById(id);
        userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        tokenService.remove(new LambdaQueryWrapper<Token>().eq(Token::getUserId, id));
    }

    @Override
    public List<String> getUserPermissionCodes(Long userId) {
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> permissionIds = rolePermissionService.list(new LambdaQueryWrapper<RolePermission>()
                        .in(RolePermission::getRoleId, roleIds))
                .stream().map(RolePermission::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) {
            return new ArrayList<>();
        }
        return permissionService.list(new LambdaQueryWrapper<Permission>()
                        .in(Permission::getId, permissionIds)
                        .eq(Permission::getType, 2))
                .stream().map(Permission::getCode).toList();
    }

    private List<MenuVO> getUserMenus(Long userId) {
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> permissionIds = rolePermissionService.list(new LambdaQueryWrapper<RolePermission>()
                        .in(RolePermission::getRoleId, roleIds))
                .stream().map(RolePermission::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Permission> menus = permissionService.list(new LambdaQueryWrapper<Permission>()
                .in(Permission::getId, permissionIds)
                .eq(Permission::getType, 1)
                .orderByAsc(Permission::getSort));
        return buildMenuTree(menus, 0L);
    }

    private List<MenuVO> buildMenuTree(List<Permission> menus, Long parentId) {
        List<MenuVO> result = new ArrayList<>();
        for (Permission permission : menus) {
            Long currentParent = permission.getParentId() == null ? 0L : permission.getParentId();
            if (currentParent.equals(parentId)) {
                MenuVO vo = new MenuVO();
                vo.setId(permission.getId());
                vo.setName(permission.getName());
                vo.setCode(permission.getCode());
                vo.setPath(permission.getPath());
                vo.setIcon(permission.getIcon());
                vo.setChildren(buildMenuTree(menus, permission.getId()));
                result.add(vo);
            }
        }
        return result;
    }

    private List<Long> getUserRoleIds(Long userId) {
        return userRoleService.list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).toList();
    }

    private void saveUserRole(Long userId, Long roleId) {
        if (roleId == null) {
            return;
        }
        if (roleService.getById(roleId) == null) {
            throw new BusinessException(400, "角色不存在");
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleService.save(userRole);
    }

    private UserInfoVO toUserInfo(User user) {
        return toUserInfo(user, getUserRoleIds(user.getId()).stream()
                .map(roleService::getById)
                .filter(role -> role != null)
                .map(Role::getRoleCode)
                .toList());
    }

    private UserInfoVO toUserInfo(User user, List<String> roles) {
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);
        vo.setRoles(roles);
        return vo;
    }

    private Map<Long, List<String>> roleCodesByUserId(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserRole> userRoles = userRoleService.list(new LambdaQueryWrapper<UserRole>()
                .in(UserRole::getUserId, userIds));
        if (userRoles.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).distinct().toList();
        Map<Long, String> roleCodeById = new HashMap<>();
        roleService.list(new LambdaQueryWrapper<Role>().in(Role::getId, roleIds))
                .forEach(role -> roleCodeById.put(role.getId(), role.getRoleCode()));

        Map<Long, List<String>> result = new HashMap<>();
        for (UserRole userRole : userRoles) {
            String roleCode = roleCodeById.get(userRole.getRoleId());
            if (roleCode != null) {
                result.computeIfAbsent(userRole.getUserId(), key -> new ArrayList<>()).add(roleCode);
            }
        }
        return result;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 10;
        }
        return Math.min(Math.max(pageSize, 1), 100);
    }

    private String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }
}
