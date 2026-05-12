package com.envoil.app.service;

import com.envoil.app.model.OpenidBizScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AppOpenidBizScopeService {

    private final JdbcTemplate jdbcTemplate;

    public AppOpenidBizScopeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 解析数据范围：优先读表；无记录时按测试 openid 回退，其它 openid 按主端全量（便于联调）。
     */
    public OpenidBizScope resolve(String openid) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT user_role, agent_id, merchant_id, salesman_id FROM env_openid_biz_scope WHERE openid = ?",
                openid);
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            String role = String.valueOf(r.get("user_role"));
            char ur = role == null || role.isEmpty() ? '1' : role.charAt(0);
            return new OpenidBizScope(ur, toLong(r.get("agent_id")), toLong(r.get("merchant_id")), toLong(r.get("salesman_id")));
        }
        return fallback(openid);
    }

    private static OpenidBizScope fallback(String openid) {
        if ("main-openid-001".equals(openid)) {
            return new OpenidBizScope('1', null, null, null);
        }
        if ("agent-openid-001".equals(openid)) {
            return new OpenidBizScope('2', 1L, null, null);
        }
        if ("sales-openid-001".equals(openid)) {
            return new OpenidBizScope('3', 1L, null, 1L);
        }
        if ("merchant-openid-001".equals(openid)) {
            return new OpenidBizScope('4', 1L, 1L, null);
        }
        return new OpenidBizScope('1', null, null, null);
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
