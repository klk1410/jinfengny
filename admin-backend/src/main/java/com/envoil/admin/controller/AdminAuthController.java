package com.envoil.admin.controller;

import com.envoil.admin.auth.JwtTokenService;
import com.envoil.admin.common.ApiResponse;
import com.envoil.admin.model.dto.LoginRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final JdbcTemplate jdbcTemplate;
    private final JwtTokenService jwtTokenService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminAuthController(JdbcTemplate jdbcTemplate, JwtTokenService jwtTokenService) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Validated @RequestBody LoginRequest req) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT user_id, username, password_hash, nick_name, status FROM env_admin_user WHERE username = ?",
                req.getUsername());
        if (rows.isEmpty()) {
            return ApiResponse.fail("用户名或密码错误");
        }
        Map<String, Object> row = rows.get(0);
        if (!"0".equals(String.valueOf(row.get("status")))) {
            return ApiResponse.fail("账号已停用");
        }
        String hash = (String) row.get("password_hash");
        if (!encoder.matches(req.getPassword(), hash)) {
            return ApiResponse.fail("用户名或密码错误");
        }
        Long uid = ((Number) row.get("user_id")).longValue();
        String token = jwtTokenService.createToken(uid, req.getUsername());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("username", row.get("username"));
        data.put("nickName", row.get("nick_name"));
        return ApiResponse.ok(data);
    }
}
