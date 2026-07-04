package com.cupk.controller;

import com.cupk.common.Result;
import com.cupk.common.RequirePermission;
import com.cupk.dto.UserCreateDTO;
import com.cupk.dto.UserDeleteDTO;
import com.cupk.dto.UserUpdateDTO;
import com.cupk.service.UserService;
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
    public Result<?> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return userService.createUser(dto);
    }

    @GetMapping
    public Result<?> listUsers(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(required = false) String keyword) {
        return userService.listUsers(pageNum, pageSize, keyword);
    }

    @RequirePermission("user:update")
    @PutMapping
    public Result<?> updateUser(@Valid @RequestBody UserUpdateDTO dto) {
        return userService.updateUser(dto.getId(), dto);
    }

    @RequirePermission("user:delete")
    @DeleteMapping
    public Result<?> deleteUser(@Valid @RequestBody UserDeleteDTO dto) {
        return userService.deleteUser(dto.getId());
    }
}
