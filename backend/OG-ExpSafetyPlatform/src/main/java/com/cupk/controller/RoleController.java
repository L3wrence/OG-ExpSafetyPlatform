package com.cupk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.common.Result;
import com.cupk.pojo.Role;
import com.cupk.pojo.RolePermission;
import com.cupk.service.RolePermissionService;
import com.cupk.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @GetMapping
    public Result<?> listAll() {
        List<Role> roles = roleService.list();
        return Result.success(roles);
    }

    @PutMapping("/{roleId}/permissions")
    public Result<?> saveRolePermissions(@PathVariable Long roleId, @RequestBody List<Long> permIds) {
        rolePermissionService.remove(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        List<RolePermission> list = permIds.stream().map(permId -> {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permId);
            return rp;
        }).collect(Collectors.toList());
        rolePermissionService.saveBatch(list);
        return Result.success("权限分配成功");
    }

    @GetMapping("/{roleId}/permissions")
    public Result<?> getRolePermissions(@PathVariable Long roleId) {
        List<Long> permIds = rolePermissionService.list(
                        new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId))
                .stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
        return Result.success(permIds);
    }
}
