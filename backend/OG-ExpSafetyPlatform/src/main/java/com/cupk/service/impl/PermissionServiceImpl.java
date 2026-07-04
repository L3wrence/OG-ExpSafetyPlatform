package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.mapper.PermissionMapper;
import com.cupk.pojo.Permission;
import com.cupk.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {
    @Override
    public List<Permission> getPermissionTree() {
        List<Permission> all = list(new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort));
        return buildTree(all, 0L);
    }

    private List<Permission> buildTree(List<Permission> all, Long parentId) {
        List<Permission> result = new ArrayList<>();
        for (Permission permission : all) {
            Long currentParent = permission.getParentId() == null ? 0L : permission.getParentId();
            if (currentParent.equals(parentId)) {
                permission.setChildren(buildTree(all, permission.getId()));
                result.add(permission);
            }
        }
        return result;
    }
}
