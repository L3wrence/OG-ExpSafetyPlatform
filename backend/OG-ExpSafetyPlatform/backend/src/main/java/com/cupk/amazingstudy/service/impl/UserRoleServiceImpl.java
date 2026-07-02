package com.cupk.amazingstudy.service.impl;



import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.amazingstudy.pojo.UserRole;
import com.cupk.amazingstudy.mapper.UserRoleMapper;
import com.cupk.amazingstudy.service.UserRoleService;
import org.springframework.stereotype.Service;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {
}