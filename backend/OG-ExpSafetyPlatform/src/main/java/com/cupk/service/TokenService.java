package com.cupk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cupk.pojo.Token;

public interface TokenService extends IService<Token> {
    String createToken(Long userId);
}
