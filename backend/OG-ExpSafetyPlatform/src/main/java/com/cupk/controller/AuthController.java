package com.cupk.controller;

import com.cupk.common.Result;
import com.cupk.common.RequirePermission;
import com.cupk.dto.ChangePasswordDTO;
import com.cupk.dto.LoginDTO;
import com.cupk.dto.RegisterDTO;
import com.cupk.service.UserService;
import com.cupk.vo.LoginResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<LoginResultVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        userService.logout(token);
        return Result.success();
    }

    @PutMapping("/password")
    @RequirePermission("profile:password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.success();
    }
}
