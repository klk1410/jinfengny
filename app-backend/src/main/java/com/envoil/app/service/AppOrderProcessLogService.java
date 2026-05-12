package com.envoil.app.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppOrderProcessLogService {

    private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public AppOrderProcessLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void append(long orderId, String orderNo, String eventCode, String title, Character actorRole, Long actorRefId) {
        jdbcTemplate.update(
                "INSERT INTO biz_env_order_process_log (order_id, order_no, event_code, event_title, actor_role, actor_ref_id) "
                        + "VALUES (?,?,?,?,?,?)",
                orderId,
                orderNo,
                eventCode,
                title,
                actorRole == null ? null : String.valueOf(actorRole),
                actorRefId);
    }

    public int countByOrderNo(String orderNo) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_env_order_process_log WHERE order_no = ?",
                Integer.class,
                orderNo);
        return n == null ? 0 : n;
    }

    public List<Map<String, Object>> listByOrderNo(String orderNo) {
        return jdbcTemplate.query(
                "SELECT log_id, order_no, event_code, event_title, actor_role, actor_ref_id, operation_time "
                        + "FROM biz_env_order_process_log WHERE order_no = ? ORDER BY log_id ASC",
                (rs, i) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("logId", rs.getLong("log_id"));
                    row.put("orderNo", rs.getString("order_no"));
                    row.put("eventCode", rs.getString("event_code"));
                    row.put("title", rs.getString("event_title"));
                    row.put("actorRole", rs.getString("actor_role"));
                    row.put("actorRefId", rs.getObject("actor_ref_id") == null ? null : rs.getLong("actor_ref_id"));
                    row.put("operationTime", formatTs(rs.getTimestamp("operation_time")));
                    return row;
                },
                orderNo);
    }

    private static String formatTs(Timestamp ts) {
        if (ts == null) {
            return "";
        }
        synchronized (TS_FMT) {
            return TS_FMT.format(ts);
        }
    }
}
