package com.cupk.amazingstudy.controller;


import com.cupk.amazingstudy.common.CommonResult;
import com.cupk.amazingstudy.common.RequirePermission;
import com.cupk.amazingstudy.dto.UserCreateDTO;
import com.cupk.amazingstudy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;



@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @RequirePermission("user:create")
    @PostMapping
    public CommonResult createUser(@Valid @RequestBody UserCreateDTO dto) {
        return userService.createUser(dto);
    }
    @GetMapping
    public CommonResult listUsers(@RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                  @RequestParam(required = false) String keyword) {
        return userService.listUsers(pageNum, pageSize, keyword);
    }
}