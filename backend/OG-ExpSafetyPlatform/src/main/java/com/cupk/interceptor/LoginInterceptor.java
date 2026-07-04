package com.cupk.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.exception.BusinessException;
import com.cupk.common.UserContext;
import com.cupk.pojo.Token;
import com.cupk.service.TokenService;
import com.cupk.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录拦截器
 * 校验 Token 并注入用户上下文
 * 成员A交付
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    @Autowired
    @Lazy
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            throw new BusinessException(401, "未登录，请先登录");
        }

        // 去掉 "Bearer " 前缀（Apifox/标准 HTTP 鉴权会带此前缀）
        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7) : authorization;
        Token tokenEntity = tokenService.getOne(
                new LambdaQueryWrapper<Token>().eq(Token::getToken, token));
        if (tokenEntity == null) {
            throw new BusinessException(401, "无效的认证令牌");
        }

        if (tokenEntity.getExpireTime() != null
                && tokenEntity.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }

        // Token 校验通过后，注入用户上下文
        UserContext.setUserId(tokenEntity.getUserId());
        List<String> perms = userService.getUserPermissionCodes(tokenEntity.getUserId());
        UserContext.setPermissions(perms);
        List<String> roles = userService.getUserRoleCodes(tokenEntity.getUserId());
        UserContext.setRoles(roles);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
