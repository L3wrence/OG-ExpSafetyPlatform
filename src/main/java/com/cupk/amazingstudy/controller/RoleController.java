package com.cupk.amazingstudy.controller;

import com.cupk.amazingstudy.common.CommonResult;
import com.cupk.amazingstudy.pojo.Role;
import com.cupk.amazingstudy.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public CommonResult listAll() {
        List<Role> roles = roleService.list();
        return CommonResult.success(roles);
    }
}