package com.cupk.amazingstudy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.amazingstudy.mapper.TokenMapper;
import com.cupk.amazingstudy.pojo.Token;
import com.cupk.amazingstudy.service.TokenService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TokenServiceImpl extends ServiceImpl<TokenMapper, Token> implements TokenService {

    @Override
    public String createToken(Long userId) {
        // 删除旧token
        this.remove(new LambdaQueryWrapper<Token>().eq(Token::getUserId, userId));
        // 生成新token
        String tokenStr = UUID.randomUUID().toString().replace("-", "");
        Token token = new Token();
        token.setUserId(userId);
        token.setToken(tokenStr);
        token.setExpireTime(LocalDateTime.now().plusDays(1));
        this.save(token);
        return tokenStr;
    }
}