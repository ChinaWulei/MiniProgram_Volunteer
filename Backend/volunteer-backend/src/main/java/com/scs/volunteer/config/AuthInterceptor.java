package com.scs.volunteer.config;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || request.getRequestURI().startsWith("/api/auth")) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("volunteer-token-")) {
            String userId = request.getParameter("userId");
            if (userId == null || userId.isBlank()) {
                userId = request.getHeader("X-User-Id");
            }
            request.setAttribute("currentUser", new CurrentUser(userId == null || userId.isBlank() ? 2L : Long.valueOf(userId), "VOLUNTEER"));
            return true;
        }
        String[] parts = token.replace("volunteer-token-", "").split("-");
        if (parts.length < 2) {
            throw new BizException("无效 token");
        }
        request.setAttribute("currentUser", new CurrentUser(Long.valueOf(parts[0]), parts[1]));
        return true;
    }
}
