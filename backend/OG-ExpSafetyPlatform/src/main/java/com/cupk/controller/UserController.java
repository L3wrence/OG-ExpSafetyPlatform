package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.UserCreateDTO;
import com.cupk.dto.UserDeleteDTO;
import com.cupk.dto.UserUpdateDTO;
import com.cupk.service.UserService;
import com.cupk.vo.UserInfoVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @RequirePermission("user:view")
    public Result<PageResult<UserInfoVO>> listUsers(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(required = false) String keyword) {
        return Result.success(userService.listUsers(pageNum, pageSize, keyword));
    }

    @PostMapping
    @RequirePermission("user:create")
    public Result<Long> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success(userService.createUser(dto));
    }

    @PutMapping
    @RequirePermission("user:update")
    public Result<Void> updateUser(@Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUser(dto.getId(), dto);
        return Result.success();
    }

    @DeleteMapping
    @RequirePermission("user:delete")
    public Result<Void> deleteUser(@Valid @RequestBody UserDeleteDTO dto) {
        userService.deleteUser(dto.getId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("user:delete")
    public Result<Void> deleteUserById(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
