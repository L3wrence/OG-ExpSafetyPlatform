package com.cupk.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.exception.BusinessException;
import com.cupk.mapper.AuthMapper;
import com.cupk.mapper.TokenMapper;
import com.cupk.pojo.Token;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    private final TokenMapper tokenMapper;
    private final AuthMapper authMapper;

    public LoginInterceptor(TokenMapper tokenMapper, AuthMapper authMapper) {
        this.tokenMapper = tokenMapper;
        this.authMapper = authMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        Token token = tokenMapper.selectOne(new LambdaQueryWrapper<Token>().eq(Token::getToken, authorization));
        if (token == null) {
            throw new BusinessException(401, "无效的认证令牌");
        }
        if (token.getExpireTime() != null && token.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
        List<String> roleCodes = authMapper.selectRoleCodes(token.getUserId());
        List<String> permissionCodes = authMapper.selectPermissionCodes(token.getUserId());
        UserContext.set(new UserSession(token.getUserId(), roleCodes, permissionCodes));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
