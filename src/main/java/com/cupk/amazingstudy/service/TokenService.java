package com.cupk.amazingstudy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cupk.amazingstudy.pojo.Token;

public interface TokenService extends IService<Token> {
    String createToken(Long userId);
}