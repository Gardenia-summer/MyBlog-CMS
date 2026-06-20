package com.tzu.myblogcms.config;

import com.tzu.myblogcms.auth.AuthSession;
import com.tzu.myblogcms.auth.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (AuthSession.hasRole(request.getSession(false), Role.USER)) {
            return true;
        }
        // 管理员不进入普通用户功能，防止用后台账号发评论或点赞。
        if (AuthSession.hasRole(request.getSession(false), Role.ADMIN)) {
            response.sendRedirect(request.getContextPath() + "/admin/articles");
            return false;
        }
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
