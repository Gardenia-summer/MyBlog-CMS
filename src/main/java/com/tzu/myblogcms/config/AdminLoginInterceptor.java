package com.tzu.myblogcms.config;

import com.tzu.myblogcms.auth.AuthSession;
import com.tzu.myblogcms.auth.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 所有 /admin/** 请求先校验管理员角色，登录页本身在 WebConfig 中排除。
        if (AuthSession.hasRole(request.getSession(false), Role.ADMIN)) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/admin/login");
        return false;
    }
}
