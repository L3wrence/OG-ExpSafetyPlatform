package com.cupk.amazingstudy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.amazingstudy.pojo.Role;
import com.cupk.amazingstudy.mapper.RoleMapper;
import com.cupk.amazingstudy.service.RoleService;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
}