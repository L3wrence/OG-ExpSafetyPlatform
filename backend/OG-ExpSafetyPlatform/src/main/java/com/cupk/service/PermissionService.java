package com.cupk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cupk.pojo.Permission;

import java.util.List;

public interface PermissionService extends IService<Permission> {
    List<Permission> getPermissionTree();
}
