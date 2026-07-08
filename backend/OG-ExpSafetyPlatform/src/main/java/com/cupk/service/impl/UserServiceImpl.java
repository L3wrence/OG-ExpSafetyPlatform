package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.common.PageResult;
import com.cupk.dto.ChangePasswordDTO;
import com.cupk.dto.LoginDTO;
import com.cupk.dto.ProfileUpdateDTO;
import com.cupk.dto.RegisterDTO;
import com.cupk.dto.UserCreateDTO;
import com.cupk.dto.UserUpdateDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.AuthMapper;
import com.cupk.mapper.OperationLogMapper;
import com.cupk.mapper.UserMapper;
import com.cupk.pojo.OperationLog;
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
import java.time.LocalDateTime;
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
    private final OperationLogMapper operationLogMapper;
    private final AuthMapper authMapper;

    public UserServiceImpl(UserRoleService userRoleService, TokenService tokenService,
                           RolePermissionService rolePermissionService, PermissionService permissionService,
                           RoleService roleService, OperationLogMapper operationLogMapper, AuthMapper authMapper) {
        this.userRoleService = userRoleService;
        this.tokenService = tokenService;
        this.rolePermissionService = rolePermissionService;
        this.permissionService = permissionService;
        this.roleService = roleService;
        this.operationLogMapper = operationLogMapper;
        this.authMapper = authMapper;
    }

    @Override
    @Transactional
    public Long register(RegisterDTO dto) {
        User existUser = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (existUser != null) {
            throw new BusinessException(400, "该学号已存在");
        }
        Role userRole = roleService.getOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, "USER"));
        if (userRole == null) {
            throw new BusinessException(500, "系统未初始化普通用户角色");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(md5(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setMajor(dto.getMajor());
        user.setClassName(dto.getClassName());
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        save(user);
        saveUserRole(user.getId(), userRole.getId());
        writeLog(user.getId(), user.getUsername(), "用户中心", "注册", "普通用户自助注册", "SUCCESS");
        return user.getId();
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
        writeCurrentLog("用户中心", "创建用户", "创建账号：" + user.getUsername(), "SUCCESS");
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
        List<UserInfoVO> records = page.getRecords().stream()
                .map(this::toUserInfo)
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public LoginResultVO login(LoginDTO dto) {
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !md5(dto.getPassword()).equals(user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            writeLog(user.getId(), user.getUsername(), "用户中心", "登录", "禁用账号尝试登录", "DENIED");
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        String token = tokenService.createToken(user.getId());

        LoginResultVO result = new LoginResultVO();
        UserInfoVO userInfo = toUserInfo(user);
        result.setToken(token);
        result.setRole(userInfo.getRole());
        result.setRoles(userInfo.getRoles());
        result.setUserInfo(userInfo);
        result.setMenus(getUserMenus(user.getId()));
        result.setPermissions(getUserPermissionCodes(user.getId()));
        writeLog(user.getId(), user.getUsername(), "用户中心", "登录", "登录成功", "SUCCESS");
        return result;
    }

    @Override
    @Transactional
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            tokenService.remove(new LambdaQueryWrapper<Token>().eq(Token::getToken, token));
        }
        writeCurrentLog("用户中心", "退出登录", "用户主动退出", "SUCCESS");
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordDTO dto) {
        User user = getById(UserContext.userId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!md5(dto.getOldPassword()).equals(user.getPassword())) {
            throw new BusinessException(400, "旧密码不正确");
        }
        if (md5(dto.getNewPassword()).equals(user.getPassword())) {
            throw new BusinessException(400, "新密码不能与旧密码相同");
        }
        user.setPassword(md5(dto.getNewPassword()));
        updateById(user);
        tokenService.remove(new LambdaQueryWrapper<Token>().eq(Token::getUserId, user.getId()));
        writeLog(user.getId(), user.getUsername(), "用户中心", "修改密码", "用户修改登录密码", "SUCCESS");
    }

    @Override
    public UserInfoVO currentUser() {
        User user = getById(UserContext.userId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toUserInfo(user);
    }

    @Override
    @Transactional
    public UserInfoVO updateProfile(ProfileUpdateDTO dto) {
        User user = getById(UserContext.userId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (dto.getRealName() != null) user.setRealName(dto.getRealName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getAvatarUrl() != null) user.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getMajor() != null) user.setMajor(dto.getMajor());
        if (dto.getClassName() != null) user.setClassName(dto.getClassName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        updateById(user);
        writeLog(user.getId(), user.getUsername(), "用户中心", "更新资料", "用户更新个人资料", "SUCCESS");
        return toUserInfo(user);
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
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getMajor() != null) {
            user.setMajor(dto.getMajor());
        }
        if (dto.getClassName() != null) {
            user.setClassName(dto.getClassName());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        updateById(user);
        if (dto.getRoleId() != null) {
            userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
            saveUserRole(id, dto.getRoleId());
        }
        writeCurrentLog("用户中心", "更新用户", "更新账号：" + user.getUsername(), "SUCCESS");
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
        writeCurrentLog("用户中心", "删除用户", "删除账号：" + user.getUsername(), "SUCCESS");
    }

    @Override
    public List<String> getUserPermissionCodes(Long userId) {
        return authMapper.selectPermissionCodes(userId);
    }

    private List<MenuVO> getUserMenus(Long userId) {
        List<String> permissionCodes = getUserPermissionCodes(userId);
        if (permissionCodes.isEmpty()) {
            return new ArrayList<>();
        }
        List<Permission> menus = permissionService.list(new LambdaQueryWrapper<Permission>()
                .in(Permission::getCode, permissionCodes)
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
        Role requestedRole = roleService.getById(roleId);
        if (requestedRole == null) {
            throw new BusinessException(400, "角色不存在");
        }
        if (!"ADMIN".equalsIgnoreCase(requestedRole.getRoleCode())) {
            requestedRole = roleService.getOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, "USER"));
        }
        if (requestedRole == null) {
            throw new BusinessException(500, "系统未初始化普通用户角色");
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(requestedRole.getId());
        userRoleService.save(userRole);
    }

    private UserInfoVO toUserInfo(User user) {
        return toUserInfo(user, authMapper.selectRoleCodes(user.getId()));
    }

    private UserInfoVO toUserInfo(User user, List<String> roles) {
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);
        vo.setRoles(roles);
        vo.setRole(primaryRole(roles));
        vo.setTeacherCertified(authMapper.countApprovedTeacherCertification(user.getId()) > 0);
        return vo;
    }

    private String primaryRole(List<String> roles) {
        for (String role : List.of("ADMIN", "USER")) {
            if (roles.stream().anyMatch(item -> role.equalsIgnoreCase(item))) {
                return role;
            }
        }
        return roles.isEmpty() ? null : roles.get(0);
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

    private void writeCurrentLog(String module, String action, String content, String result) {
        Long userId;
        String username;
        try {
            userId = UserContext.userId();
            User user = getById(userId);
            username = user == null ? null : user.getUsername();
        } catch (BusinessException e) {
            userId = null;
            username = null;
        }
        writeLog(userId, username, module, action, content, result);
    }

    private void writeLog(Long userId, String username, String module, String action, String content, String result) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setModule(module);
        log.setAction(action);
        log.setContent(content);
        log.setResult(result);
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
