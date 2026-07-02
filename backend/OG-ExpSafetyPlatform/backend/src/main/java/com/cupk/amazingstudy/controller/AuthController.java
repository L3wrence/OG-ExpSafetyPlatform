package com.cupk.amazingstudy.controller;

import com.cupk.amazingstudy.common.CommonResult;
import com.cupk.amazingstudy.dto.LoginDTO;
import com.cupk.amazingstudy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public CommonResult login(@Valid @RequestBody LoginDTO dto) {
        return userService.login(dto);
    }
}