package com.envoil.admin.auth;

import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AdminJwtAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ADMIN_USER_ID = "ADMIN_USER_ID";

    private final JwtTokenService jwtTokenService;

    public AdminJwtAuthInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if ("/admin/auth/login".equals(request.getServletPath())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        String token = header.substring(7).trim();
        try {
            Number uid = jwtTokenService.parseClaims(token).get("uid", Number.class);
            request.setAttribute(ATTR_ADMIN_USER_ID, uid == null ? null : uid.longValue());
            return true;
        } catch (JwtException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
