package com.envoil.admin.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserBootstrap implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminUserBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM env_admin_user", Long.class);
        if (n != null && n > 0) {
            return;
        }
        String hash = encoder.encode("admin123");
        jdbcTemplate.update(
                "INSERT INTO env_admin_user (username, password_hash, nick_name, status) VALUES (?,?,?,?)",
                "admin", hash, "超级管理员", "0");
    }
}
