package com.cupk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.pojo.Role;
import com.cupk.pojo.RolePermission;
import com.cupk.service.RolePermissionService;
import com.cupk.service.RoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;

    public RoleController(RoleService roleService, RolePermissionService rolePermissionService) {
        this.roleService = roleService;
        this.rolePermissionService = rolePermissionService;
    }

    @GetMapping
    @RequirePermission("role:view")
    public Result<List<Role>> listAll() {
        return Result.success(roleService.list());
    }

    @GetMapping("/{roleId}/permissions")
    @RequirePermission("role:view")
    public Result<List<Long>> getRolePermissions(@PathVariable Long roleId) {
        List<Long> permissionIds = rolePermissionService.list(
                        new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId))
                .stream().map(RolePermission::getPermissionId).toList();
        return Result.success(permissionIds);
    }

    @PutMapping("/{roleId}/permissions")
    @RequirePermission("role:permission:update")
    public Result<Void> saveRolePermissions(@PathVariable Long roleId, @RequestBody List<Long> permissionIds) {
        rolePermissionService.remove(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        List<RolePermission> list = permissionIds.stream().map(permissionId -> {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            return rolePermission;
        }).toList();
        if (!list.isEmpty()) {
            rolePermissionService.saveBatch(list);
        }
        return Result.success();
    }
}
